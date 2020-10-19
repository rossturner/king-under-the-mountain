package technology.rocketjump.undermount.modding.processing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.reflect.FieldUtils;
import technology.rocketjump.undermount.misc.Name;
import technology.rocketjump.undermount.modding.exception.ModLoadingException;
import technology.rocketjump.undermount.modding.model.ModArtifact;
import technology.rocketjump.undermount.modding.model.ModArtifactDefinition;
import technology.rocketjump.undermount.modding.model.ParsedMod;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;

public abstract class ModArtifactProcessor {

	protected final ModArtifactDefinition definition;
	protected final ObjectMapper objectMapper;

	public ModArtifactProcessor(ModArtifactDefinition definition) {
		this.definition = definition;
		objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}

	public abstract void apply(ModArtifact modArtifact, ParsedMod mod, Path assetDir) throws ModLoadingException;

	public abstract void combine(List<ModArtifact> artifacts, Path tempDir) throws ModLoadingException, IOException;

	public abstract void write(Path assetsDir) throws IOException;

	protected Field getNameField() {
		List<Field> nameFields = FieldUtils.getFieldsListWithAnnotation(definition.classType, Name.class);
		if (nameFields.size() != 1) {
			throw new RuntimeException(definition.classType.getSimpleName() + " does not have a field annotated with " + Name.class.getSimpleName());
		}
		Field field = nameFields.get(0);
		field.setAccessible(true);
		return field;
	}

}
