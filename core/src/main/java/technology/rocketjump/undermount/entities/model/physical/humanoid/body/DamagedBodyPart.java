package technology.rocketjump.undermount.entities.model.physical.humanoid.body;

public class DamagedBodyPart {

	private BodyPart bodyPart;
	private BodyPartDamage damage;

	public BodyPart getBodyPart() {
		return bodyPart;
	}

	public void setBodyPart(BodyPart bodyPart) {
		this.bodyPart = bodyPart;
	}

	public BodyPartDamage getDamage() {
		return damage;
	}

	public void setDamage(BodyPartDamage damage) {
		this.damage = damage;
	}
}
