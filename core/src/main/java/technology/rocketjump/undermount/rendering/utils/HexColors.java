package technology.rocketjump.undermount.rendering.utils;

import com.badlogic.gdx.graphics.Color;

public class HexColors {

	public static final Color POSITIVE_COLOR = HexColors.get("#36ba3f");
	public static final Color NEGATIVE_COLOR = HexColors.get("#D4534C");

	public static Color get(String hexString) {
		if (hexString == null) {
			return null;
		} else if (hexString.startsWith("#") && hexString.length() == 7) {
			return new Color(
					Integer.valueOf(hexString.substring(1, 3), 16) / 255f,
					Integer.valueOf(hexString.substring(3, 5), 16) / 255f,
					Integer.valueOf(hexString.substring(5, 7), 16) / 255f,
					1
			);
		} else if (hexString.startsWith("#") && hexString.length() == 9) {
			return new Color(
					Integer.valueOf(hexString.substring(1, 3), 16) / 255f,
					Integer.valueOf(hexString.substring(3, 5), 16) / 255f,
					Integer.valueOf(hexString.substring(5, 7), 16) / 255f,
					Integer.valueOf(hexString.substring(7, 9), 16) / 255f
			);
		} else {
			return null;
		}
	}

	public static String toHexString(Color color) {
		if (color.a >= 1) {
			return "#" + color.toString().substring(0, 6).toUpperCase();
		} else {
			return "#" + color.toString().toUpperCase();
		}
	}
}
