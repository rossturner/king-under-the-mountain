package technology.rocketjump.undermount.modding;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.misc.versioning.Version;
import technology.rocketjump.undermount.modding.model.ModArtifact;
import technology.rocketjump.undermount.modding.model.ModArtifactDefinition;
import technology.rocketjump.undermount.modding.model.ModArtifactListing;
import technology.rocketjump.undermount.modding.model.ParsedMod;

@Singleton
public class ModCompatibilityChecker {

	private final ArtifactCompatibilityChecker artifactCompatibilityChecker;
	private final ModArtifactListing modArtifactListing;

	@Inject
	public ModCompatibilityChecker(ArtifactCompatibilityChecker artifactCompatibilityChecker, ModArtifactListing modArtifactListing) {
		this.artifactCompatibilityChecker = artifactCompatibilityChecker;
		this.modArtifactListing = modArtifactListing;
	}

	public Compatibility checkCompatibility(ParsedMod mod) {
		Version modGameVersion = mod.getInfo().getGameVersion();
		if (modGameVersion == null) {
			return Compatibility.UNKNOWN;
		}

		boolean allArtifactsCompatible = true;
		for (ModArtifactDefinition artifactDefinition : modArtifactListing.getAll()) {
			ModArtifact modArtifact = mod.get(artifactDefinition);
			if (modArtifact != null) {
				allArtifactsCompatible = artifactCompatibilityChecker.checkCompatibility(modArtifact, modGameVersion);
				if (!allArtifactsCompatible) {
					break;
				}
			}
		}

		return allArtifactsCompatible ? Compatibility.COMPATIBLE : Compatibility.INCOMPATIBLE;
	}

	public enum Compatibility {

		COMPATIBLE,
		INCOMPATIBLE,
		UNKNOWN;

		public String getI18nKey() {
			return "MODS.COMPATIBILITY."+this.name();
		}

	}
}
