package technology.rocketjump.undermount.entities.model.physical.creature.body;

public enum BodyPartDamageLevel {

	Destroyed(10, 0, 0.9f, 0.8f),
	BrokenBones(8, 3, 0.8f, 0.4f),
	Bleeding(3, 2, 0.5f, 0.04f),
	Bruised(1, 1, 0.2f, 0),
	None(0, 0, 0f, 0);

	public final int damageRequiredToCause;
	public final int furtherDamageModifier;
	public final float chanceToCauseStun;
	public final float chanceToGoUnconscious;

	BodyPartDamageLevel(int damageRequiredToCause, int furtherDamageModifier, float chanceToCauseStun, float chanceToGoUnconscious) {
		this.damageRequiredToCause = damageRequiredToCause;
		this.furtherDamageModifier = furtherDamageModifier;
		this.chanceToCauseStun = chanceToCauseStun;
		this.chanceToGoUnconscious = chanceToGoUnconscious;
	}

	public static BodyPartDamageLevel getForDamageAmount(int damageAmount) {
		for (BodyPartDamageLevel value : BodyPartDamageLevel.values()) {
			if (damageAmount >= value.damageRequiredToCause) {
				return value;
			}
		}
		return None;
	}

	public boolean isGreaterThan(BodyPartDamageLevel other) {
		return this.damageRequiredToCause > other.damageRequiredToCause;
	}

	public String i18nKey() {
		return "BODY_STRUCTURE.DAMAGE."+name().toUpperCase();
	}
}