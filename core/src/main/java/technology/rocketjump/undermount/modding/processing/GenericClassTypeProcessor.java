package technology.rocketjump.undermount.modding.processing;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.factories.names.LoadedFromCsv;
import technology.rocketjump.undermount.misc.SequentialId;
import technology.rocketjump.undermount.modding.exception.ModLoadingException;
import technology.rocketjump.undermount.modding.model.ModArtifact;
import technology.rocketjump.undermount.modding.model.ModArtifactDefinition;
import technology.rocketjump.undermount.modding.model.ParsedMod;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static technology.rocketjump.undermount.modding.model.ModArtifactDefinition.ArtifactCombinationType.ADDITIVE;
import static technology.rocketjump.undermount.modding.model.ModArtifactDefinition.ModFileType.CSV;
import static technology.rocketjump.undermount.modding.model.ModArtifactDefinition.ModFileType.JSON_MAP;

public class GenericClassTypeProcessor extends ModArtifactProcessor {

	private Map<String, Object> combined = new LinkedHashMap<>();
	private SequentialIdGenerator idGenerator = new SequentialIdGenerator();

	public GenericClassTypeProcessor(ModArtifactDefinition definition) {
		super(definition);
	}

	@Override
	public void apply(ModArtifact modArtifact, ParsedMod mod, Path assetDir) throws ModLoadingException {
		switch (modArtifact.artifactDefinition.inputFileType) {
			case JSON_ARRAY: {
				loadFromJsonArrays(modArtifact);
				break;
			}
			case JSON_OBJECT: {
				loadFromJsonObjects(modArtifact);
				break;
			}
			case JSON_MAP: {
				loadFromJsonAsMap(modArtifact);
				break;
			}
			case CSV: {
				loadFromCsv(modArtifact);
				break;
			}
			default:
				throw new RuntimeException("Not yet implemented: " + getClass().getSimpleName() +
						".loadSourceFiles() for inputFileType " + modArtifact.artifactDefinition.inputFileType);
		}
	}

	@Override
	public void combine(List<ModArtifact> artifacts, Path tempDir) throws ModLoadingException {
		if (definition.combinationType.equals(ADDITIVE)) {
			if (definition.inputFileType.equals(JSON_MAP) || definition.inputFileType.equals(CSV)) {
				for (ModArtifact artifact : artifacts) {
					Map<String, Object> artifactData = (Map<String, Object>) artifact.getData();
					for (Map.Entry<String, Object> entry : artifactData.entrySet()) {
						if (combined.containsKey(entry.getKey())) {
							Logger.info("Replacing " + definition.classType.getSimpleName() + " " + entry.getValue());
						}
						combined.put(entry.getKey(), entry.getValue());
					}
				}
			} else {
				// Still need to replace on name
				Field nameField = getNameField();

				for (ModArtifact artifact : artifacts) {
					List<?> artifactData = getData(artifact);
					for (Object instance : artifactData) {
						String nameOfInstance = getNameOfInstance(nameField, instance);
						if (combined.containsKey(nameOfInstance)) {
							Logger.info("Replacing " + definition.classType.getSimpleName() + " " + nameOfInstance);
						}
						combined.put(nameOfInstance, instance);
					}
				}
			}
		} else {
			throw new RuntimeException("Not yet implemented, combinationType " + definition.combinationType + " for " + getClass().getSimpleName());
		}
	}

	@Override
	public void write(Path assetsDir) throws IOException {
		switch (definition.outputType) {
			case SINGLE_FILE: {
				Path outputFilePath = getOutputFile(assetsDir);
				Files.deleteIfExists(outputFilePath);
				switch (definition.outputFileType) {
					case JSON_ARRAY: {
						String outputJson = objectMapper.writeValueAsString(new ArrayList<>(combined.values()));
						Files.write(outputFilePath, outputJson.getBytes());
						break;
					}
					case JSON_MAP: {
						String outputJson = objectMapper.writeValueAsString(combined);
						Files.write(outputFilePath, outputJson.getBytes());
						break;
					}
					case CSV: {
						List<String> lines = new ArrayList<>();
						for (Object instance : combined.values()) {
							lines.add(((LoadedFromCsv)instance).writeToLine());
						}
						Files.write(outputFilePath, lines);
						break;
					}
					default:
						throw new RuntimeException("Not yet implemented: " + definition.outputFileType);
				}
				break;
			}
			default:
				throw new RuntimeException("Not yet implemented: " + definition.outputType);
		}

	}

	private String getNameOfInstance(Field nameField, Object instance) throws ModLoadingException {
		try {
			Object nameObj = nameField.get(instance);
			if (nameObj == null) {
				throw new ModLoadingException("Name of " + instance.toString() + " is null");
			} else {
				return nameObj.toString();
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Could not access field " + nameField.getName() + " on " + definition.classType.getSimpleName(), e);
		}
	}

	@SuppressWarnings("unchecked")
	private List<Object> getData(ModArtifact modArtifact) {
		return (List<Object>) modArtifact.getData();
	}

	private void loadFromJsonArrays(ModArtifact modArtifact) throws ModLoadingException {
		List<Object> loadedArray = new ArrayList<>();
		for (Path sourceFile : modArtifact.sourceFiles) {
			try {
				List<?> loadedFromFile = objectMapper.readValue(FileUtils.readFileToString(sourceFile.toFile()),
						objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, modArtifact.artifactDefinition.classType));
				loadedArray.addAll(loadedFromFile);
			} catch (IOException e) {
				Logger.error(e, "Error reading " + modArtifact.artifactDefinition.classType.getSimpleName() + " from " + sourceFile.toString());
				throw new ModLoadingException(e);
			}
		}

		applyAnySequentialIds(loadedArray);

		modArtifact.setData(loadedArray);
	}

	private void loadFromJsonObjects(ModArtifact modArtifact) throws ModLoadingException {
		List<Object> loadedArray = new ArrayList<>();
		for (Path sourceFile : modArtifact.sourceFiles) {
			try {
				Object loadedFromFile = objectMapper.readValue(FileUtils.readFileToString(sourceFile.toFile()),
						modArtifact.artifactDefinition.classType);
				loadedArray.add(loadedFromFile);
			} catch (IOException e) {
				Logger.error(e, "Error reading " + modArtifact.artifactDefinition.classType.getSimpleName() + " from " + sourceFile.toString());
				throw new ModLoadingException(e);
			}
		}

		applyAnySequentialIds(loadedArray);

		modArtifact.setData(loadedArray);
	}

	private void loadFromJsonAsMap(ModArtifact modArtifact) throws ModLoadingException {
		Map<String, Object> artifactInstances = new TreeMap<>();
		for (Path sourceFile : modArtifact.sourceFiles) {
			try {
				JSONObject mapJson = JSON.parseObject(FileUtils.readFileToString(sourceFile.toFile()));
				for (Map.Entry<String, Object> entry : mapJson.entrySet()) {
					String name = entry.getKey();
					JSONObject value = (JSONObject) entry.getValue();

					Object typedInstance = objectMapper.readValue(value.toJSONString(), modArtifact.artifactDefinition.classType);
					artifactInstances.put(name, typedInstance);
				}
			} catch (IOException e) {
				Logger.error(e, "Error reading " + modArtifact.artifactDefinition.classType.getSimpleName() + " from " + sourceFile.toString());
				throw new ModLoadingException(e);
			}
		}

		applyAnySequentialIds(artifactInstances.values());

		modArtifact.setData(artifactInstances);
	}

	private void loadFromCsv(ModArtifact modArtifact) throws ModLoadingException {
		if (!LoadedFromCsv.class.isAssignableFrom(definition.classType)) {
			throw new RuntimeException(definition.classType.getSimpleName() + " does not implement " + LoadedFromCsv.class.getSimpleName());
		}
		Map<String, LoadedFromCsv> artifactInstances = new TreeMap<>();
		for (Path sourceFile : modArtifact.sourceFiles) {
			try {
				List<String> lines = Files.readAllLines(sourceFile);
				for (String line : lines) {
					LoadedFromCsv instance = (LoadedFromCsv) definition.classType.getDeclaredConstructor().newInstance();
					instance.readFromLine(line);
					if (!StringUtils.isEmpty(instance.getUniqueName())) {
						artifactInstances.put(instance.getUniqueName(), instance);
					}
				}
			} catch (IOException | ReflectiveOperationException e) {
				Logger.error(e, "Error reading " + modArtifact.artifactDefinition.classType.getSimpleName() + " from " + sourceFile.toString());
				throw new ModLoadingException(e);
			}
		}

		modArtifact.setData(artifactInstances);
	}

	private Path getOutputFile(Path assetsDir) {
		if (definition.outputFileName == null) {
			throw new RuntimeException("Programming error: This artifact has multiple output files");
		} else {
			return assetsDir.resolve(definition.getName());
		}
	}

	private void applyAnySequentialIds(Collection<Object> instances) {
		List<Field> sequentialIdFields = FieldUtils.getFieldsListWithAnnotation(definition.classType, SequentialId.class);
		if (!sequentialIdFields.isEmpty()) {
			for (Field field : sequentialIdFields) {
				if (!field.getType().getName().equals("long")) {
					throw new RuntimeException(SequentialId.class.getSimpleName() + " must be applied to a long-type field");
				}
				field.setAccessible(true);
			}
			for (Object instance : instances) {
				applySequentialId(instance, sequentialIdFields);
			}
		}
	}

	private void applySequentialId(Object instance, List<Field> sequentialIdFields) {
		try {
			for (Field sequentialIdField : sequentialIdFields) {
				sequentialIdField.setLong(instance, idGenerator.instanceNextId());
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
