package technology.rocketjump.undermount.mapgen.model.output;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.mapgen.generators.SequentialIdGenerator;
import technology.rocketjump.undermount.mapgen.model.AbstractGameMap;

import java.util.*;

import static technology.rocketjump.undermount.mapgen.model.output.CompassDirection.*;

/**
 * This class represents a contiguous set of game map tiles that all either mountain or outside tiles
 */
public class MapRegion {

	private final long regionId;
	private Set<GameMapTile> tiles = new HashSet<>();

	public final TileType tileType;
	private int numBorderTiles = 0;
	public final List<MapEdgeSection> edges = new LinkedList<>();
	public final Set<CompassDirection> edgeDirections = EnumSet.noneOf(CompassDirection.class);

	private int minX = Integer.MAX_VALUE;
	private int maxX = Integer.MIN_VALUE;
	private int minY = Integer.MAX_VALUE;
	private int maxY = Integer.MIN_VALUE;

	public MapRegion(TileType tileType) {
		this.regionId = SequentialIdGenerator.nextId();
		this.tileType = tileType;
	}

	public void add(GameMapTile tile, AbstractGameMap parentMap) {
		if (!contains(tile)) {
			tile.setRegion(this);
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

			if (tile.isBorderTile()) {
				numBorderTiles++;

				if (parentMap != null) {
					if (tile.getPosition().x == 0) {
						edgeDirections.add(WEST);
						updateEdges(WEST, tile.getPosition());
					} else if (tile.getPosition().x == parentMap.getWidth() - 1) {
						edgeDirections.add(EAST);
						updateEdges(EAST, tile.getPosition());
					}

					if (tile.getPosition().y == 0) {
						edgeDirections.add(SOUTH);
						updateEdges(SOUTH, tile.getPosition());
					} else if (tile.getPosition().y == parentMap.getHeight() - 1) {
						edgeDirections.add(NORTH);
						updateEdges(NORTH, tile.getPosition());
					}
				}
			}
		}
		tiles.add(tile);
	}

	private void updateEdges(CompassDirection direction, GridPoint2 position) {
		List<MapEdgeSection> adjacentEdgesInDirection = new LinkedList<>();
		for (MapEdgeSection edge : edges) {
			if (edge.direction == direction && edge.isAdjacent(position)) {
				adjacentEdgesInDirection.add(edge);
			}
		}

		if (adjacentEdgesInDirection.size() == 0) {
			MapEdgeSection edgeSection = new MapEdgeSection(direction, position);
			edges.add(edgeSection);
		} else {
			for (MapEdgeSection adjacentEdge : adjacentEdgesInDirection) {
				adjacentEdge.addAdjacentPoint(position);
			}
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

	public TileType getTileType() {
		return tileType;
	}

	public int size() {
		return tiles.size();
	}

	public int getNumBorderTiles() {
		return numBorderTiles;
	}

	public long getRegionId() {
		return regionId;
	}

	public void remove(GameMapTile tile) {
		if (contains(tile)) {
			tiles.remove(tile);
			tile.setRegion(null);
		}
	}

	public Set<GameMapTile> getTiles() {
		return tiles;
	}

	public List<MapEdgeSection> getEdges() {
		return edges;
	}

	public Set<CompassDirection> getEdgeDirections() {
		return edgeDirections;
	}
}
