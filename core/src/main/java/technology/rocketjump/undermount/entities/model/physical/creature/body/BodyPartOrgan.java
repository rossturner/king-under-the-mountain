package technology.rocketjump.undermount.entities.model.physical.creature.body;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.entities.model.physical.creature.body.organs.OrganDefinition;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BodyPartOrgan {

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
}
