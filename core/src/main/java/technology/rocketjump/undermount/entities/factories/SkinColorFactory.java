package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class SkinColorFactory {

	private List<Color> skinColors = new ArrayList<>();

	public SkinColorFactory() {
		// Taken from https://s-media-cache-ak0.pinimg.com/564x/4c/e8/41/4ce841fb97276eff65e9ac03249bafbf.jpg
		List<String> rgbStrings = asList(
//				"5a453c",
//				"695046",
//				"785c50",
				"87675a",
				"967264",
				"a57e6e",
				"b48a78",
				"c39582",
				"d2a18c",
				"e1ac96",
				"f0b8a0",
				"ffc3aa",
				"ffceb4",
				"ffdabe",
				"ffe5c8");

		this.skinColors = rgbStrings.stream().map(rbgString ->
				new Color(
						Integer.valueOf(rbgString.substring(0, 2), 16) / 255f,
						Integer.valueOf(rbgString.substring(2, 4), 16) / 255f,
						Integer.valueOf(rbgString.substring(4, 6), 16) / 255f,
						1))
		.collect(Collectors.toList());
	}

	public Color randomSkinColor(Random random) {
		return skinColors.get(random.nextInt(skinColors.size())).cpy();
	}

}
