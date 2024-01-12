package technology.rocketjump.undermount.mapgen.generators;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.mapgen.calculators.RegionCalculator;
import technology.rocketjump.undermount.mapgen.calculators.RiverPathfindingCalculator;
import technology.rocketjump.undermount.mapgen.model.output.*;

import java.util.*;

public class RiverGenerator {

	/**
	 * Marks tiles in the outdoor region covering the most edges
	 *
	 * @param map
	 * @param random
	 * @return
	 */
	public boolean addRiver(GameMap map, Random random) {
		// First find an outdoor region which borders 2 edges, preferably more
		MapRegion regionToUse = pickRegionToUse(map);
		if (regionToUse == null || regionToUse.edgeDirections.size() <= 1) {
			// There are no regions or region only has one border edge, so the river won't really work
			return false;
		}

		// Pick start edge and end edge on different sides
		MapEdgeSection startEdge, endEdge;
		List<CompassDirection> edgeDirectionList = new ArrayList<>();
		edgeDirectionList.addAll(regionToUse.edgeDirections);
		if (edgeDirectionList.size() == 2) {
			// There are 2 prospective edges so use them
			do {
				startEdge = regionToUse.edges.get(random.nextInt(regionToUse.edges.size()));
				endEdge = regionToUse.edges.get(random.nextInt(regionToUse.edges.size()));
			} while (startEdge.direction == endEdge.direction);

		} else {
			// There are 3 or 4 prospective edge directions, so pick 2 which are opposite
			do {
				startEdge = regionToUse.edges.get(random.nextInt(regionToUse.edges.size()));
				endEdge = regionToUse.edges.get(random.nextInt(regionToUse.edges.size()));
			} while (!startEdge.direction.isOppositeTo(endEdge.direction));
		}

		// Adjust start and end edges such that the outermost 10% tiles are ignored
		startEdge = trimEdge(startEdge);
		endEdge = trimEdge(endEdge);

		GridPoint2 startPoint = new GridPoint2(
				startEdge.minimum.x + random.nextInt(startEdge.maximum.x - startEdge.minimum.x + 1),
				startEdge.minimum.y + random.nextInt(startEdge.maximum.y - startEdge.minimum.y + 1));
		GridPoint2 endPoint = new GridPoint2(
				endEdge.minimum.x + random.nextInt(endEdge.maximum.x - endEdge.minimum.x + 1),
				endEdge.minimum.y + random.nextInt(endEdge.maximum.y - endEdge.minimum.y + 1));

		return runRiver(startPoint, endPoint, map);
	}

	private boolean runRiver(GridPoint2 startPoint, GridPoint2 endPoint, GameMap map) {
		RiverPathfindingCalculator pathfinder = new RiverPathfindingCalculator(startPoint, endPoint, map);
		List<GridPoint2> path = pathfinder.findPath();

		if (path.size() == 0) {
			// Could not find a path - TODO figure out why
			System.err.println("Could not find a suitable path for river in " + this.getClass().getName());
			return false;
		}

		List<GridPoint2> riverTiles = new ArrayList<>();
		for (int cursor = 0; cursor < path.size(); cursor++) {
			GridPoint2 riverPoint = path.get(cursor);
			GameMapTile tile = map.get(riverPoint);
			tile.setRiver(true);
			riverTiles.add(riverPoint);
		}
		map.setRiver(riverTiles);
		map.getRiverStartTiles().add(path.get(0));
		map.getRiverEndTiles().add(path.get(path.size() - 1));
		return true;
	}

	private MapEdgeSection trimEdge(MapEdgeSection original) {
		MapEdgeSection result = new MapEdgeSection(original);
		result.minimum.x = original.minimum.x + ((original.maximum.x - original.minimum.x) * 10 / 100);
		result.maximum.x = original.maximum.x - ((original.maximum.x - original.minimum.x) * 10 / 100);
		result.minimum.y = original.minimum.y + ((original.maximum.y - original.minimum.y) * 10 / 100);
		result.maximum.y = original.maximum.y - ((original.maximum.y - original.minimum.y) * 10 / 100);
		return result;
	}

	private MapRegion pickRegionToUse(GameMap map) {

		Set<MapRegion> sortedOutdoorRegions = new TreeSet<>(new Comparator<MapRegion>() {
			@Override
			public int compare(MapRegion r1, MapRegion r2) {
				if (r1.edgeDirections.size() == r2.edgeDirections.size()) {
					return r2.getNumBorderTiles() - r1.getNumBorderTiles();
				} else {
					return r2.edgeDirections.size() - r1.edgeDirections.size();
				}
			}
		});


		for (MapRegion region : map.getRegions().values()) {
			if (region.getTileType().equals(TileType.OUTSIDE) && region.edgeDirections.size() > 0) {
				sortedOutdoorRegions.add(region);
			}
		}

		return sortedOutdoorRegions.iterator().next();
	}

	public void ensureRiverEndpoints(GameMap gameMap, int radius) {
		if (gameMap.getRiverStartTiles().isEmpty()) {
			return;
		}
		GridPoint2 startTilePosition = gameMap.getRiverStartTiles().get(0);
		for (int offsetX = -radius; offsetX <= radius; offsetX++) {
			for (int offsetY = -radius; offsetY <= radius; offsetY++) {
				GameMapTile gameMapTile = gameMap.get(startTilePosition.x + offsetX, startTilePosition.y + offsetY);
				if (gameMapTile != null && gameMapTile.hasRiver() && gameMapTile.isBorderTile()) {
					gameMap.getRiverStartTiles().add(gameMapTile.getPosition());
				}
			}
		}
		startTilePosition = gameMap.getRiverEndTiles().get(0);
		for (int offsetX = -radius; offsetX <= radius; offsetX++) {
			for (int offsetY = -radius; offsetY <= radius; offsetY++) {
				GameMapTile gameMapTile = gameMap.get(startTilePosition.x + offsetX, startTilePosition.y + offsetY);
				if (gameMapTile != null && gameMapTile.hasRiver() && gameMapTile.isBorderTile()) {
					gameMap.getRiverEndTiles().add(gameMapTile.getPosition());
				}
			}
		}
	}

	public void replaceRiverRegion(GameMap gameMap) {
		List<GridPoint2> riverStartTiles = gameMap.getRiverStartTiles();
		if (riverStartTiles == null || riverStartTiles.isEmpty()) {
			return;
		}

		MapRegion regionToReplace = gameMap.get(riverStartTiles.get(0)).getRegion();
		for (GameMapTile gameMapTile : regionToReplace.getTiles()) {
			gameMapTile.setRegion(null);
		}
		new RegionCalculator().assignRegions(regionToReplace.getTiles(), gameMap);

		gameMap.getRegions().remove(regionToReplace.getRegionId());
	}
}
