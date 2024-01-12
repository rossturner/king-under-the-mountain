package technology.rocketjump.undermount.mapgen.model;

public class HeightmapVertex {

	private float height = 0f; // Assume this is normalised in range 0 -> 1

	public float getHeight() {
		return height;
	}

	public void setHeight(float height) {
		this.height = height;
	}
}
