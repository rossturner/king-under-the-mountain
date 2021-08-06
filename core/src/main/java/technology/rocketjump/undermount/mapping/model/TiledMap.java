package technology.rocketjump.undermount.mapping.model;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.mapping.tile.*;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.zones.Zone;

import java.util.*;

import static java.util.Collections.emptyList;
import static technology.rocketjump.undermount.mapping.tile.CompassDirection.*;

/**
 * Note that this class will be accessed (read only) by other (pathfinding) threads
 */
public class TiledMap {

	private final long seed;
	private final int width;
	private final int height;

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	private Array<Array<MapTile>> cells;
	private Array<Array<MapVertex>> mapVertices;

	private Map<Integer, List<MapTile>> tilesByPercentile = new HashMap<>();

	private GridPoint2 embarkPoint;

	private int numRegions;
	private Map<Integer, Map<Long, Zone>> regionsToZonesMap = new HashMap<>();

	private final FloorType defaultFloor;
	private final GameMaterial defaultFloorMaterial;

	public TiledMap(long seed, int width, int height, FloorType defaultFloor, GameMaterial defaultFloorMaterial) {
		this.seed = seed;
		this.width = width;
		this.height = height;

		this.defaultFloor = defaultFloor;
		this.defaultFloorMaterial = defaultFloorMaterial;

		cells = new Array<>(width);
		mapVertices = new Array<>(width + 1);

		Random random = new Random(seed);
		for (int x = 0; x < width; x++) {
			Array<MapTile> column = new Array<>(height);
			Array<MapVertex> vertexColumn = new Array<>(height + 1);
			for (int y = 0; y < height; y++) {
				MapTile mapTile = new MapTile(random.nextLong(), x, y, defaultFloor, defaultFloorMaterial);
				column.add(mapTile);
				tilesByPercentile.computeIfAbsent((int)Math.abs(mapTile.getSeed() % 100), a -> new ArrayList<>()).add(mapTile);
				vertexColumn.add(new MapVertex(x, y));
			}
			vertexColumn.add(new MapVertex(x, height));
			cells.add(column);
			mapVertices.add(vertexColumn);
		}

		Array<MapVertex> vertexColumn = new Array<>(height + 1);
		for (int y = 0; y < height + 1; y++) {
			vertexColumn.add(new MapVertex(width, y));
		}
		mapVertices.add(vertexColumn);

	}

	public long getSeed() {
		return seed;
	}

	public FloorType getDefaultFloor() {
		return defaultFloor;
	}

	public GameMaterial getDefaultFloorMaterial() {
		return defaultFloorMaterial;
	}

	public MapTile getTile(int tileX, int tileY) {
		if (tileX < 0 || tileX >= width) {
			return null;
		} else if (tileY < 0 || tileY >= height) {
			return null;
		} else {
			return cells.get(tileX).get(tileY);
		}
	}

	public TileNeighbours getNeighbours(GridPoint2 location) {
		return getNeighbours(location.x, location.y);
	}

	public TileNeighbours getNeighbours(int x, int y) {
		TileNeighbours neighbours = new TileNeighbours();
		for (CompassDirection direction : CompassDirection.values()) {
			MapTile cellInDirection = getTile(x + direction.getXOffset(), y + direction.getYOffset());
			if (cellInDirection != null) {
				neighbours.put(direction, cellInDirection);
			}
		}
		return neighbours;
	}

	private static final List<CompassDirection> orthogonalDirections = new LinkedList<>();
	static {
		for (CompassDirection compassDirection : CompassDirection.values()) {
			if (!compassDirection.isDiagonal()) {
				orthogonalDirections.add(compassDirection);
			}
		}
	}

	public TileNeighbours getOrthogonalNeighbours(int x, int y) {
		TileNeighbours neighbours = new TileNeighbours();
		for (CompassDirection direction : orthogonalDirections) {
			MapTile cellInDirection = getTile(x + direction.getXOffset(), y + direction.getYOffset());
			if (cellInDirection != null) {
				neighbours.put(direction, cellInDirection);
			}
		}
		return neighbours;
	}

	public MapTile getTile(MapVertex vertex, CompassDirection cellDirectionFromVertex) {
		if (cellDirectionFromVertex.equals(CompassDirection.SOUTH_WEST)) {
			return getTile(vertex.getVertexX() - 1, vertex.getVertexY() - 1);
		} else if (cellDirectionFromVertex.equals(NORTH_WEST)) {
			return getTile(vertex.getVertexX() - 1, vertex.getVertexY());
		} else if (cellDirectionFromVertex.equals(CompassDirection.NORTH_EAST)) {
			return getTile(vertex.getVertexX(), vertex.getVertexY());
		} else if (cellDirectionFromVertex.equals(CompassDirection.SOUTH_EAST)) {
			return getTile(vertex.getVertexX(), vertex.getVertexY() - 1);
		} else {
			throw new IllegalArgumentException(cellDirectionFromVertex + " does not make sense for a tile to access from vertex (" + vertex.getVertexX() + ", " + vertex.getVertexY());
		}
	}

	public MapVertex getVertex(int vertexX, int vertexY) {
		return mapVertices.get(vertexX).get(vertexY);
	}

	public MapVertex getVertex(MapTile cell, CompassDirection vertexDirectionFromCell) {
		if (vertexDirectionFromCell.equals(CompassDirection.SOUTH_WEST)) {
			return getVertex(cell.getTileX(), cell.getTileY());
		} else if (vertexDirectionFromCell.equals(NORTH_WEST)) {
			return getVertex(cell.getTileX(), cell.getTileY() + 1);
		} else if (vertexDirectionFromCell.equals(CompassDirection.NORTH_EAST)) {
			return getVertex(cell.getTileX() + 1, cell.getTileY() + 1);
		} else if (vertexDirectionFromCell.equals(CompassDirection.SOUTH_EAST)) {
			return getVertex(cell.getTileX() + 1, cell.getTileY());
		} else {
			throw new IllegalArgumentException(vertexDirectionFromCell + " does not make sense for a vertex to access from tile (" + cell.getTileX() + ", " + cell.getTileY());
		}
	}

	public MapVertex[] getVertices(int tileX, int tileY) {
		return new MapVertex[] {
			getVertex(tileX, tileY),
			getVertex(tileX, tileY + 1),
			getVertex(tileX + 1, tileY + 1),
			getVertex(tileX + 1, tileY),
		};
	}

	public TileNeighbours getTileNeighboursOfVertex(MapVertex vertex) {
		TileNeighbours neighbours = new TileNeighbours();
		neighbours.put(NORTH_WEST, getTile(vertex.getVertexX(), vertex.getVertexY()));
		neighbours.put(CompassDirection.NORTH_EAST, getTile(vertex.getVertexX() - 1, vertex.getVertexY()));
		neighbours.put(CompassDirection.SOUTH_WEST, getTile(vertex.getVertexX() - 1, vertex.getVertexY() - 1));
		neighbours.put(CompassDirection.SOUTH_EAST, getTile(vertex.getVertexX(), vertex.getVertexY() - 1));
		return neighbours;
	}

	public EnumMap<CompassDirection, MapVertex> getVertexNeighboursOfCell(MapTile cell) {
		EnumMap<CompassDirection, MapVertex> neighbourVertices = new EnumMap<>(CompassDirection.class);

		neighbourVertices.put(NORTH_WEST, getVertex(cell, NORTH_WEST));
		neighbourVertices.put(NORTH_EAST, getVertex(cell, NORTH_EAST));
		neighbourVertices.put(SOUTH_EAST, getVertex(cell, SOUTH_EAST));
		neighbourVertices.put(SOUTH_WEST, getVertex(cell, SOUTH_WEST));

		return neighbourVertices;
	}

	public MapTile getTile(GridPoint2 tilePosition) {
		if (tilePosition == null) {
			return null;
		}
		return getTile(tilePosition.x, tilePosition.y);
	}

	public MapTile getTile(Vector2 worldPosition) {
		if (worldPosition == null) {
			return null;
		} else {
			return getTile(MathUtils.floor(worldPosition.x), MathUtils.floor(worldPosition.y));
		}
	}

	/**
	 * This method returns the 4 nearest tiles to worldPosition, for more granular locality checks
	 */
	public Array<MapTile> getNearestTiles(Vector2 worldPosition) {
		Array<MapTile> nearest = new Array<>(4);
		if (worldPosition == null) {
			return nearest;
		}

		int worldTileX = MathUtils.floor(worldPosition.x);
		int worldTileY = MathUtils.floor(worldPosition.y);

		float subTileOffsetX = worldPosition.x - worldTileX;
		float subTileOffsetY = worldPosition.y - worldTileY;

		int xOffset = (subTileOffsetX < 0.5f) ? -1 : +1;
		int yOffset = (subTileOffsetY < 0.5f) ? -1 : +1;

		addTileTo(nearest, worldTileX, 				worldTileY);
		addTileTo(nearest, worldTileX + xOffset, 	worldTileY);
		addTileTo(nearest, worldTileX, 				worldTileY + yOffset);
		addTileTo(nearest, worldTileX + xOffset, 	worldTileY + yOffset);

		return nearest;
	}

	private void addTileTo(Array<MapTile> nearest, int tileX, int tileY) {
		MapTile tile = getTile(tileX, tileY);
		if (tile != null) {
			nearest.add(tile);
		} else {
			nearest.add(new NullMapTile(tileX, tileY));
		}
	}

	public GridPoint2 getEmbarkPoint() {
		return embarkPoint;
	}

	public void setEmbarkPoint(GridPoint2 embarkPoint) {
		this.embarkPoint = embarkPoint;
	}

	public int getNumRegions() {
		return numRegions;
	}

	public void setNumRegions(int numRegions) {
		this.numRegions = numRegions;
	}

	public int createNewRegionId() {
		return ++numRegions;
	}

	public Collection<Zone> getZonesInRegion(int regionId) {
		return regionsToZonesMap.computeIfAbsent(regionId, x -> new HashMap<>()).values();
	}

	public void removeZone(Zone zoneToRemove) {
		Zone removedZone = regionsToZonesMap.get(zoneToRemove.getRegionId()).remove(zoneToRemove.getZoneId());
		if (removedZone == null) {
			Logger.error("Could not find zone to remove with ID " + zoneToRemove.getZoneId());
		} else {
			removedZone.removeFromAllTiles(this);
		}
	}

	public void addZone(Zone movedZone) {
		regionsToZonesMap.computeIfAbsent(movedZone.getRegionId(), x -> new HashMap<>()).put(movedZone.getZoneId(), movedZone);
		movedZone.addToAllTiles(this);
	}

	public List<MapTile> getTilesForPercentile(int percentile) {
		return tilesByPercentile.getOrDefault(percentile, emptyList());
	}
}
