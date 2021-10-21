package technology.rocketjump.undermount.entities.model.physical.creature.body;


import technology.rocketjump.undermount.entities.model.physical.creature.body.organs.OrganDamageLevel;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

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

	public Optional<BodyPartOrgan> rollToHitOrgan(Random random, BodyPartDamage existingDamage) {
		if (partDefinition.getOrgans().isEmpty()) {
			return Optional.empty();
		} else {
			float roll = random.nextFloat();
			for (BodyPartOrgan organ : partDefinition.getOrgans()) {
				if (!existingDamage.getOrganDamageLevel(organ).equals(OrganDamageLevel.DESTROYED)) {
					roll -= organ.getRelativeSize();
					if (roll < 0) {
						return Optional.of(organ);
					}
				}
			}
			return Optional.empty();
		}
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
