package technology.rocketjump.undermount.modding.processing;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import com.fasterxml.jackson.core.JsonProcessingException;
import technology.rocketjump.undermount.environment.model.SunlightPhase;
import technology.rocketjump.undermount.modding.exception.ModLoadingException;
import technology.rocketjump.undermount.modding.model.ModArtifact;
import technology.rocketjump.undermount.modding.model.ModArtifactDefinition;
import technology.rocketjump.undermount.modding.model.ParsedMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SunlightProcessor extends UntypedJsonProcessor {

	private List<JSONObject> combinedArray = new ArrayList<>();

	public SunlightProcessor(ModArtifactDefinition definition) {
		super(definition);
	}

	@Override
	public void apply(ModArtifact modArtifact, ParsedMod mod, Path assetDir) throws ModLoadingException {
		super.apply(modArtifact, mod, assetDir);

		JSONArray dataArray = (JSONArray) modArtifact.getData();
		if (dataArray.size() != 1) {
			throw new ModLoadingException("Only expecting a single JSON file for " + definition.getName());
		}

		List<SunlightPhase> sunlightPhases = parseSunlightPhases(dataArray.getJSONObject(0));

		try {
			JSONArray newArray = JSON.parseArray(objectMapper.writeValueAsString(sunlightPhases));
			modArtifact.setData(newArray);
		} catch (JsonProcessingException e) {
			throw new ModLoadingException(e);
		}
	}

	@Override
	public void combine(List<ModArtifact> artifacts, Path tempDir) throws ModLoadingException {
		switch (definition.combinationType) {
			case REPLACES_EXISTING:
				for (ModArtifact artifact : artifacts) {
					JSONArray artifactData = (JSONArray) artifact.getData();
					combinedArray.clear();
					for (Object o : artifactData) {
						combinedArray.add((JSONObject) o);
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
						String outputJson = objectMapper.writeValueAsString(combinedArray);
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


	public List<SunlightPhase> parseSunlightPhases(JSONObject sunlightJson) throws ModLoadingException {
		List<SunlightPhase> results = new LinkedList<>();

		Map<String, Color> namedColors = new HashMap<>();
		JSONObject colorDefinitions = sunlightJson.getJSONObject("colors");
		for (String colorName : colorDefinitions.keySet()) {
			JSONArray colorValues = colorDefinitions.getJSONArray(colorName);
			if (colorValues.size() != 3) {
				throw new ModLoadingException("Color array in sunlight definition must have 3 values");
			}
			namedColors.put(colorName.toLowerCase(), new Color(
					colorValues.getIntValue(0) / 255f, colorValues.getIntValue(1) / 255f, colorValues.getIntValue(2) / 255f, 1.0f
			));
		}

		JSONArray phasesJsonArray = sunlightJson.getJSONArray("phases");

		for (int index = 0; index < phasesJsonArray.size(); index++) {
			JSONObject phaseJson = phasesJsonArray.getJSONObject(index);
			results.add(new SunlightPhase(phaseJson.getFloatValue("time"), namedColors.get(phaseJson.getString("color").toLowerCase())));
		}

		return results;
	}

}
