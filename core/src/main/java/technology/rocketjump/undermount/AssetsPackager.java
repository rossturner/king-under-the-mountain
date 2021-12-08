package technology.rocketjump.undermount;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.modding.ModParser;
import technology.rocketjump.undermount.modding.exception.ModLoadingException;
import technology.rocketjump.undermount.modding.model.*;
import technology.rocketjump.undermount.modding.processing.ModArtifactProcessor;
import technology.rocketjump.undermount.modding.validation.ModArtifactValidator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class AssetsPackager {

	private final ModParser modParser;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private Path tempDir;

	public static void main(String... args) {
		List<Path> modDirs = Arrays.asList(Paths.get("mods/base"), Paths.get("mods/Community Translations"));
		AssetsPackager assetsPackager = new AssetsPackager(new ModParser(new ModArtifactListing()));

		assetsPackager.packageDirsToAssets(modDirs, Paths.get("assets"));
	}

	@Inject
	public AssetsPackager(ModParser modParser) {
		this.modParser = modParser;
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
	}

	public void packageModsToAssets(List<ParsedMod> activeMods, Path assetsDir) {
		initTempDir(assetsDir);
		try {
			ModCollection collection = new ModCollection();
			activeMods.stream().forEach(mod -> collection.add(mod));
			packageModCollection(collection, assetsDir);
		} finally {
			FileUtils.deleteQuietly(tempDir.toFile());
		}
	}

	public void packageDirsToAssets(List<Path> selectedModDirs, Path assetsDir) {
		initTempDir(assetsDir);
		try {
			ModCollection modCollection = new ModCollection();

			for (Path modDir : selectedModDirs) {
				if (!Files.exists(modDir) || !Files.isDirectory(modDir)) {
					throw new RuntimeException(modDir.toAbsolutePath().toString() + " does not exist or is not a directory");
				}
				try {
					long start = System.currentTimeMillis();
					ParsedMod parsedMod = modParser.parseMod(modDir);
					long end = System.currentTimeMillis();
					Logger.info("Parsed " + modDir.toString() + " in " + (end - start) + "ms");
					modCollection.add(parsedMod);
				} catch (IOException e) {
					Logger.error(e, "Exception while parsing " + modDir.toString());
				}
			}

			packageModCollection(modCollection, assetsDir);


		} finally {
			FileUtils.deleteQuietly(tempDir.toFile());
		}
	}

	private void packageModCollection(ModCollection modCollection, Path assetDir) {
		Map<ModArtifactDefinition, Long> currentChecksums = null;
		try {
			currentChecksums = readChecksums(assetDir);
		} catch (IOException e) {
			Logger.error(e, "Could not read existing checksums");
		}

		for (ModArtifactDefinition artifactDefinition : modParser.getArtifactListing().getAll()) {
			long newChecksum = 0L;
			try {
				newChecksum = modCollection.getChecksum(artifactDefinition);
			} catch (IOException e) {
				Logger.error(e, "Could not process checksum for input files of " + artifactDefinition.getName());
			}

			if (newChecksum != currentChecksums.get(artifactDefinition)) {
				long start = System.currentTimeMillis();

				List<Exception> modLoadExceptions = new ArrayList<>();

				ModArtifactProcessor processor = createProcessorInstance(artifactDefinition);

				List<ModArtifact> validArtifacts = modCollection.getAll().stream()
						.map(mod -> {
							ModArtifact artifact = mod.get(artifactDefinition);
							if (artifact == null) {
								return null;
							} else {
								try {
									processor.apply(artifact, mod, assetDir);
									validate(artifact, mod);
								} catch (Exception e) {
									modLoadExceptions.add(e);
								}
								return artifact;
							}
						})
						.filter(Objects::nonNull)
						.collect(Collectors.toList());

				if (!modLoadExceptions.isEmpty()) {
					Logger.error(modLoadExceptions.get(0), "Exception while processing artifact " + artifactDefinition.getName());
					continue; // Do not set new checksum so it is reattempted next time
				}

				try {
					combineAndPackage(validArtifacts, artifactDefinition, assetDir);
					currentChecksums.put(artifactDefinition, newChecksum);
				} catch (ModLoadingException | IOException e) {
					Logger.error(e, "Could not write artifact " + artifactDefinition.getName());
					e.printStackTrace();
				} finally {
					long end = System.currentTimeMillis();
					Logger.info("Processed " + artifactDefinition.toString() + " in " + (end - start) + "ms");
				}

			}
		}

		try {
			writeChecksums(assetDir, currentChecksums);
		} catch (IOException e) {
			Logger.error(e, "Could not write checksums");
		}
	}

	private void validate(ModArtifact modArtifact, ParsedMod mod) throws ModLoadingException {
		for (Class<? extends ModArtifactValidator> validatorClass : modArtifact.artifactDefinition.validators) {
			try {
				ModArtifactValidator validatorInstance = validatorClass.getDeclaredConstructor().newInstance();
				validatorInstance.apply(modArtifact, mod);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void combineAndPackage(List<ModArtifact> validArtifacts, ModArtifactDefinition artifactDefinition, Path assetsDir) throws ModLoadingException, IOException {
		ModArtifactProcessor processor = createProcessorInstance(artifactDefinition);
		processor.combine(validArtifacts, tempDir);
		processor.write(assetsDir);
	}

	private Map<ModArtifactDefinition, Long> readChecksums(Path assetDir) throws IOException {
		Path checksumPath = assetDir.resolve("metadata/checksums.json");
		JSONObject checksumJson = JSON.parseObject(FileUtils.readFileToString(checksumPath.toFile()));

		Map<ModArtifactDefinition, Long> checksums = new TreeMap<>();
		for (ModArtifactDefinition artifactDefinition : modParser.getArtifactListing().getAll()) {
			long value = checksumJson.getLongValue(artifactDefinition.getName());
			checksums.put(artifactDefinition, value);
		}
		return checksums;
	}

	private void writeChecksums(Path assetDir, Map<ModArtifactDefinition, Long> checksums) throws IOException {
		Path checksumPath = assetDir.resolve("metadata/checksums.json");
		FileUtils.writeStringToFile(checksumPath.toFile(), objectMapper.writeValueAsString(checksums));
	}


	private ModArtifactProcessor createProcessorInstance(ModArtifactDefinition artifactDefinition) {
		try {
			if (artifactDefinition.processor == null) {
				throw new RuntimeException("No processor defined for " + artifactDefinition.getName());
			}
			Constructor<? extends ModArtifactProcessor> constructor = artifactDefinition.processor.getDeclaredConstructor(ModArtifactDefinition.class);
			return constructor.newInstance(artifactDefinition);
		} catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private void initTempDir(Path assetDir) {
		tempDir = assetDir.resolve("temp");
		try {
			File tempDirFile = tempDir.toFile();
			if (tempDirFile.exists()) {
				FileUtils.deleteDirectory(tempDirFile);
			}
			FileUtils.forceMkdir(tempDirFile);
		} catch (IOException e) {
			throw new RuntimeException("Could not write to " + tempDir);
		}
	}
}
