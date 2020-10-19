package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HairColorFactory {

	private List<Color> baseColors = new ArrayList<>();

	public HairColorFactory() {
		// Colors taken from http://www.collectedwebs.com/art/colors/hair/
		// MODDING Extract this to a mod file

		baseColors.add(new Color(9f/255f, 8f/255f, 6f/255f, 1f)); // Black

		baseColors.add(new Color(113f/255f, 99f/255f, 90f/255f, 1f)); // Dark grey
		baseColors.add(new Color(183f/255f, 166f/255f, 158f/255f, 1f)); // Medium grey
		baseColors.add(new Color(214f/255f, 196f/255f, 194f/255f, 1f)); // Light grey

		baseColors.add(new Color(255f/255f, 245f/255f, 225f/255f, 1f)); // White Blonde
		baseColors.add(new Color(230f/255f, 206f/255f, 168f/255f, 1f)); // Light Blonde
		baseColors.add(new Color(222f/255f, 188f/255f, 153f/255f, 1f)); // Ash Blonde
		baseColors.add(new Color(184f/255f, 151f/255f, 120f/255f, 1f)); // Honey Blonde
		baseColors.add(new Color(165f/255f, 107f/255f, 070f/255f, 1f)); // Strawberry Blonde

		baseColors.add(new Color(181f/255f, 82f/255f, 57f/255f, 1f)); // Light red
		baseColors.add(new Color(141f/255f, 74f/255f, 67f/255f, 1f)); // Dark red

		baseColors.add(new Color(145f/255f, 85f/255f, 61f/255f, 1f)); // Light auburn
		baseColors.add(new Color(83f/255f, 61f/255f, 50f/255f, 1f)); // Dark auburn

		baseColors.add(new Color(83f/255f, 61f/255f, 50f/255f, 1f)); // Dark brown
		baseColors.add(new Color(78f/255f, 67f/255f, 63f/255f, 1f)); // Medium brown
		baseColors.add(new Color(80f/255f, 68f/255f, 68f/255f, 1f)); // Chestnut brown
		baseColors.add(new Color(106f/255f, 78f/255f, 66f/255f, 1f)); // Brown
		baseColors.add(new Color(151f/255f, 121f/255f, 97f/255f, 1f)); // Ash brown

	}

	public Color randomHairColor(Random random) {
		return baseColors.get(random.nextInt(baseColors.size())).cpy();
	}
}
