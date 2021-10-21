package technology.rocketjump.undermount.entities.model.physical.creature.body;

public enum BodyPartDamageLevel {

	Destroyed(10, 0),
	BrokenBones(8, 3),
	Bleeding(3, 2),
	Bruised(1, 1),
	None(0, 0);

	public final int damageRequiredToCause;
	public final int furtherDamageModifier;

	BodyPartDamageLevel(int damageRequiredToCause, int furtherDamageModifier) {
		this.damageRequiredToCause = damageRequiredToCause;
		this.furtherDamageModifier = furtherDamageModifier;
	}

	public static BodyPartDamageLevel getForDamageAmount(int damageAmount) {
		for (BodyPartDamageLevel value : BodyPartDamageLevel.values()) {
			if (damageAmount >= value.damageRequiredToCause) {
				return value;
			}
		}
		return None;
	}
}