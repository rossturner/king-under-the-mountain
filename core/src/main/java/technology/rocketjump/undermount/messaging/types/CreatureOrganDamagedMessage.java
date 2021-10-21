package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.body.BodyPart;
import technology.rocketjump.undermount.entities.model.physical.creature.body.BodyPartOrgan;
import technology.rocketjump.undermount.entities.model.physical.creature.body.organs.OrganDamageLevel;

public class CreatureOrganDamagedMessage {

	public final Entity targetEntity;
	public final BodyPart impactedBodyPart;
	public final BodyPartOrgan impactedOrgan;
	public final OrganDamageLevel organDamageLevel;

	public CreatureOrganDamagedMessage(Entity targetEntity, BodyPart impactedBodyPart, BodyPartOrgan impactedOrgan, OrganDamageLevel organDamageLevel) {
		this.targetEntity = targetEntity;
		this.impactedBodyPart = impactedBodyPart;
		this.impactedOrgan = impactedOrgan;
		this.organDamageLevel = organDamageLevel;
	}
}
