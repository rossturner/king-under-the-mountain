package technology.rocketjump.undermount.modding.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModCollection {

	private List<ParsedMod> modsInOrder = new ArrayList<>();

	public List<ModArtifact> getArtifacts(ModArtifactDefinition artifactDefinition) {
		List<ModArtifact> artifacts = new ArrayList<>();
		for (ParsedMod mod : modsInOrder) {
			ModArtifact artifact = mod.get(artifactDefinition);
			if (artifact != null) {
				artifacts.add(artifact);
			}
		}
		return artifacts;
	}

	public long getChecksum(ModArtifactDefinition artifactDefinition) throws IOException {
		long total = 0;
		for (ParsedMod mod : modsInOrder) {
			ModArtifact artifact = mod.get(artifactDefinition);
			if (artifact != null) {
				total += artifact.checksum();
			}
		}
		return total;
	}

	public void add(ParsedMod mod) {
		modsInOrder.add(mod);
	}


	public List<ParsedMod> getAll() {
		return modsInOrder;
	}
}
