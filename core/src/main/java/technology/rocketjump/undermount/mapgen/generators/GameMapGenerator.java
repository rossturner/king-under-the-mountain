package technology.rocketjump.undermount.mapgen.generators;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.mapgen.calculators.RegionCalculator;
import technology.rocketjump.undermount.mapgen.model.AbstractGameMap;
import technology.rocketjump.undermount.mapgen.model.BinaryGrid;
import technology.rocketjump.undermount.mapgen.model.HeightGameMap;
import technology.rocketjump.undermount.mapgen.model.VertexGameMap;
import technology.rocketjump.undermount.mapgen.model.input.GameMapGenerationParams;
import technology.rocketjump.undermount.mapgen.model.input.GemType;
import technology.rocketjump.undermount.mapgen.model.input.OreType;
import technology.rocketjump.undermount.mapgen.model.output.*;

import java.util.*;

import static technology.rocketjump.undermount.mapgen.generators.MidpointDisplacement.vary;

public class GameMapGenerator {

	private final MidpointDisplacement midpointDisplacement = new MidpointDisplacement();
	private final CellularAutomata cellularAutomata = new CellularAutomata();
	private final OreVeinGenerator oreVeinGenerator = new OreVeinGenerator();
	private final RegionCalculator regionCalculator = new RegionCalculator();
	private final TreePlanter treePlanter = new TreePlanter();
	private final ShrubPlanter shrubPlanter = new ShrubPlanter();
	private final MushroomSpawner mushroomSpawner;
	private final RiverGenerator riverGenerator = new RiverGenerator();
	private final RockTypeGenerator rockTypeGenerator = new RockTypeGenerator();

	private final Random random;
	private final GameMapGenerationParams generationParams;
	private boolean finished = false;

	private VertexGameMap vertexMap;
	private HeightGameMap heightMap;
	private float heightmapVariance;
	private GameMap gameMap;

	private int gameMapIterations;
	private float desiredHeightForMountains = -1f; // Used to record mountain range depth
	private List<Set<GridPoint2>> enclosedOutsideAreas;

	public GameMapGenerator(GameMapGenerationParams generationParams, Random random) {
		this.random = random;
		this.generationParams = generationParams;
		this.mushroomSpawner = new MushroomSpawner(generationParams.getMushroomTypes());

		vertexMap = new VertexGameMap(1, 1);
		heightmapVariance = generationParams.getHeightMapVariance();
		vertexMap.set(0, 0, vary(0f, heightmapVariance, random));
		vertexMap.set(0, 1, vary(0f, heightmapVariance, random));
		vertexMap.set(1, 0, vary(0f, heightmapVariance, random));
		vertexMap.set(1, 1, vary(0f, heightmapVariance, random));
	}

	public GameMap completeGeneration() {
		while (!finished) {
			processNextStep();
		}
		return gameMap;
	}

	public void processNextStep() {

		if (gameMap != null) {
			// Currently on a game map
			processGameMapStep();
		} else if (heightMap != null) {
			// We have a height map!
			processHeightMapStep();
		} else {
			// Still on a vertex map
			processVertexMapStep();
		}

	}

	private void processGameMapStep() {
		if (gameMapIterations == 0) {
			enclosedOutsideAreas = regionCalculator.setEnclosedOutsideAreasAsMountain(gameMap);
		} else if (gameMapIterations < 6) {
			gameMap = cellularAutomata.smoothWalls(gameMap);
		} else if (gameMapIterations == 6) {
			gameMap.addCaves(enclosedOutsideAreas);
		} else if (gameMapIterations == 7) {
			gameMap.randomiseCaves(random);
		} else if (gameMapIterations < 13) {
			gameMap = cellularAutomata.smoothCaves(gameMap);
		} else if (gameMapIterations < 17) {
			gameMap = cellularAutomata.smoothOutdoorSubregions(gameMap);
		} else if (gameMapIterations == 17) {
			rockTypeGenerator.assignRockGroups(gameMap, random);
		} else if (gameMapIterations <= 21) {
			gameMap = cellularAutomata.smoothRockTypes(gameMap);
		} else if (gameMapIterations == 22) {
			regionCalculator.assignRegions(gameMap);
		} else if (gameMapIterations == 23) {
			regionCalculator.assignSubRegions(gameMap);
		} else if (gameMapIterations == 24) {
			rockTypeGenerator.assignRockTypes(gameMap, generationParams, random);
		} else if (gameMapIterations == 25) {
			riverGenerator.addRiver(gameMap, random);
		} else if (gameMapIterations <= 27) {
			cellularAutomata.growRiver(gameMap);
		} else if (gameMapIterations == 28) {
			riverGenerator.ensureRiverEndpoints(gameMap, 5);
			riverGenerator.replaceRiverRegion(gameMap);
			treePlanter.placeTrees(gameMap, TileSubType.FOREST, random, generationParams);
		} else if (gameMapIterations == 29) {
			shrubPlanter.placeShrubs(gameMap, TileSubType.GRASSLAND, random, generationParams);
		} else if (gameMapIterations == 30) {
			mushroomSpawner.placeMushrooms(gameMap, random, generationParams);
		} else if (gameMapIterations == 31) {
			initialiseOreGeneration();
			generateOre();
			generateOre();
			generateOre();
		} else if (amountOreGenerated < amountOreRequired) {
			generateOre();
			generateOre();
			generateOre();
		} else if (amountGemsGenerated < amountGemsRequired) {
			generateGems();
			generateGems();
			generateGems();
		} else {
			finished = true;
		}
		gameMapIterations++;
	}

	private void processHeightMapStep() {
		if (heightMap.getWidth() > generationParams.getTargetWidth() || heightMap.getHeight() > generationParams.getTargetHeight()) {
			int extraWidth = heightMap.getWidth() - generationParams.getTargetWidth();
			int extraHeight = heightMap.getHeight() - generationParams.getTargetHeight();

			int offsetX = extraWidth / 2;
			int offsetY = extraHeight / 2;

			heightMap = heightMap.crop(offsetX, offsetY, generationParams.getTargetWidth(), generationParams.getTargetHeight());
			vertexMap = vertexMap.crop(offsetX, offsetY, generationParams.getTargetWidth(), generationParams.getTargetHeight());

		} else if (desiredHeightForMountains < 0f) {
			// Find height such that 25% is above the line
			desiredHeightForMountains = heightMap.heightForRatioAbove(generationParams.getRatioOfMountains());
		} else {
			gameMap = heightMap.toGameMap(desiredHeightForMountains);
			int largestFeature = Math.min(generationParams.getTargetWidth(), generationParams.getTargetHeight());
			SimplexNoise simplexNoise = new SimplexNoise(largestFeature, 0.6, random);
			gameMap.addSimplexNoise(simplexNoise);
			gameMap.setVertexGameMap(vertexMap);
		}
	}

	private void processVertexMapStep() {
		if (vertexMap.getNumTilesWide() <= generationParams.getTargetWidth() || vertexMap.getNumTilesHigh() <= generationParams.getTargetHeight()) {
			vertexMap = midpointDisplacement.doubleSize(vertexMap);
			heightmapVariance = heightmapVariance * generationParams.getHeightMapRoughness();
			midpointDisplacement.applyDiamondSquareToPredoubled(vertexMap, heightmapVariance, random);
			vertexMap.normalise();
		} else {
			// Convert to heightmap
			heightMap = vertexMap.toHeightMap();
		}
	}

	private int totalMapSize;

	private int amountOreRequired;

	private int amountOreGenerated;
	private int amountGemsRequired;
	private int amountGemsGenerated;
	private List<MapRegion> largeMountainRegions;
	private int totalMountainTiles;
	private void initialiseOreGeneration() {
		totalMapSize = generationParams.getTargetWidth() * generationParams.getTargetHeight();
		long minUsefulRegionSize = Math.round(0.001 * (double) totalMapSize);


		largeMountainRegions = new ArrayList<>();
		totalMountainTiles = 0;

		for (MapRegion region : gameMap.getRegions().values()) {
			if (region.getTileType().equals(TileType.MOUNTAIN) && region.size() > minUsefulRegionSize) {
				largeMountainRegions.add(region);
				totalMountainTiles += region.size();
			}
		}

		amountOreRequired = (int)Math.ceil(totalMountainTiles * generationParams.getRequiredTotalOreRatio());
		amountOreGenerated = 0;

		amountGemsRequired = (int)Math.ceil(totalMountainTiles * generationParams.getRequiredTotalGemRatio());
		amountGemsGenerated = 0;
	}

	private void generateGems() {
		int requestedVeinLength = 2 + random.nextInt(3);
		float veinThickness = 1f + (random.nextFloat() * 2);
		float veinWidthVariance = 0.3f;

		BinaryGrid veinGrid = oreVeinGenerator.generate(requestedVeinLength, veinThickness, veinWidthVariance, random);

		MapRegion regionToUse = pickRegion(largeMountainRegions, totalMountainTiles);
		GridPoint2 randomPointInRegion = randomPointIn(regionToUse);

		GridPoint2 startPoint = randomPointInRegion.cpy().sub(
				veinGrid.getWidth() / 2,
				veinGrid.getHeight() / 2
		);

		GemType getToUse = pickGemToGenerate(gameMap.get(randomPointInRegion));

		for (int x = 0; x < veinGrid.getWidth(); x++) {
			for (int y = 0; y < veinGrid.getHeight(); y++) {
				boolean gemRequired = veinGrid.get(x, y);
				if (gemRequired) {
					GridPoint2 mapPoint = startPoint.cpy().add(x, y);
					GameMapTile mapTile = gameMap.get(mapPoint.x, mapPoint.y);
					if (mapTile != null && mapTile.getTileType().equals(TileType.MOUNTAIN)) {
						mapTile.setGem(getToUse, random);
						amountGemsGenerated++;
					}
				}
			}
		}

	}

	private void generateOre() {
		int requestedVeinLength = random.nextInt(generationParams.getMaxOreVeinLength() - generationParams.getMinOreVeinLength()) + generationParams.getMinOreVeinLength();
		float veinThickness = random.nextFloat() * (generationParams.getMaxOreVeinLength() - generationParams.getMinOreVeinLength()) / 4;
		veinThickness = Math.max(veinThickness, requestedVeinLength / 5f);
		float veinWidthVariance = veinThickness * random.nextFloat() / 2;
		veinWidthVariance = veinWidthVariance + (((random.nextFloat() * veinWidthVariance) * 2) - 1);
		veinWidthVariance = Math.max(veinWidthVariance, 1f);

		BinaryGrid oreGrid = oreVeinGenerator.generate(requestedVeinLength, veinThickness, veinWidthVariance, random);

		MapRegion regionToUse = pickRegion(largeMountainRegions, totalMountainTiles);
		GridPoint2 randomPointInRegion = randomPointIn(regionToUse);

		GridPoint2 startPoint = randomPointInRegion.cpy().sub(
				oreGrid.getWidth() / 2,
				oreGrid.getHeight() / 2
		);

		OreType oreToUse = pickOreToGenerate(gameMap.get(randomPointInRegion));

		for (int x = 0; x < oreGrid.getWidth(); x++) {
			for (int y = 0; y < oreGrid.getHeight(); y++) {
				boolean oreRequired = oreGrid.get(x, y);
				if (oreRequired) {
					GridPoint2 mapPoint = startPoint.cpy().add(x, y);
					GameMapTile mapTile = gameMap.get(mapPoint.x, mapPoint.y);
					if (mapTile != null && mapTile.getTileType().equals(TileType.MOUNTAIN)) {
						mapTile.setOre(oreToUse, random);
						amountOreGenerated++;
					}
				}
			}
		}

	}

	private GridPoint2 randomPointIn(MapRegion regionToUse) {
		GridPoint2 randomPoint = null;
		while (randomPoint == null) {
			randomPoint = new GridPoint2(
				random.nextInt(regionToUse.getMaxX() - regionToUse.getMinX()) + regionToUse.getMinX(),
				random.nextInt(regionToUse.getMaxY() - regionToUse.getMinY()) + regionToUse.getMinY()
			);
			GameMapTile randomTile = gameMap.get(randomPoint.x, randomPoint.y);
			if (randomTile != null && !randomTile.getRegion().equals(regionToUse)) {
				// We're in the wrong region, so reject
				randomPoint = null;
			}
		}
		return randomPoint;
	}

	private MapRegion pickRegion(List<MapRegion> largeMountainRegions, int totalMountainTiles) {
		int regionPicker = random.nextInt(totalMountainTiles);
		Iterator<MapRegion> iterator = largeMountainRegions.iterator();
		MapRegion regionToUse = null;
		while (regionPicker >= 0) {
			regionToUse = iterator.next();
			regionPicker -= regionToUse.size();
		}
		return regionToUse;
	}


	private OreType pickOreToGenerate(GameMapTile gameMapTile) {
		List<OreType> oreTypeList = gameMapTile.getRockType().getOreTypes();
		float totalWeighting = 0f;
		for (OreType oreType : oreTypeList) {
			totalWeighting += oreType.getWeighting();
		}


		OreType oreToUse = null;
		Iterator<OreType> iterator = oreTypeList.iterator();
		float orePicker = random.nextFloat() * totalWeighting;
		while (orePicker >= 0) {
			oreToUse = iterator.next();
			orePicker -= oreToUse.getWeighting();
		}

		return oreToUse;
	}

	private GemType pickGemToGenerate(GameMapTile gameMapTile) {
		List<GemType> gemTypes = generationParams.getGemTypes(gameMapTile.getRockGroup());
		float totalWeighting = 0f;
		for (GemType gemType : gemTypes) {
			totalWeighting += gemType.getWeighting();
		}


		GemType gemToUse = null;
		Iterator<GemType> iterator = gemTypes.iterator();
		float orePicker = random.nextFloat() * totalWeighting;
		while (orePicker >= 0 && iterator.hasNext()) {
			gemToUse = iterator.next();
			orePicker -= gemToUse.getWeighting();
		}

		return gemToUse;
	}

	public AbstractGameMap getCurrentMap() {
		if (gameMap != null) {
			return gameMap;
		} else if (heightMap != null) {
			return heightMap;
		} else {
			return vertexMap;
		}
	}

	public boolean isFinished() {
		return finished;
	}
}
