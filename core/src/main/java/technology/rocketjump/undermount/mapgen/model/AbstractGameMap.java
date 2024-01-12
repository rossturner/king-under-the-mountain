package technology.rocketjump.undermount.mapgen.model;

/**
 * Base class for different types of map e.g. heightmap, terrain map
 */
public abstract class AbstractGameMap {

	protected final int width, height;

	public AbstractGameMap(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public float getAspectRatio() {
		return ((float)width) / ((float)height);
	}

}
