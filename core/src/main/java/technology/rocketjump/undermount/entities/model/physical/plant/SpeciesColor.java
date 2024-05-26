package technology.rocketjump.undermount.entities.model.physical.plant;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.Array;
import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.rendering.utils.ColorMixer;

import java.util.Random;

public class SpeciesColor {

	private String swatch;
	private String transitionSwatch;
	private String colorChart;
	private String colorCode;
	private boolean hidden = false;

	@JsonIgnore
	private final Array<Array<Color>> transitionColors = new Array<>();
	@JsonIgnore
	private final Array<Color> swatchColors = new Array<>();
	@JsonIgnore
	private Color specificColor = Color.TEAL;

	public Color getColor(float currentStageProgress, long seed) {
		Random random = new RandomXS128(seed);
		if (swatch != null) {
			return ColorMixer.randomBlend(random, swatchColors);
		} else if (colorChart != null && swatchColors.notEmpty()) {
			return swatchColors.get(random.nextInt(swatchColors.size));
		} else if (transitionSwatch != null) {
			return ColorMixer.fromTransition(currentStageProgress, transitionColors, random);
		} else if (hidden) {
			return Color.CLEAR;
		} else {
			return specificColor;
		}
	}

	public Color getColor(long seed) {
		if (swatch != null) {
			Random random = new RandomXS128(seed);
			return ColorMixer.randomBlend(random, swatchColors);
		} else if (colorChart != null && swatchColors.notEmpty()) {
			Random random = new RandomXS128(seed);
			return swatchColors.get(random.nextInt(swatchColors.size));
		} else if (hidden) {
			return Color.CLEAR;
		} else {
			return specificColor;
		}
	}

	public String getSwatch() {
		return swatch;
	}

	public void setSwatch(String swatch) {
		this.swatch = swatch;
	}

	public String getTransitionSwatch() {
		return transitionSwatch;
	}

	public void setTransitionSwatch(String transitionSwatch) {
		this.transitionSwatch = transitionSwatch;
	}

	public String getColorCode() {
		return colorCode;
	}

	public void setColorCode(String colorCode) {
		this.colorCode = colorCode;
	}

	public Array<Array<Color>> getTransitionColors() {
		return transitionColors;
	}

	public Array<Color> getSwatchColors() {
		return swatchColors;
	}

	public Color getSpecificColor() {
		return specificColor;
	}

	public void setSpecificColor(Color specificColor) {
		this.specificColor = specificColor;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public String getColorChart() {
		return colorChart;
	}

	public void setColorChart(String colorChart) {
		this.colorChart = colorChart;
	}
}
