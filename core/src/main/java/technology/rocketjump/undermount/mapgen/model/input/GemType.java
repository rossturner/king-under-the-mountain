package technology.rocketjump.undermount.mapgen.model.input;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.undermount.mapgen.model.RockGroup;

public class GemType {

	private final RockGroup rockGroup;
	private final String name;
	private final Color color;
	private final float weighting;

	public GemType(RockGroup rockGroup, String name, Color color, float weighting) {
		this.rockGroup = rockGroup;
		this.name = name;
		this.color = color;
		this.weighting = weighting;
	}

	public RockGroup getRockGroup() {
		return rockGroup;
	}

	public String getName() {
		return name;
	}

	public Color getColor() {
		return color;
	}

	public float getWeighting() {
		return weighting;
	}

	@Override
	public String toString() {
		return name + " (" + rockGroup + ")";
	}
}
