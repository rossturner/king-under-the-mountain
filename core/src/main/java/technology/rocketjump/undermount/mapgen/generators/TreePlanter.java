package technology.rocketjump.undermount.mapgen.generators;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.mapgen.model.input.GameMapGenerationParams;
import technology.rocketjump.undermount.mapgen.model.input.TreeType;
import technology.rocketjump.undermount.mapgen.model.output.*;

import java.util.*;

public class TreePlanter {

	private static final int NUM_TREES_TO_SPAWN_FROM_EACH_TREE = 3;
	public static final int MAX_TREE_HEIGHT_TILES = 4;

	// Used by randomPointNear()
	private Vector<GridPoint2> outerOffsets = new Vector<>(8);
	private Vector<GridPoint2> innerOffsets = new Vector<>(9);

	public TreePlanter() {
		outerOffsets.add(new GridPoint2(0, 6)); // A
		outerOffsets.add(new GridPoint2(3, 4)); // B
		outerOffsets.add(new GridPoint2(3, 1)); // C
		outerOffsets.add(new GridPoint2(3, -2)); // D
		outerOffsets.add(new GridPoint2(0, -3)); // E
		outerOffsets.add(new GridPoint2(-3, -3)); // F
		outerOffsets.add(new GridPoint2(-3, 1)); // G
		outerOffsets.add(new GridPoint2(-3, 4)); // H

		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				innerOffsets.add(new GridPoint2(x, y));
			}
		}
	}

	public void placeTrees(GameMap map, TileSubType targetType, Random random, GameMapGenerationParams generationParams) {
		for (MapSubRegion subRegion : map.getSubRegions().values()) {
			if (subRegion.getSubRegionType().equals(targetType)) {
				List<TreeType> treesToUse = pickTreeTypesForForest(map, generationParams, subRegion, random);
				placeTrees(map, subRegion, random, treesToUse);
			}
		}
	}

	private List<TreeType> pickTreeTypesForForest(GameMap map, GameMapGenerationParams generationParams, MapSubRegion subRegion, Random random) {
		List<TreeType> treesToUse = new ArrayList<>();

		float mapHeightPosition = (float)subRegion.getMiddle().y / (float)map.getHeight();

		while (treesToUse.size() < 2) {
			TreeType treeType = generationParams.getTreeTypes().get(random.nextInt(generationParams.getTreeTypes().size()));
			if (treeType.getMinYPosition() > mapHeightPosition || treeType.getMaxYPosition() < mapHeightPosition) {
				// Discarding tree type as it does not fit in position
				continue;
			}
			if (!treesToUse.contains(treeType)) {
				treesToUse.add(treeType);
			}
		}

		return treesToUse;
	}

	private class TreeSpawnPosition {
		public final GridPoint2 position;
		public final TreeType treeType;

		public TreeSpawnPosition(GridPoint2 position, TreeType treeType) {
			this.position = position;
			this.treeType = treeType;
		}
	}

	private void placeTrees(GameMap map, MapSubRegion subRegion, Random random, List<TreeType> treesToUse) {
		LinkedList<TreeSpawnPosition> positionsToSpawnFrom = new LinkedList<>();

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
			if (isTreeAllowedAt(initialPosition, map, subRegion)) {
				TreeType treeType = treesToUse.get(random.nextInt(treesToUse.size()));
				map.get(initialPosition).setTree(treeType);
				positionsToSpawnFrom.add(new TreeSpawnPosition(initialPosition, treeType));
			}
		}

		while (!positionsToSpawnFrom.isEmpty()) {
			TreeSpawnPosition positionToSpawnFrom = positionsToSpawnFrom.removeFirst();

			int treesSpawned = 0;
			while (treesSpawned < NUM_TREES_TO_SPAWN_FROM_EACH_TREE) {
				GridPoint2 validNearbyPoint = null;
				for (int i = 0; i < 16; i++) {
					validNearbyPoint = randomPointNear(positionToSpawnFrom.position, random);
					if (isTreeAllowedAt(validNearbyPoint, map, subRegion)) {
						break;
					} else {
						validNearbyPoint = null;
					}
				}
				// If validNearbyPoint still null, couldn't find a place to spawn a tree
				if (validNearbyPoint == null) {
					break;
				} else {
					map.get(validNearbyPoint).setTree(positionToSpawnFrom.treeType);
					positionsToSpawnFrom.add(new TreeSpawnPosition(validNearbyPoint, positionToSpawnFrom.treeType));
					treesSpawned++;
				}
			}
		}

	}

	/**
	 * This method returns a gridpoint somewhat evenly distributed around where a tree at origin would allow a neighbour
	 *
	 * See this diagram
	 *
	 * 	   aaa
	 * 	   aaa
	 * 	hhhaaabbb
	 * 	hhh...bbb
	 * 	hhh.|.bbb
	 * 	ggg.|.ccc
	 * 	ggg.|.ccc
	 * 	ggg.O.ccc
	 * 	fff...ddd
	 * 	fffeeeddd
	 * 	fffeeeddd
	 * 	   eee
	 *
	 * Where a - h are a 3x3 block of tiles to pick between, with one of the 9 tiles within them then chosen randomly
	 * Where . is a space around the tree denoted by | growing from the origin O
	 */
	public GridPoint2 randomPointNear(GridPoint2 origin, Random random) {
		return origin.cpy()
				.add(outerOffsets.get(random.nextInt(outerOffsets.size())))
				.add(innerOffsets.get(random.nextInt(innerOffsets.size())));
	}

	/**
	 * This method checks if the given position would be too close to another tree already in the map
	 */
	public boolean isTreeAllowedAt(GridPoint2 position, GameMap map, MapSubRegion subRegionToMatch) {
		GameMapTile tileAtPosition = map.get(position);
		if (tileAtPosition == null) {
			return false;
		}

		if (tileAtPosition.getSubRegion().getSubRegionId() != subRegionToMatch.getSubRegionId()) {
			// Keep to same sub-region for now
			return false;
		}

		// First check no trees nearby
		for (int x = position.x - 1; x <= position.x + 1; x++) {
			for (int y = position.y - MAX_TREE_HEIGHT_TILES; y <= position.y + MAX_TREE_HEIGHT_TILES; y++) {
				GameMapTile tile = map.get(x, y);
				if (tile == null) {
					continue; // If bottom of map is below tree, don't care too much
				}
				if (tile.hasTree() || tile.hasRiver()) {
					return false;
				}
			}
		}

		// Then check tree has room to grow into
		for (int x = position.x - 1; x <= position.x + 1; x++) {
			for (int y = position.y - 1; y <= position.y + MAX_TREE_HEIGHT_TILES; y++) {
				GameMapTile tile = map.get(x, y);
				if (tile == null || !tile.getTileType().equals(TileType.OUTSIDE)) {
					return false;
				}
			}
		}

		// Otherwise okay
		return true;
	}

}
