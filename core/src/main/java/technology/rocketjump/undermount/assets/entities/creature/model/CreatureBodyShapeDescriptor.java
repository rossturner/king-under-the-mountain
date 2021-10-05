package technology.rocketjump.undermount.assets.entities.creature.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatureBodyShapeDescriptor {

	private CreatureBodyShape value;
	private Float minStrength;
	private Float maxStrength;

	public CreatureBodyShape getValue() {
		return value;
	}

	public void setValue(CreatureBodyShape value) {
		this.value = value;
	}

	public Float getMinStrength() {
		return minStrength;
	}

	public void setMinStrength(Float minStrength) {
		this.minStrength = minStrength;
	}

	public Float getMaxStrength() {
		return maxStrength;
	}

	public void setMaxStrength(Float maxStrength) {
		this.maxStrength = maxStrength;
	}
}
