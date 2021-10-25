package technology.rocketjump.undermount.entities.model.physical.creature.features;

import com.badlogic.gdx.graphics.Color;
import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.rendering.utils.HexColors;

public class BloodFeature {

	private static final String DEFAULT_BLOOD_COLOR = "#e62d23";

	private String colorCode = DEFAULT_BLOOD_COLOR;
	@JsonIgnore
	private Color color = HexColors.get(DEFAULT_BLOOD_COLOR);

	public String getColorCode() {
		return colorCode;
	}

	public void setColorCode(String colorCode) {
		this.colorCode = colorCode;
		this.color = HexColors.get(colorCode);
	}

	public Color getColor() {
		return color;
	}
}
