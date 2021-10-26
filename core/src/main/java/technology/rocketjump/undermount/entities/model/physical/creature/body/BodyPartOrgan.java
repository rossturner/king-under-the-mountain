package technology.rocketjump.undermount.entities.model.physical.creature.body;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.entities.model.physical.creature.body.organs.OrganDefinition;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BodyPartOrgan implements ChildPersistable {

	private String type;
	@JsonIgnore
	private OrganDefinition organDefinition;
	private BodyPartDiscriminator discriminator;
	private float relativeSize;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public OrganDefinition getOrganDefinition() {
		return organDefinition;
	}

	public void setOrganDefinition(OrganDefinition organDefinition) {
		this.organDefinition = organDefinition;
	}

	public float getRelativeSize() {
		return relativeSize;
	}

	public void setRelativeSize(float relativeSize) {
		this.relativeSize = relativeSize;
	}

	public BodyPartDiscriminator getDiscriminator() {
		return discriminator;
	}

	public void setDiscriminator(BodyPartDiscriminator discriminator) {
		this.discriminator = discriminator;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BodyPartOrgan that = (BodyPartOrgan) o;
		return organDefinition.equals(that.organDefinition) && discriminator == that.discriminator;
	}

	@Override
	public int hashCode() {
		return Objects.hash(organDefinition, discriminator);
	}

	@Override
	public String toString() {
		return (discriminator != null ? discriminator.name() + " " : "") + organDefinition.getName();
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("type", organDefinition.getName());
		if (discriminator != null) {
			asJson.put("discriminator", discriminator.name());
		}
		asJson.put("size", relativeSize);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.type = asJson.getString("type");
		this.organDefinition = relatedStores.organDefinitionDictionary.getByName(this.type);
		if (this.organDefinition == null) {
			throw new InvalidSaveException("Could not find organ by name " + type);
		}
		this.discriminator = EnumParser.getEnumValue(asJson, "discriminator", BodyPartDiscriminator.class, null);
		this.relativeSize = asJson.getFloatValue("size");
	}
}
