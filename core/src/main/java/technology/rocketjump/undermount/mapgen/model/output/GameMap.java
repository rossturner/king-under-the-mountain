package technology.rocketjump.undermount.mapgen.model.output;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import technology.rocketjump.undermount.mapgen.generators.SimplexNoise;
import technology.rocketjump.undermount.mapgen.model.AbstractGameMap;
import technology.rocketjump.undermount.mapgen.model.FloorType;
import technology.rocketjump.undermount.mapgen.model.RoofType;
import technology.rocketjump.undermount.mapgen.model.VertexGameMap;

import java.util.*;

public class GameMap extends AbstractGameMap {

	private Vector<Vector<GameMapTile>> xArray;

	private VertexGameMap vertexGameMap;

	private final Map<Long, MapRegion> regions = new HashMap<>();
	private final Map<Long, MapSubRegion> subRegions = new HashMap<>();

	private List<GridPoint2> riverStartTiles = new LinkedList<>();
	private List<GridPoint2> riverEndTiles = new LinkedList<>();
	private List<GridPoint2> riverTiles = null;

	public GameMap(int width, int height) {
		super(width, height);
		xArray = new Vector<>(width);
		for (int x = 0; x < width; x++) {
			Vector<GameMapTile> yArray = new Vector<>(height);
			for (int y = 0; y < height; y++) {
				yArray.add(new GameMapTile(new GridPoint2(x, y), isBorderTile(x, y, width, height)));
			}
			xArray.add(yArray);
		}
	}

	private boolean isBorderTile(int x, int y, int width, int height) {
		return x == 0 || y == 0 || x == width - 1 || y == height - 1;
	}

	public GameMapTile get(int x, int y) {
		if (x < 0 || x >= width) {
			return null;
		} else if (y < 0 || y >= height) {
			return null;
		} else {
			return xArray.get(x).get(y);
		}
	}

	public GameMapTile get(GridPoint2 position) {
		return get(position.x, position.y);
	}

	public void set(int x, int y, GameMapTile value) {
		xArray.get(x).set(y, value);
	}

	public GameMap clone() {
		GameMap cloned = new GameMap(width, height);

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				cloned.set(x, y, new GameMapTile(this.get(x, y)));
			}
		}

		cloned.setVertexGameMap(this.getVertexGameMap());

		return cloned;
	}

	public void addSimplexNoise(SimplexNoise simplexNoise) {
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				double noise = simplexNoise.getNoise(x, y);
				noise = (noise + 1) / 2; // now in range 0 to 1
				GameMapTile tile = get(x, y);
				noise = noise + tile.getHeightMapValue() / 2; // average noise and height
				tile.setNoisyHeightValue((float)noise);
			}
		}
	}

	public void randomiseCaves(Random random) {

		int largestFeature = 70;
		double persistence = 0.7;
		SimplexNoise simplexNoise = new SimplexNoise(largestFeature, persistence, random);

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				GameMapTile tile = get(x, y);
				if (tile.getRoofType() == RoofType.Underground) {
					// This is underground and a candidate to be a cave tile

					double noise = simplexNoise.getNoise(x, y);
					noise = (noise + 1) / 2; // now in range 0 to 1

					// Chance to be cave should be proportional to height
//					float chanceToBeCave = tile.getHeightMapValue() + ((random.nextFloat() - 0.45f) * 0.15f);
//					float chanceToBeCave = (tile.getHeightMapValue() / 10 * 7) + (random.nextFloat() / 10 * 3) + 0.05f;
//					float chanceToBeCave = 0.4f;
//					double chanceToBeCave = noise / 10 * 5 + (tile.getHeightMapValue() / 10 * 5);
//					if (random.nextDouble() < chanceToBeCave) {
//						tile.setAsCave();
//					}
					double averaged = (noise / 10 * 6) + (tile.getHeightMapValue() / 10 * 4);
					if (averaged > 0.6) {
						tile.setAsCave();
					}
				}

			}
		}
	}


	public void addCaves(List<Set<GridPoint2>> enclosedOutsideAreas) {
		for (Set<GridPoint2> enclosedOutsideArea : enclosedOutsideAreas) {
			for (GridPoint2 tileLocation : enclosedOutsideArea) {
				get(tileLocation.x, tileLocation.y).setAsCave();
			}
		}
	}

	/**
	 * For this method, heights are both positive (mountain) or negative (ground) and need to be normalised from -1 to 1
	 */
	public void normaliseHeights() {
		float minHeight = Float.MAX_VALUE;
		float maxHeight = Float.MIN_VALUE;

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				float heightAtPoint = get(x, y).getHeightMapValue();
				if (heightAtPoint < minHeight) {
					minHeight = heightAtPoint;
				}
				if (heightAtPoint > maxHeight) {
					maxHeight = heightAtPoint;
				}
			}
		}

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				GameMapTile tile = get(x, y);
				float heightAtPoint = tile.getHeightMapValue();
				float adjusted = (((heightAtPoint - minHeight) / (maxHeight - minHeight)) * 2) - 1;
				tile.setHeightMapValue(adjusted);
			}
		}
	}

	public int getNumWallNeighbours(int x, int y) {
		int totalWallNeighbours = 0;

		if (isWallNeighbour(x - 1, y - 1))
			totalWallNeighbours++;
		if (isWallNeighbour(x, y - 1))
			totalWallNeighbours++;
		if (isWallNeighbour(x + 1, y - 1))
			totalWallNeighbours++;

		if (isWallNeighbour(x - 1, y))
			totalWallNeighbours++;
		if (isWallNeighbour(x + 1, y))
			totalWallNeighbours++;

		if (isWallNeighbour(x - 1, y + 1))
			totalWallNeighbours++;
		if (isWallNeighbour(x, y + 1))
			totalWallNeighbours++;
		if (isWallNeighbour(x + 1, y + 1))
			totalWallNeighbours++;

		return totalWallNeighbours;
	}

	private boolean isWallNeighbour(int x, int y) {
		GameMapTile neighbour = get(x, y);
		if (neighbour == null) {
			return false;
		} else {
			return neighbour.getFloorType() == FloorType.None;
		}
	}

	public List<GameMapTile> getDiagonalNeighbours(GridPoint2 position) {
		List<GameMapTile> neighbours = new ArrayList<>(4);

		GameMapTile NE = get(position.x + 1, position.y + 1);
		if (NE != null) {
			neighbours.add(NE);
		}
		GameMapTile SE = get(position.x + 1, position.y - 1);
		if (SE != null) {
			neighbours.add(SE);
		}
		GameMapTile NW = get(position.x - 1, position.y + 1);
		if (NW != null) {
			neighbours.add(NW);
		}
		GameMapTile SW = get(position.x - 1, position.y - 1);
		if (SW != null) {
			neighbours.add(SW);
		}

		return neighbours;
	}

	public List<GameMapTile> getOrthogonalNeighbours(GridPoint2 position) {
		List<GameMapTile> neighbours = new ArrayList<>(4);

		GameMapTile up = get(position.x, position.y + 1);
		if (up != null) {
			neighbours.add(up);
		}
		GameMapTile down = get(position.x, position.y -1);
		if (down != null) {
			neighbours.add(down);
		}
		GameMapTile left = get(position.x - 1, position.y);
		if (left != null) {
			neighbours.add(left);
		}
		GameMapTile right = get(position.x + 1, position.y);
		if (right != null) {
			neighbours.add(right);
		}

		return neighbours;
	}

	public Map<Long, MapRegion> getRegions() {
		return regions;
	}

	public void addRegion(MapRegion region) {
		regions.put(region.getRegionId(), region);
	}

	public void addSubRegion(MapSubRegion subRegion) {
		subRegions.put(subRegion.getSubRegionId(), subRegion);
	}

	public Array<GameMapTile> getAllNeighbourTiles(int x, int y) {
		Array<GameMapTile> neighbours = new Array<>(8);

		GameMapTile north = get(x, y + 1);
		if (north != null) {
			neighbours.add(north);
		}
		GameMapTile northEast = get(x + 1, y + 1);
		if (northEast != null) {
			neighbours.add(northEast);
		}
		GameMapTile east = get(x + 1, y);
		if (east != null) {
			neighbours.add(east);
		}
		GameMapTile southEast = get(x + 1, y - 1);
		if (southEast != null) {
			neighbours.add(southEast);
		}
		GameMapTile south = get(x, y -1);
		if (south != null) {
			neighbours.add(south);
		}
		GameMapTile southWest = get(x - 1, y -1);
		if (southWest != null) {
			neighbours.add(southWest);
		}
		GameMapTile west = get(x - 1, y);
		if (west != null) {
			neighbours.add(west);
		}
		GameMapTile northWest = get(x - 1, y + 1);
		if (northWest != null) {
			neighbours.add(northWest);
		}

		return neighbours;
	}

	public Map<Long, MapSubRegion> getSubRegions() {
		return subRegions;
	}

	public void setRiver(List<GridPoint2> riverTiles) {
		this.riverTiles = riverTiles;
	}

	public boolean hasRiver() {
		return riverTiles != null;
	}

	public List<GridPoint2> getRiverTiles() {
		return riverTiles;
	}

	public List<GridPoint2> getRiverStartTiles() {
		return riverStartTiles;
	}

	public List<GridPoint2> getRiverEndTiles() {
		return riverEndTiles;
	}

	public VertexGameMap getVertexGameMap() {
		return vertexGameMap;
	}

	public void setVertexGameMap(VertexGameMap vertexGameMap) {
		this.vertexGameMap = vertexGameMap;
	}
}
