package technology.rocketjump.undermount.modding;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.undermount.misc.versioning.Version;
import technology.rocketjump.undermount.modding.model.ModArtifact;
import technology.rocketjump.undermount.modding.model.ModArtifactDefinition;
import technology.rocketjump.undermount.modding.model.ModArtifactListing;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class is used to query individual mod artifacts for their version compatibility
 */
@Singleton
public class ArtifactCompatibilityChecker {

	public static final String FILE_PATH = "assets/metadata/compatibility.json";
	private final ObjectMapper objectMapper;

	private final Map<String, Version> artifactNamesToVersions = new LinkedHashMap<>();

	@Inject
	public ArtifactCompatibilityChecker(ModArtifactListing artifactListing) throws IOException {
		objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		// LOAD in from file
		File jsonFile = new File(FILE_PATH);

		JSONObject compatibilityJson = JSON.parseObject(FileUtils.readFileToString(jsonFile));

		// add to map
		for (ModArtifactDefinition artifactDefinition : artifactListing.getAll()) {
			Version artifactVersion = GlobalSettings.VERSION; // default to current game version
			String artifactName = artifactDefinition.getName();
			if (compatibilityJson.containsKey(artifactName)) {
				artifactVersion = new Version(compatibilityJson.getString(artifactName));
			}

			artifactNamesToVersions.put(artifactName, artifactVersion);
		}

		// write back out
		Map<String, String> outputVersion = new LinkedHashMap<>();
		for (Map.Entry<String, Version> entry : artifactNamesToVersions.entrySet()) {
			outputVersion.put(entry.getKey(), entry.getValue().toString());
		}
		FileUtils.writeStringToFile(jsonFile, objectMapper.writeValueAsString(outputVersion));
	}

	public Version getVersion(ModArtifactDefinition artifactDefinition) {
		return artifactNamesToVersions.get(artifactDefinition.toString());
	}

	public boolean checkCompatibility(ModArtifact modArtifact, Version modGameVersion) {
		Version lastCompatibleVersion = getVersion(modArtifact.artifactDefinition);
		return modGameVersion.toInteger() >= lastCompatibleVersion.toInteger();
	}
}
