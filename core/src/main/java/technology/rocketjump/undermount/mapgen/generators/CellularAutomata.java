package technology.rocketjump.undermount.mapgen.generators;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import technology.rocketjump.undermount.mapgen.model.FloorType;
import technology.rocketjump.undermount.mapgen.model.RockGroup;
import technology.rocketjump.undermount.mapgen.model.output.GameMap;
import technology.rocketjump.undermount.mapgen.model.output.GameMapTile;
import technology.rocketjump.undermount.mapgen.model.output.TileSubType;
import technology.rocketjump.undermount.mapgen.model.output.TileType;

import java.util.*;

public class CellularAutomata {

	private final int deathLimit = 4;
	private final int birthLimit = 4;

	public void growRiver(GameMap map) {
		List<GridPoint2> riverTiles = map.getRiverTiles();
		if (riverTiles == null) {
			return;
		}

		Set<GameMapTile> riverNeighbourTiles = new HashSet<>();
		for (GridPoint2 riverTile : riverTiles) {
			for (GameMapTile neighbour : map.getOrthogonalNeighbours(riverTile)) {
				if (!neighbour.hasRiver() && neighbour.getTileType().equals(TileType.OUTSIDE)) {
					riverNeighbourTiles.add(neighbour);
				}
			}
		}

		for (GameMapTile riverNeighbourTile : riverNeighbourTiles) {

			long numOrthogonalRiverTiles = 0;

			for (GameMapTile neighbourTile : map.getOrthogonalNeighbours(riverNeighbourTile.getPosition())) {
				if (neighbourTile.hasRiver()) {
					numOrthogonalRiverTiles++;
				}
			}

			if (numOrthogonalRiverTiles >= 2) {
				riverNeighbourTile.setRiver(true);
				riverTiles.add(riverNeighbourTile.getPosition());
			} else if (numOrthogonalRiverTiles == 1) {
				// If there is one orthogonal neighbour river tile and there are also 2 diagonal river tile neighbours, this can be a river too
				long numDiagonalRiverTiles = 0;
				for (GameMapTile neighbour : map.getDiagonalNeighbours(riverNeighbourTile.getPosition())) {
					if (neighbour.hasRiver()) {
						numDiagonalRiverTiles++;
					}
				}
				if (numDiagonalRiverTiles >= 2) {
					riverNeighbourTile.setRiver(true);
					riverTiles.add(riverNeighbourTile.getPosition());
				}
			}
		}

	}

	public GameMap smoothCaves(GameMap input) {
		GameMap output = input.clone();

		for (int x = 0; x < input.getWidth(); x++) {
			for (int y = 0; y < input.getHeight(); y++) {
				GameMapTile tile = input.get(x, y);

				// This is either a wall or cave floor, eligible for modification
				if (tile.getFloorType() != FloorType.Outdoor) {
					int wallNeighbours = 8 - input.getNumWallNeighbours(x, y);
					if (tile.getFloorType() != FloorType.None) {
						// This is a cave, so it is "alive"
						if (wallNeighbours < deathLimit){
							output.get(x, y).setAsMountain();
						} else{
							output.get(x, y).setAsCave();
						}
					} else {
						// This is a wall, so it is "dead"
						if(wallNeighbours > birthLimit){
							output.get(x, y).setAsCave();
						} else{
							output.get(x, y).setAsMountain();
						}
					}
				}

			}
		}

		return output;

	}

	public GameMap smoothWalls(GameMap input) {
		GameMap output = input.clone();

		for (int x = 0; x < input.getWidth(); x++) {
			for (int y = 0; y < input.getHeight(); y++) {
				int wallNeighbours = input.getNumWallNeighbours(x, y);
				GameMapTile tile = input.get(x, y);

				if (tile.getFloorType() == FloorType.None) {
					// This is a wall, so it is "alive"
					if(wallNeighbours < deathLimit){
						output.get(x, y).setAsOutside();
					} else{
						output.get(x, y).setAsMountain();
					}
				} else {
					// This is not a wall, so it is "dead"
					if(wallNeighbours > birthLimit){
						output.get(x, y).setAsMountain();
					} else{
						output.get(x, y).setAsOutside();
					}
				}
			}
		}

		// Above algorithm gives artifacts on corners, so sort these out
		fixCorner(output, 0, 0, 1, 1);
		fixCorner(output, 0, output.getHeight() - 1, 1, -1);
		fixCorner(output, output.getWidth() - 1, 0, -1, 1);
		fixCorner(output, output.getWidth() - 1, output.getHeight() - 1, -1, -1);

		return output;
	}

	public GameMap smoothRockTypes(GameMap input) {
		GameMap output = input.clone();

		for (int x = 0; x < input.getWidth(); x++) {
			for (int y = 0; y < input.getHeight(); y++) {
				GameMapTile currentTile = input.get(x, y);
				if (currentTile.getRockGroup().equals(RockGroup.None)) {
					continue;
				}
				Array<GameMapTile> neighbours = input.getAllNeighbourTiles(x, y);
				neighbours.add(currentTile);

				Map<RockGroup, Integer> typeCounts = new EnumMap<>(RockGroup.class);

				for (GameMapTile neighbour : neighbours) {
					if (neighbour.getRockGroup().equals(RockGroup.None)) {
						continue;
					}
					Integer currentCount = typeCounts.get(neighbour.getRockGroup());
					if (currentCount == null) {
						currentCount = 0;
					}
					currentCount++;
					typeCounts.put(neighbour.getRockGroup(), currentCount);
				}

				int maxCount = 0;
				for (Integer count : typeCounts.values()) {
					if (count > maxCount) {
						maxCount = count;
					}
				}

				RockGroup typeOfMostNeighbours = null;
				for (Map.Entry<RockGroup, Integer> entry : typeCounts.entrySet()) {
					if (entry.getValue() == maxCount) {
						typeOfMostNeighbours = entry.getKey();
						break;
					}
				}

				if (typeOfMostNeighbours != null && !typeOfMostNeighbours.equals(currentTile.getRockGroup())) {
					// Need to change type in output
					output.get(x, y).setRockGroup(typeOfMostNeighbours);
				}

			}
		}

		return output;
	}

	public GameMap smoothOutdoorSubregions(GameMap input) {
		GameMap output = input.clone();

		for (int x = 0; x < input.getWidth(); x++) {
			for (int y = 0; y < input.getHeight(); y++) {
				GameMapTile currentTile = input.get(x, y);
				if (!currentTile.getTileType().equals(TileType.OUTSIDE)) {
					continue;
				}
				Array<GameMapTile> neighbours = input.getAllNeighbourTiles(x, y);

				Map<TileSubType, Integer> typeCounts = new EnumMap<>(TileSubType.class);

				for (GameMapTile neighbour : neighbours) {
					if (!neighbour.getTileType().equals(TileType.OUTSIDE)) {
						continue;
					}
					Integer currentCount = typeCounts.get(neighbour.getTileSubType());
					if (currentCount == null) {
						currentCount = 0;
					}
					currentCount++;
					typeCounts.put(neighbour.getTileSubType(), currentCount);
				}
				// TODO Should also count self?

				int maxCount = 0;
				for (Integer count : typeCounts.values()) {
					if (count > maxCount) {
						maxCount = count;
					}
				}

				TileSubType typeOfMostNeighbours = null;
				for (Map.Entry<TileSubType, Integer> subRegionTypeIntegerEntry : typeCounts.entrySet()) {
					if (subRegionTypeIntegerEntry.getValue() == maxCount) {
						typeOfMostNeighbours = subRegionTypeIntegerEntry.getKey();
						break;
					}
				}

				if (typeOfMostNeighbours != null && !typeOfMostNeighbours.equals(currentTile.getTileSubType())) {
					// Need to change type in output
					output.get(x, y).setTileSubType(typeOfMostNeighbours);
				}

			}
		}
		return output;
	}

	private void fixCorner(GameMap output, int targetX, int targetY, int offsetX, int offsetY) {
		GameMapTile cornerTile = output.get(targetX, targetY);
		if (cornerTile.getTileType().equals(TileType.OUTSIDE)) {
			if (output.get(targetX, targetY + offsetY).getTileType().equals(TileType.MOUNTAIN) &&
					output.get(targetX + offsetX, targetY).getTileType().equals(TileType.MOUNTAIN)) {
				cornerTile.setAsMountain();
			}
		} else {
			if (output.get(targetX, targetY + offsetY).getTileType().equals(TileType.OUTSIDE) &&
					output.get(targetX + offsetX, targetY).getTileType().equals(TileType.OUTSIDE)) {
				cornerTile.setAsOutside();
			}
		}
	}
}
