package technology.rocketjump.undermount.rendering.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

import java.util.Collection;
import java.util.Random;

public class ColorMixer {

	public static Color fromTransition(float progress, Array<Array<Color>> transitionColors, Random random) {
		Array<Color> newColors = new Array<>();

		for (Array<Color> colorSwatch : transitionColors) {
			float transitionTarget = progress * colorSwatch.size;

			int nearestColorIndex = Math.round(transitionTarget);
			float remainder = transitionTarget - nearestColorIndex;
			int otherColorIndex;
			if (remainder > 0) {
				otherColorIndex = Math.max(nearestColorIndex - 1, 0);
			} else {
				otherColorIndex = nearestColorIndex + 1;
			}

			Color nearestColor = colorSwatch.get(Math.min(nearestColorIndex, colorSwatch.size - 1)).cpy();
			Color otherColor = colorSwatch.get(Math.min(otherColorIndex, colorSwatch.size - 1)).cpy();

			float nearestColorAmount = 1 - (Math.abs(remainder));
			float otherColorAmount = 1 - nearestColorAmount;

			nearestColor.mul(nearestColorAmount);
			otherColor.mul(otherColorAmount);

			Color finalColor = nearestColor.add(otherColor);
			finalColor.a = 1;

			newColors.add(finalColor);
		}

		return ColorMixer.randomBlend(random, newColors);
	}

	public static Color randomBlend(Random random, Array<Color> colorArray) {
		float[] selectedRatio = new float[colorArray.size];
		float totalAmount = 0f;
		for (int cursor = 0; cursor < colorArray.size; cursor++) {
			float ratio = random.nextFloat();
			selectedRatio[cursor] = ratio;
			totalAmount += ratio;
		}
		Color pickedColor = new Color();
		for (int cursor = 0; cursor < colorArray.size; cursor++) {
			selectedRatio[cursor] = selectedRatio[cursor] / totalAmount;
			pickedColor.add(colorArray.get(cursor).cpy().mul(selectedRatio[cursor]));
		}
		pickedColor.a = 1;
		return pickedColor;
	}

	public static Color averageBlend(Collection<Color> colors) {
		float numColors = colors.size();
		Color averageColor = new Color();
		for (Color color : colors) {
			averageColor.add(color.cpy().mul(1f / numColors));
		}
		averageColor.a = 1;
		return averageColor;
	}

	public static Color average(Color oldColor, Color newColor) {
		return oldColor.cpy().mul(0.5f).add(newColor.cpy().mul(0.5f));
	}

	public static Color interpolate(int minValue, int maxValue, int actualValue, Color minColor, Color maxColor) {
		float interpolationAmount = (float) actualValue / ((float) maxValue - (float) minValue);
		return minColor.cpy().lerp(maxColor, interpolationAmount);
	}

	public static Color interpolate(float minValue, float maxValue, float actualValue, Color minColor, Color maxColor) {
		float interpolationAmount = actualValue / (maxValue - minValue);
		return minColor.cpy().lerp(maxColor, interpolationAmount);
	}
}
