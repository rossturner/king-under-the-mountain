package technology.rocketjump.undermount.mapgen.model.input;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.undermount.mapgen.model.RockGroup;

import java.util.ArrayList;
import java.util.List;

public class RockType {

	private final RockGroup rockGroup;
	private final String name;
	private final Color color;
	private final float weighting;

	private List<OreType> oreTypes = new ArrayList<>();

	public RockType(RockGroup rockGroup, String name, Color color, float weighting) {
		this.rockGroup = rockGroup;
		this.name = name;
		this.color = color;
		this.weighting = weighting;
	}

	public void addOreType(OreType... oreTypes) {
		for (OreType oreType : oreTypes) {
			this.oreTypes.add(oreType);
		}
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

	public List<OreType> getOreTypes() {
		return oreTypes;
	}

	@Override
	public String toString() {
		return name + " (" + rockGroup + ")";
	}
}
