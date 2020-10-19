package technology.rocketjump.undermount.modding.processing;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.modding.exception.ModLoadingException;
import technology.rocketjump.undermount.modding.model.ModArtifact;
import technology.rocketjump.undermount.modding.model.ModArtifactDefinition;
import technology.rocketjump.undermount.modding.model.ParsedMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.undermount.modding.model.ModArtifactDefinition.OutputType.SINGLE_FILE;

public class EntityAssetTypesProcessor extends ModArtifactProcessor {

	private Map<EntityType, List<EntityAssetType>> combinedData = new LinkedHashMap<>();

	protected EntityAssetTypesProcessor(ModArtifactDefinition definition) {
		super(definition);
	}

	@Override
	public void apply(ModArtifact modArtifact, ParsedMod mod, Path assetDir) throws ModLoadingException {
		Map<EntityType, List<EntityAssetType>> data = new LinkedHashMap<>();
		for (Path sourceFile : modArtifact.sourceFiles) {
			try {
				JSONObject sourceJson = JSON.parseObject(FileUtils.readFileToString(sourceFile.toFile()));

				for (EntityType entityType : EntityType.values()) {
					JSONArray entityTypeJson = sourceJson.getJSONArray(entityType.name());
					if (entityTypeJson != null) {
						List<EntityAssetType> entityAssetTypes = data.computeIfAbsent(entityType, a -> new ArrayList<>());
						for (int cursor = 0; cursor < entityTypeJson.size(); cursor++) {
							EntityAssetType entityAssetType = new EntityAssetType(entityTypeJson.getString(cursor));
							entityAssetTypes.add(entityAssetType);
						}
					}
				}
			} catch (IOException | JSONException e) {
				Logger.error(e, "Could not parse EntityAssetTypes from {}", sourceFile.toString());
				throw new ModLoadingException(e);
			}
		}
		modArtifact.setData(data);
	}

	@Override
	public void combine(List<ModArtifact> artifacts, Path tempDir) {
		for (ModArtifact artifact : artifacts) {
			Map<EntityType, List<EntityAssetType>> artifactData = getData(artifact);
			for (Map.Entry<EntityType, List<EntityAssetType>> entry : artifactData.entrySet()) {
				combinedData.computeIfAbsent(entry.getKey(), e -> new ArrayList<>()).addAll(entry.getValue());
			}
		}
	}

	@Override
	public void write(Path assetsDir) throws IOException {
		if (definition.outputType.equals(SINGLE_FILE)) {
			Path filePath = assetsDir.resolve(definition.assetDir).resolve(definition.outputFileName);
			if (Files.exists(filePath)) {
				Files.delete(filePath);
			}
			String outputJson = objectMapper.writeValueAsString(combinedData);
			Files.write(filePath, outputJson.getBytes());
		} else {
			throw new RuntimeException("Not yet implemented: outputType " + definition.outputType + " for " + getClass().getSimpleName());
		}
	}

	@SuppressWarnings("unchecked")
	public Map<EntityType, List<EntityAssetType>> getData(ModArtifact modArtifact) {
		return (Map<EntityType, List<EntityAssetType>>) modArtifact.getData();
	}

}
