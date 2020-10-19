package technology.rocketjump.undermount.environment.model;

import com.badlogic.gdx.graphics.Color;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SunlightPhase {

	private final double time;
	private final Color color;

	@JsonCreator
	public SunlightPhase(@JsonProperty("time") double time, @JsonProperty("color") Color color) {
		this.time = time;
		this.color = color;
	}

	public double getTime() {
		return time;
	}

	public Color getColor() {
		return color;
	}

}
