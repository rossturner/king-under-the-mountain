package technology.rocketjump.undermount.modding.model;

import technology.rocketjump.undermount.modding.processing.ModArtifactProcessor;
import technology.rocketjump.undermount.modding.validation.ModArtifactValidator;

import java.util.Arrays;
import java.util.List;

public class ModArtifactDefinition implements Comparable<ModArtifactDefinition> {

	public final String assetDir;

	public final String outputFileName;
	public final OutputType outputType;
	public final ModFileType outputFileType;

	public final Class<?> classType;
	public final Class<? extends ModArtifactProcessor> processor;
	public final List<Class<? extends ModArtifactValidator>> validators;

	public final String modDir;
	public final String inputFileNameMatcher;
	public final ModFileType inputFileType;

	public final ArtifactCombinationType combinationType;

	// Optional attributes accessed by setters
	private String packJsonPath;

	public ModArtifactDefinition(String assetDir, String outputFileName, OutputType outputType, ModFileType outputFileType,
								 Class<?> classType, String modDir, String inputFileNameMatcher, ModFileType inputFileType,
								 ArtifactCombinationType combinationType,
								 Class<? extends ModArtifactProcessor> processor, Class<? extends ModArtifactValidator>... validators) {
		this.assetDir = assetDir;

		this.outputFileName = outputFileName;
		this.outputType = outputType;
		this.outputFileType = outputFileType;

		this.classType = classType;

		this.modDir = modDir;
		this.inputFileNameMatcher = inputFileNameMatcher;
		this.inputFileType = inputFileType;

		this.combinationType = combinationType;
		this.processor = processor;
		this.validators = Arrays.asList(validators);
	}

	public String getName() {
		return assetDir + "/" + (outputFileName == null ? "*" : outputFileName) + outputFileType.fileExtension;
	}

	public void setPackJsonPath(String packJsonPath) {
		this.packJsonPath = packJsonPath;
	}

	public String getPackJsonPath() {
		return packJsonPath;
	}

	@Override
	public int compareTo(ModArtifactDefinition o) {
		return this.getName().compareTo(o.getName());
	}

	@Override
	public boolean equals(Object other) {
		if (other != null && other instanceof ModArtifactDefinition) {
			return this.getName().equals(((ModArtifactDefinition) other).getName());
		} else {
			return false;
		}
	}

	public enum OutputType {

		SINGLE_FILE,
		COPY_ORIGINAL_FILES,
		SPECIAL

	}

	public enum ModFileType {

		JSON_ARRAY(".json"),
		JSON_OBJECT(".json"),
		JSON_MAP(".json"), // special case where object keys map as names
		JSON_KEY_VALUES(".json"), // special case where every key is an individually-overwriteable property
		CSV(".csv"),
		WAV(".wav"),
		OGG(".ogg"),
		GLSL(".glsl"),
		TTF(".ttf"),
		PNG(".png"),
		PNG_PLUS_NORMALS(".png"),
		PACKR_ATLAS_PLUS_NORMALS(".atlas"),
		PACKR_ATLAS(".atlas");

		public final String fileExtension;

		ModFileType(String fileExtension) {
			this.fileExtension = fileExtension;
		}

	}

	public enum ArtifactCombinationType {

		ADDITIVE, // Note that this still replaces based on a matching name // TODO specify unique name field of a type
		REPLACES_EXISTING

	}

	@Override
	public String toString() {
		return getName();
	}

}
