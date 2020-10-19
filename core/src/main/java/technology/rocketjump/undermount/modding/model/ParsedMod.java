package technology.rocketjump.undermount.modding.model;

import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

public class ParsedMod {

	private final Map<ModArtifactDefinition, ModArtifact> artifacts = new TreeMap<>();
	private final Path basePath;
	private final ModInfo info;

	public ParsedMod(Path basePath, ModInfo info) {
		this.basePath = basePath;
		this.info = info;
	}

	public void add(ModArtifact artifact) {
		artifacts.put(artifact.artifactDefinition, artifact);
	}

	public ModArtifact get(ModArtifactDefinition artifactDefinition) {
		return artifacts.get(artifactDefinition);
	}

	public Path getBasePath() {
		return basePath;
	}

	public ModInfo getInfo() {
		return info;
	}

	@Override
	public String toString() {
		if (info != null) {
			return info.toString();
		} else {
			return basePath.toString();
		}
	}
}
