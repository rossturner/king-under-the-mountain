package technology.rocketjump.undermount.entities.model.physical.creature;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RaceGenderDescriptor {

	private float weighting;
	private float hasHair;

	public float getWeighting() {
		return weighting;
	}

	public void setWeighting(float weighting) {
		this.weighting = weighting;
	}

	public float getHasHair() {
		return hasHair;
	}

	public void setHasHair(float hasHair) {
		this.hasHair = hasHair;
	}
}
