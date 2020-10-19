package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.graphics.Color;

import java.util.Random;

public class AccessoryColorFactory {

	public Color randomAccessoryColor(Random random) {
		int[] colors = new int[]{
				random.nextInt(128),
				random.nextInt(128),
				random.nextInt(128),
		};
		int picker = random.nextInt(3);
		colors[picker] = 128 + random.nextInt(128);
		return new Color(colors[0] / 255f, colors[1] / 255f, colors[2] / 255f, 1);
	}
}
