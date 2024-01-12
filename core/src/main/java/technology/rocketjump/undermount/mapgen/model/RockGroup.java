package technology.rocketjump.undermount.mapgen.model;

import com.badlogic.gdx.graphics.Color;

public enum RockGroup {

	None(100, 100, 100),
	Sedimentary(255, 255, 128),
	Igneous(255, 102, 0),
	Metamorphic(204, 51, 255);

	private final Color color;

	private RockGroup(int red, int green, int blue) {
		this.color = new Color(red / 255f, green/255f, blue/255f, 1f);
	}

	public Color getColor() {
		return color;
	}
}
