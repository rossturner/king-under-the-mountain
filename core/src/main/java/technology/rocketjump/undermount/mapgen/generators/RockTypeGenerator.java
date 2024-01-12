package technology.rocketjump.undermount.mapgen.generators;

import technology.rocketjump.undermount.mapgen.model.RockGroup;
import technology.rocketjump.undermount.mapgen.model.RoofType;
import technology.rocketjump.undermount.mapgen.model.input.GameMapGenerationParams;
import technology.rocketjump.undermount.mapgen.model.input.RockType;
import technology.rocketjump.undermount.mapgen.model.output.GameMap;
import technology.rocketjump.undermount.mapgen.model.output.GameMapTile;

import java.util.*;

public class RockTypeGenerator {

	public void assignRockGroups(GameMap gameMap, Random random) {
		int largestFeature = 200;
		double persistence = 0.4;
		SimplexNoise simplexNoise = new SimplexNoise(largestFeature, persistence, random);

		for (int x = 0; x < gameMap.getWidth(); x++) {
			for (int y = 0; y < gameMap.getHeight(); y++) {
				GameMapTile tile = gameMap.get(x, y);
				if (tile.getRoofType() == RoofType.Underground) {
					// This is underground and a candidate to be a cave tile

					float noise = (float)simplexNoise.getNoise(x, y);
					noise = (noise + 1) / 2; // now in range 0 to 1
//					noise = (noise / 2f) + (tile.getHeightMapValue() / 2f);

					if (noise > 0.6f) {
						tile.setRockGroup(RockGroup.Metamorphic);
					} else {
						// Modelling line of y = 0.6 - x
						// where x is noise and y is height
						float heightThreshold = 0.8f - noise;
						if (tile.getHeightMapValue() > heightThreshold) {
							tile.setRockGroup(RockGroup.Igneous);
						} else {
							tile.setRockGroup(RockGroup.Sedimentary);
						}
					}

					tile.setDebugValue(noise);
				}

			}
		}
	}

	public void assignRockTypes(GameMap gameMap, GameMapGenerationParams generationParams, Random random) {


		for (int x = 0; x < gameMap.getWidth(); x++) {
			for (int y = 0; y < gameMap.getHeight(); y++) {
				GameMapTile tile = gameMap.get(x, y);
				if (!tile.getRockGroup().equals(RockGroup.None) && tile.getRockType() == null) {
					// This tile needs a rock type applying
					RockGroup rockGroup = tile.getRockGroup();
					RockType rockTypeToApply = pickRockType(generationParams.getRockTypes(rockGroup), random);

					LinkedList<GameMapTile> frontier = new LinkedList<>();
					Set<GameMapTile> explored = new HashSet<>();
					frontier.add(tile);

					while (!frontier.isEmpty()) {
						GameMapTile currentTile = frontier.removeFirst();
						if (!explored.contains(currentTile)) {
							// if this is an applicable tile
							if (rockGroup.equals(currentTile.getRockGroup()) && currentTile.getRockType() == null) {
								currentTile.setRockType(rockTypeToApply);
								for (GameMapTile neighbour : gameMap.getOrthogonalNeighbours(currentTile.getPosition())) {
									if (!explored.contains(neighbour)) {
										frontier.add(neighbour);
									}
								}
							}
							explored.add(currentTile);
						}

					}
				}
			}
		}

	}

	private RockType pickRockType(List<RockType> rockTypes, Random random) {
		float totalWeightings = 0f;
		for (RockType rockType : rockTypes) {
			totalWeightings += rockType.getWeighting();
		}

		float weightingPicker = random.nextFloat() * totalWeightings;

		for (RockType rockType : rockTypes) {
			weightingPicker -= rockType.getWeighting();
			if (weightingPicker <= 0) {
				return rockType;
			}
		}

		// Not sure that this can happen?
		System.err.println("Logic error picking rock type in " + this.getClass().getName());
		return rockTypes.get(0);
	}
}
