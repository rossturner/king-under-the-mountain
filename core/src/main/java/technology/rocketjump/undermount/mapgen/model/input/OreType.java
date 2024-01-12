package technology.rocketjump.undermount.mapgen.model.input;

import com.badlogic.gdx.graphics.Color;

public class OreType {

	private String oreName;
	private Color oreColor;
	private float weighting; // Doesn't need to be normalised, just relative to other amounts

	public OreType(String oreName, Color oreColor, float weighting) {
		this.oreName = oreName;
		this.oreColor = oreColor;
		this.weighting = weighting;
	}

	public String getOreName() {
		return oreName;
	}

	public Color getOreColor() {
		return oreColor;
	}

	public float getWeighting() {
		return weighting;
	}

	@Override
	public String toString() {
		return oreName;
	}
}
