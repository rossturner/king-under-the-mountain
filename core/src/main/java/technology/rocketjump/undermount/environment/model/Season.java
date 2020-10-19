package technology.rocketjump.undermount.environment.model;

public enum Season {

	SPRING,
	SUMMER,
	AUTUMN,
	WINTER;

	private final String translationKey;
	private Season nextSeason;

	static {
		SPRING.nextSeason = SUMMER;
		SUMMER.nextSeason = AUTUMN;
		AUTUMN.nextSeason = WINTER;
		WINTER.nextSeason = SPRING;
	}

	Season() {
		this.translationKey = "SEASON."+this.name();
	}

	public Season getNext() {
		return nextSeason;
	}

	public String getI18nKey() {
		return translationKey;
	}
}
