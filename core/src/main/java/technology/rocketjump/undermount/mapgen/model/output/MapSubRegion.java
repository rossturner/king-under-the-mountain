package technology.rocketjump.undermount.mapgen.model.output;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.mapgen.generators.SequentialIdGenerator;

import java.util.HashSet;
import java.util.Set;

public class MapSubRegion {

	private final TileSubType subRegionType;
	private final long subRegionId;

	private Set<GameMapTile> tiles = new HashSet<>();

	private int minX = Integer.MAX_VALUE;
	private int maxX = Integer.MIN_VALUE;
	private int minY = Integer.MAX_VALUE;
	private int maxY = Integer.MIN_VALUE;


	public MapSubRegion(TileSubType subRegionType) {
		this.subRegionId = SequentialIdGenerator.nextId();
		this.subRegionType = subRegionType;
	}

	public void add(GameMapTile tile) {
		if (!contains(tile)) {
			tile.setSubRegion(this);
			if (tile.getPosition().x < minX) {
				minX = tile.getPosition().x;
			}
			if (tile.getPosition().x > maxX) {
				maxX = tile.getPosition().x;
			}
			if (tile.getPosition().y < minY) {
				minY = tile.getPosition().y;
			}
			if (tile.getPosition().y > maxY) {
				maxY = tile.getPosition().y;
			}
		}
		tiles.add(tile);
	}

	public void remove(GameMapTile tile) {
		if (contains(tile)) {
			tiles.remove(tile);
			tile.setSubRegion(null);
		}
	}

	public boolean contains(GameMapTile tile) {
		return tiles.contains(tile);
	}

	public int getMinX() {
		return minX;
	}

	public int getMaxX() {
		return maxX;
	}

	public int getMinY() {
		return minY;
	}

	public int getMaxY() {
		return maxY;
	}

	public int size() {
		return tiles.size();
	}

	public Set<GameMapTile> getTiles() {
		return tiles;
	}

	public TileSubType getSubRegionType() {
		return subRegionType;
	}

	public long getSubRegionId() {
		return subRegionId;
	}

	public GridPoint2 getMiddle() {
		return new GridPoint2(
				((maxX - minX) / 2) + minX,
				((maxY - minY) / 2) + minY
		);
	}

}
