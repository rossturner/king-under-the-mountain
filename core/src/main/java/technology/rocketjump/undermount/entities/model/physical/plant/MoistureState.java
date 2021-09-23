package technology.rocketjump.undermount.entities.model.physical.plant;

public enum MoistureState {


	WATERED(1.3f),
	NEEDS_WATERING(0.8f),
	DROUGHTED(0.05f);

	public static final double GAME_HOURS_BETWEEN_WATER_REQUIRED = 16.0;
	public static final int DISTANCE_TO_CONSUME_WATER = 2;
	public static final double GAME_HOURS_BEFORE_DROUGHT = 24.0 * 5.0;

	public final float growthModifier;

	MoistureState(float growthModifier) {
		this.growthModifier = growthModifier;
	}

	public String getI18nKey() {
		return "CROP.MOISTURE_STATE." + name();
	}
}
