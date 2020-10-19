package technology.rocketjump.undermount.modding.processing;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.modding.exception.ModLoadingException;
import technology.rocketjump.undermount.modding.model.ModArtifact;
import technology.rocketjump.undermount.modding.model.ModArtifactDefinition;
import technology.rocketjump.undermount.modding.model.ParsedMod;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.undermount.modding.model.ModArtifactDefinition.ArtifactCombinationType.REPLACES_EXISTING;

public class UntypedJsonProcessor extends ModArtifactProcessor {

	protected Map<String, JSONObject> combined = new LinkedHashMap<>();
	protected JSONArray combinedArray = new JSONArray();

	public UntypedJsonProcessor(ModArtifactDefinition definition) {
		super(definition);
	}

	@Override
	public void apply(ModArtifact modArtifact, ParsedMod mod, Path assetDir) throws ModLoadingException {
		switch (modArtifact.artifactDefinition.inputFileType) {
			case JSON_ARRAY: {
				loadJsonArrays(modArtifact);
				break;
			}
			case JSON_MAP:
			case JSON_OBJECT: {
				loadJsonObjects(modArtifact);
				break;
			}
			case JSON_KEY_VALUES: {
				loadJsonKeyValues(modArtifact);
				break;
			}
			default:
				throw new RuntimeException("Not yet implemented: " + getClass().getSimpleName() +
						".loadSourceFiles() for inputFileType " + modArtifact.artifactDefinition.inputFileType);
		}
	}

	@Override
	public void combine(List<ModArtifact> artifacts, Path tempDir) throws ModLoadingException {
		switch (definition.combinationType) {
			case ADDITIVE:
				switch (definition.inputFileType) {
					case JSON_KEY_VALUES: {
						JSONObject combined = this.combined.computeIfAbsent("single", (a) -> new JSONObject(true));
						for (ModArtifact artifact : artifacts) {
							JSONObject artifactJson = (JSONObject) artifact.getData();
							combined.putAll(artifactJson);
						}
						break;
					}
					default: {
						// Still need to replace on name
						Field nameField = getNameField();

						for (ModArtifact artifact : artifacts) {
							JSONArray artifactData = (JSONArray) artifact.getData();
							for (int cursor = 0; cursor < artifactData.size(); cursor++) {
								JSONObject instance = artifactData.getJSONObject(cursor);
								String nameOfInstance = instance.getString(nameField.getName());
								if (combined.containsKey(nameOfInstance)) {
									Logger.info("Replacing " + definition.classType.getSimpleName() + " " + nameOfInstance);
								}
								combined.put(nameOfInstance, instance);
							}
						}
					}
				}
				break;
			case REPLACES_EXISTING:
				for (ModArtifact artifact : artifacts) {
					JSONArray artifactJson = (JSONArray) artifact.getData();
					if (artifactJson.size() != 1) {
						throw new RuntimeException("Expecting only one JSON object for combinationType " + REPLACES_EXISTING.name());
					} else {
						JSONObject jsonObject = artifactJson.getJSONObject(0);
						combined.put("single", jsonObject);
					}
				}
				break;
			default:
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
					case JSON_MAP:
					case JSON_KEY_VALUES:
					case JSON_OBJECT: {
						if (combined.size() != 1) {
							throw new RuntimeException("Expecting only one JSON object for outputType " + definition.outputType.name());
						}
						String outputJson = objectMapper.writeValueAsString(combined.values().iterator().next());
						Files.write(outputFilePath, outputJson.getBytes());
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

	private void loadJsonArrays(ModArtifact modArtifact) throws ModLoadingException {
		JSONArray loadedArray = new JSONArray();
		for (Path sourceFile : modArtifact.sourceFiles) {
			try {
				JSONArray asJson = JSON.parseArray(FileUtils.readFileToString(sourceFile.toFile()));
				loadedArray.addAll(asJson);
			} catch (IOException e) {
				Logger.error(e, "Error reading " + modArtifact.artifactDefinition.classType.getSimpleName() + " from " + sourceFile.toString());
				throw new ModLoadingException(e);
			}
		}
		modArtifact.setData(loadedArray);
	}

	private void loadJsonObjects(ModArtifact modArtifact) throws ModLoadingException {
		JSONArray loadedArray = new JSONArray();
		for (Path sourceFile : modArtifact.sourceFiles) {
			try {
				JSONObject asJson = JSON.parseObject(FileUtils.readFileToString(sourceFile.toFile()));
				loadedArray.add(asJson);
			} catch (IOException e) {
				Logger.error(e, "Error reading " + modArtifact.artifactDefinition.classType.getSimpleName() + " from " + sourceFile.toString());
				throw new ModLoadingException(e);
			}
		}
		modArtifact.setData(loadedArray);
	}

	private void loadJsonKeyValues(ModArtifact modArtifact) throws ModLoadingException {
		JSONObject cumulativeJson = new JSONObject(true);
		for (Path sourceFile : modArtifact.sourceFiles) {
			try {
				JSONObject fileJson = JSON.parseObject(FileUtils.readFileToString(sourceFile.toFile()));
				cumulativeJson.putAll(fileJson);
			} catch (IOException e) {
				Logger.error(e, "Error reading " + modArtifact.artifactDefinition.classType.getSimpleName() + " from " + sourceFile.toString());
				throw new ModLoadingException(e);
			}
		}
		modArtifact.setData(cumulativeJson);
	}


	protected Path getOutputFile(Path assetsDir) {
		if (definition.outputFileName == null) {
			throw new RuntimeException("Programming error: This artifact has multiple output files");
		} else {
			return assetsDir.resolve(definition.getName());
		}
	}
}
