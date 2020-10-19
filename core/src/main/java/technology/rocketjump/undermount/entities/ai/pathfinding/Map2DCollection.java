package technology.rocketjump.undermount.entities.ai.pathfinding;

import com.badlogic.gdx.utils.IntMap;

/**
 * This class stores a subset of map locations by indexing them as
 * mapWidth * yPosition + xPosition
 */
public class Map2DCollection<T> {

	private IntMap<T> items = new IntMap<>();
	private final int mapWidth;

	public Map2DCollection(int mapWidth) {
		this.mapWidth = mapWidth;
	}

	public T get(int x, int y) {
		return items.get(toMapKey(x, y));
	}

	public void add(int x, int y, T item) {
		items.put(toMapKey(x, y), item);
	}

	private int toMapKey(int x, int y) {
		return (mapWidth * y) + x;
	}

	public int size() {
		return items.size;
	}

	public boolean isEmpty() {
		return items.size == 0;
	}

}
