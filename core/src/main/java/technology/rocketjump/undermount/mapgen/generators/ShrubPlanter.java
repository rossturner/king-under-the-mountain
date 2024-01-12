package technology.rocketjump.undermount.mapgen.generators;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import technology.rocketjump.undermount.mapgen.model.input.GameMapGenerationParams;
import technology.rocketjump.undermount.mapgen.model.input.ShrubType;
import technology.rocketjump.undermount.mapgen.model.output.GameMap;
import technology.rocketjump.undermount.mapgen.model.output.GameMapTile;
import technology.rocketjump.undermount.mapgen.model.output.MapSubRegion;
import technology.rocketjump.undermount.mapgen.model.output.TileSubType;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ShrubPlanter {

	private static final int NUM_SHRUBS_TO_SPAWN_FROM_EACH_TREE = 3;
	public static final int MAX_SHRUB_NEIGHBOURS_ALLOWED = 2;

	// Used by randomPointNear()
	private Array<GridPoint2> outerOffsets = new Array<>(8);
	private Array<GridPoint2> innerOffsets = new Array<>(9);

	public ShrubPlanter() {
		outerOffsets.add(new GridPoint2(-2, 0)); // W
		outerOffsets.add(new GridPoint2(-2, 2)); // NW
		outerOffsets.add(new GridPoint2(0, 2)); // N
		outerOffsets.add(new GridPoint2(2, 2)); // NE
		outerOffsets.add(new GridPoint2(2, 0)); // E
		outerOffsets.add(new GridPoint2(2, -2)); // SE
		outerOffsets.add(new GridPoint2(0, -2)); // S
		outerOffsets.add(new GridPoint2(-2, -2)); // SW

		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				innerOffsets.add(new GridPoint2(x, y));
			}
		}
	}

	public void placeShrubs(GameMap map, TileSubType targetType, Random random, GameMapGenerationParams generationParams) {
		for (MapSubRegion subRegion : map.getSubRegions().values()) {
			if (subRegion.getSubRegionType().equals(targetType)) {
				ShrubType nonFruitShrub = pickShrub(generationParams, random, false);
				ShrubType fruitShrub = pickShrub(generationParams, random, true);

				placeShrubs(map, subRegion, random, nonFruitShrub, fruitShrub, generationParams.getRatioOfFruitingShrubs());
			}
		}
	}

	private ShrubType pickShrub(GameMapGenerationParams generationParams, Random random, boolean hasFruit) {
		List<ShrubType> shrubTypes = generationParams.getShrubTypes();

		ShrubType picked = null;
		while (picked == null) {
			// FIXME Need to ensure at least one shrub type with both fruit and non-fruit
			ShrubType aShrubType = shrubTypes.get(random.nextInt(shrubTypes.size()));
			if (aShrubType.hasFruit() == hasFruit) {
				picked = aShrubType;
			}
		}

		return picked;
	}

	private void placeShrubs(GameMap map, MapSubRegion subRegion, Random random, ShrubType nonFruitShrub, ShrubType fruitShrub, float ratioOfFruitingShrubs) {
		LinkedList<GridPoint2> positionsToSpawnFrom = new LinkedList<>();

		List<GridPoint2> initialPositions = new LinkedList<>();

		int minX = subRegion.getMinX();
		int maxX = subRegion.getMaxX();
		int width = maxX - minX;
		int minY = subRegion.getMinY();
		int maxY = subRegion.getMaxY();
		int height = maxY - minY;

		// This loop sets 9 points within the region, each 1/4 of the width/height apart
		for (int quarterX = 1; quarterX <= 3; quarterX++) {
			for (int quarterY = 1; quarterY <= 3; quarterY++) {
				initialPositions.add(new GridPoint2(
						minX + ((width / 4) * quarterX),
						minY + ((height / 4) * quarterY)));
			}
		}

		for (GridPoint2 initialPosition : initialPositions) {
			if (isShrubAllowedAt(initialPosition, map, subRegion)) {
				ShrubType typeToUse = pickShrubType(random, nonFruitShrub, fruitShrub, ratioOfFruitingShrubs);
				map.get(initialPosition).setShrubType(typeToUse);
				positionsToSpawnFrom.add(initialPosition);
			}
		}

		while (!positionsToSpawnFrom.isEmpty()) {
			GridPoint2 positionToSpawnFrom = positionsToSpawnFrom.removeFirst();

			int shrubsSpawned = 0;
			while (shrubsSpawned < NUM_SHRUBS_TO_SPAWN_FROM_EACH_TREE) {
				GridPoint2 validNearbyPoint = null;
				for (int i = 0; i < 6; i++) {
					validNearbyPoint = randomPointNear(positionToSpawnFrom, random);
					if (isShrubAllowedAt(validNearbyPoint, map, subRegion)) {
						break;
					} else {
						validNearbyPoint = null;
					}
				}
				// If validNearbyPoint still null, couldn't find a place to spawn a tree
				if (validNearbyPoint == null) {
					break;
				} else {
					map.get(validNearbyPoint).setShrubType(pickShrubType(random, nonFruitShrub, fruitShrub, ratioOfFruitingShrubs));
					positionsToSpawnFrom.add(validNearbyPoint);
					shrubsSpawned++;
				}
			}
		}

	}

	private ShrubType pickShrubType(Random random, ShrubType nonFruitShrub, ShrubType fruitShrub, float ratioOfFruitingShrubs) {
		boolean hasFruit = random.nextFloat() < ratioOfFruitingShrubs;
		return hasFruit ? fruitShrub : nonFruitShrub;
	}

	/**
	 * This method returns a gridpoint near X
	 *
	 * See this diagram
	 *
	 * 1121211
	 * 1121211
	 * 2231322
	 * 111X111
	 * 2232322
	 * 1121211
	 * 1121211
	 *
	 */
	public GridPoint2 randomPointNear(GridPoint2 origin, Random random) {
		return origin.cpy()
				.add(outerOffsets.get(random.nextInt(outerOffsets.size)))
				.add(innerOffsets.get(random.nextInt(innerOffsets.size)));
	}

	/**
	 * Shrubs are allowed as long as there are at most 3 other shrubs or trees nearby
	 */
	public boolean isShrubAllowedAt(GridPoint2 position, GameMap map, MapSubRegion subRegionToMatch) {
		GameMapTile tileAtPosition = map.get(position);
		if (tileAtPosition == null || tileAtPosition.hasTree() || tileAtPosition.hasShrub() || tileAtPosition.hasRiver()) {
			return false;
		}

		if (tileAtPosition.getSubRegion().getSubRegionId() != subRegionToMatch.getSubRegionId()) {
			// Keep to same sub-region for now
			return false;
		}

		int numShrubNeighbours = 0;
		for (int x = position.x - 1; x <= position.x + 1; x++) {
			for (int y = position.y - 1; y <= position.y + 1; y++) {
				GameMapTile tile = map.get(x, y);
				if (tile == null) {
					continue; // If bottom of map is below tree, don't care too much
				}
				if (tile.hasTree() || tile.hasShrub()) {
					numShrubNeighbours++;
				}
			}
		}

		return numShrubNeighbours <= MAX_SHRUB_NEIGHBOURS_ALLOWED;
	}

}
