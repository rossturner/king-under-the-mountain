package technology.rocketjump.undermount.entities.model.physical.humanoid;

public enum DeathReason {

	STARVATION,
	DEHYDRATION,
	BURNING,
	FOOD_POISONING,
	EXHAUSTION,
	CRUSHED_BY_FALLING_DEBRIS,
	UNKNOWN;

	public String getI18nKey() {
		return "DEATH_REASON."+name();
	}


}
