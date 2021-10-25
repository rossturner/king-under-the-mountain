package technology.rocketjump.undermount.entities.model.physical.creature;

public enum DeathReason {

	STARVATION,
	DEHYDRATION,
	BURNING,
	FOOD_POISONING,
	EXHAUSTION,
	CRUSHED_BY_FALLING_DEBRIS,
	FROZEN,
	INTERNAL_BLEEDING,
	CRITICAL_ORGAN_DAMAGE,
	SUFFOCATION,
	UNKNOWN;

	public String getI18nKey() {
		return "DEATH_REASON."+name();
	}


}
