package technology.rocketjump.undermount.entities.model.physical.humanoid.body.features;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SkinFeature {

	private float hardness;

	public float getHardness() {
		return hardness;
	}

	public void setHardness(float hardness) {
		this.hardness = hardness;
	}
}
