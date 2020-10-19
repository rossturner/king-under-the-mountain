package technology.rocketjump.undermount.entities.ai.goap;

public enum EntityNeed {

	FOOD,
	DRINK,
	SLEEP;

	public String getI18nKey() {
		return "NEEDS." + name();
	}

}
