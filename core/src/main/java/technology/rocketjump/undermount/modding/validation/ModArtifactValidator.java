package technology.rocketjump.undermount.modding.validation;

import technology.rocketjump.undermount.modding.exception.ModLoadingException;
import technology.rocketjump.undermount.modding.model.ModArtifact;
import technology.rocketjump.undermount.modding.model.ParsedMod;

public interface ModArtifactValidator {

	default void apply(ModArtifact modArtifact, ParsedMod mod) throws ModLoadingException {
	}
}
