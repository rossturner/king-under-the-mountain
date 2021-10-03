package technology.rocketjump.undermount.entities.model.physical.creature.body;


import java.util.Objects;

public class BodyPart {

	private BodyPartDefinition partDefinition;
	private BodyPartDiscriminator discriminator; // If more than one

	public BodyPart(BodyPartDefinition partDefinition, BodyPartDiscriminator discriminator) {
		this.partDefinition = partDefinition;
		this.discriminator = discriminator;
	}

	public BodyPartDefinition getPartDefinition() {
		return partDefinition;
	}

	public BodyPartDiscriminator getDiscriminator() {
		return discriminator;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BodyPart bodyPart = (BodyPart) o;
		return partDefinition.equals(bodyPart.partDefinition) && discriminator == bodyPart.discriminator;
	}

	@Override
	public int hashCode() {
		return Objects.hash(partDefinition, discriminator);
	}

	@Override
	public String toString() {
		return (discriminator != null ? discriminator.name() + " " : "") + partDefinition.getName();
	}
}
