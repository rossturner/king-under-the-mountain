package technology.rocketjump.undermount.entities.model.physical.creature.body.organs;

public enum OrganDamageLevel {

	DESTROYED(5, 0),
	HEAVY(3, 2),
	LIGHT(1, 1),
	NONE(0, 0);

	public final int damageRequiredToCause;
	public final int furtherDamageModifier;

	OrganDamageLevel(int damageRequiredToCause, int furtherDamageModifier) {
		this.damageRequiredToCause = damageRequiredToCause;
		this.furtherDamageModifier = furtherDamageModifier;
	}

	public static OrganDamageLevel getForDamageAmount(int damageAmount) {
		for (OrganDamageLevel value : OrganDamageLevel.values()) {
			if (damageAmount >= value.damageRequiredToCause) {
				return value;
			}
		}
		return NONE;
	}

	public boolean isGreaterThan(OrganDamageLevel other) {
		return this.damageRequiredToCause > other.damageRequiredToCause;
	}

	public String i18nKey() {
		return "BODY_STRUCTURE.ORGAN_DAMAGE."+name().toUpperCase();
	}
}
