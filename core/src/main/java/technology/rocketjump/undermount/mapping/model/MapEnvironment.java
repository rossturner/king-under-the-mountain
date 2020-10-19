package technology.rocketjump.undermount.mapping.model;

/**
 * This class is to store data such as the ambient temperature, weather, etc.
 */
public class MapEnvironment {

	private float sunlightAmount = 1;

	public float getSunlightAmount() {
		return sunlightAmount;
	}

	public void setSunlightAmount(float sunlightAmount) {
		this.sunlightAmount = sunlightAmount;
	}
}
