package technology.rocketjump.undermount.mapgen.calculators;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.mapgen.model.output.*;

import java.util.*;

public class RegionCalculator {

	/**
	 * This method assigns every tile to either be an OUTSIDE or MOUNTAIN region
	 */
	public void assignRegions(GameMap map) {
		Set<GameMapTile> assigned = new HashSet<>();

		for (int x = 0; x < map.getWidth(); x++) {
			for (int y = 0; y < map.getHeight(); y++) {
				GameMapTile currentTile = map.get(x, y);
				if (!assigned.contains(currentTile)) {
					floodFillRegion(currentTile, map, assigned);
				}
			}
		}
	}

	public void assignRegions(Set<GameMapTile> tiles, GameMap map) {
		Set<GameMapTile> assigned = new HashSet<>();
		for (GameMapTile currentTile : tiles) {
			if (!assigned.contains(currentTile)) {
				floodFillRegion(currentTile, map, assigned);
			}
		}
	}

	/**
	 * This method assigns every tile in a main OUTSIDE or MOUNTAIN region into a specific subregion
	 * <p>
	 * Need to ensure this is called after caves have been smoothed
	 */
	public void assignSubRegions(GameMap map) {
		Set<GameMapTile> assigned = new HashSet<>();

		for (int x = 0; x < map.getWidth(); x++) {
			for (int y = 0; y < map.getHeight(); y++) {
				GameMapTile currentTile = map.get(x, y);
				if (!assigned.contains(currentTile)) {
					floodFillSubRegion(currentTile, map, assigned);
				}
			}
		}

	}

	public List<Set<GridPoint2>> setEnclosedOutsideAreasAsMountain(GameMap map) {
		Set<GameMapTile> visited = new HashSet<>();
		List<Set<GridPoint2>> enclosedOutsideAreas = new ArrayList<>();

		for (int x = 0; x < map.getWidth(); x++) {
			for (int y = 0; y < map.getHeight(); y++) {
				GameMapTile cursorTile = map.get(x, y);
				if (visited.contains(cursorTile)) {
					continue;
				} else {
					visited.add(cursorTile);
				}

				if (cursorTile.getTileType().equals(TileType.OUTSIDE)) {
					boolean edgeFound = false;
					Set<GameMapTile> region = new HashSet<>();
					LinkedList<GameMapTile> frontier = new LinkedList<>();
					frontier.add(cursorTile);

					while (!frontier.isEmpty()) {
						GameMapTile frontierTile = frontier.removeFirst();
						visited.add(frontierTile);

						if (frontierTile.getTileType().equals(TileType.OUTSIDE)) {
							region.add(frontierTile);
							if (frontierTile.isBorderTile()) {
								edgeFound = true;
							}

							for (GameMapTile neighbour : map.getOrthogonalNeighbours(frontierTile.getPosition())) {
								if (!frontier.contains(neighbour) && !visited.contains(neighbour)) {
									frontier.add(neighbour);
								}
							}
						}
					}

					if (!edgeFound) {
						Set<GridPoint2> gridpoints = new HashSet<>();
						for (GameMapTile gameMapTile : region) {
							gameMapTile.setAsMountain();
							gameMapTile.setTileSubType(TileSubType.MOUNTAIN_ROCK);
							gridpoints.add(gameMapTile.getPosition());
						}
						enclosedOutsideAreas.add(gridpoints);
					}
				}

			}
		}
		return enclosedOutsideAreas;
	}

	private void floodFillRegion(GameMapTile initialTile, GameMap map, Set<GameMapTile> assigned) {
		TileType regionTileType = initialTile.getTileType();
		MapRegion region = new MapRegion(regionTileType);
		Set<GameMapTile> ignored = new HashSet<>();

		LinkedList<GameMapTile> frontier = new LinkedList<>();
		frontier.add(initialTile);

		while (!frontier.isEmpty()) {
			GameMapTile currentTile = frontier.removeFirst();
			if (currentTile.getTileType().equals(regionTileType)) {
				// Add this to region, add neighbours to frontier

				region.add(currentTile, map);
				assigned.add(currentTile);

				for (GameMapTile neighbour : map.getOrthogonalNeighbours(currentTile.getPosition())) {
					if (!frontier.contains(neighbour) && !ignored.contains(neighbour) && !assigned.contains(neighbour)) {
						frontier.add(neighbour);
					}
				}


			} else {
				ignored.add(currentTile);
			}
		}

		map.addRegion(region);
	}

	private void floodFillSubRegion(GameMapTile initialTile, GameMap map, Set<GameMapTile> assigned) {
		TileSubType subRegionType = initialTile.getTileSubType();

		MapSubRegion subRegion = new MapSubRegion(subRegionType);

		Set<GameMapTile> ignored = new HashSet<>();

		LinkedList<GameMapTile> frontier = new LinkedList<>();
		frontier.add(initialTile);

		while (!frontier.isEmpty()) {
			GameMapTile currentTile = frontier.removeFirst();
			if (currentTile.getTileSubType().equals(subRegionType)) {
				// Add this to region, add neighbours to frontier
				subRegion.add(currentTile);
				assigned.add(currentTile);
				for (GameMapTile neighbour : map.getOrthogonalNeighbours(currentTile.getPosition())) {
					if (!frontier.contains(neighbour) && !ignored.contains(neighbour) && !assigned.contains(neighbour)) {
						frontier.add(neighbour);
					}
				}
			} else {
				ignored.add(currentTile);
			}
		}
		map.addSubRegion(subRegion);
	}
}
