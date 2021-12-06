package technology.rocketjump.undermount.mapping.factories;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.FloorTypeDictionary;
import technology.rocketjump.undermount.assets.WallTypeDictionary;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.entities.EntityStore;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.undermount.mapgen.model.input.GemType;
import technology.rocketjump.undermount.mapgen.model.input.OreType;
import technology.rocketjump.undermount.mapgen.model.output.GameMap;
import technology.rocketjump.undermount.mapgen.model.output.GameMapTile;
import technology.rocketjump.undermount.mapgen.model.output.MapRegion;
import technology.rocketjump.undermount.mapgen.model.output.TileType;
import technology.rocketjump.undermount.mapping.OutdoorLightProcessor;
import technology.rocketjump.undermount.mapping.model.InvalidMapGenerationException;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.MapVertex;
import technology.rocketjump.undermount.mapping.tile.layout.WallLayout;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoof;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;
import technology.rocketjump.undermount.mapping.tile.wall.Wall;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.zones.Zone;
import technology.rocketjump.undermount.zones.ZoneClassification;
import technology.rocketjump.undermount.zones.ZoneTile;

import java.util.*;
import java.util.stream.Collectors;

import static technology.rocketjump.undermount.mapgen.model.FloorType.Rock;
import static technology.rocketjump.undermount.mapgen.model.output.TileSubType.LOAMY_FLOOR_CAVE;

public class GameMapConverter {

	private final FloorTypeDictionary floorTypeDictionary;
	private final WallTypeDictionary wallTypeDictionary;
	private final GameMaterialDictionary materialDictionary;
	private final OutdoorLightProcessor outdoorLightProcessor;
	private final WaterFlowCalculator waterFlowCalculator;
	private final WaterFlowVertexApplicator waterFlowVertexApplicator;
	private final EntityStore entityStore;

	@Inject
	public GameMapConverter(FloorTypeDictionary floorTypeDictionary, WallTypeDictionary wallTypeDictionary,
							GameMaterialDictionary materialDictionary, OutdoorLightProcessor outdoorLightProcessor,
							WaterFlowCalculator waterFlowCalculator, WaterFlowVertexApplicator waterFlowVertexApplicator,
							EntityStore entityStore) {
		this.floorTypeDictionary = floorTypeDictionary;
		this.wallTypeDictionary = wallTypeDictionary;
		this.materialDictionary = materialDictionary;
		this.outdoorLightProcessor = outdoorLightProcessor;
		this.waterFlowCalculator = waterFlowCalculator;
		this.waterFlowVertexApplicator = waterFlowVertexApplicator;
		this.entityStore = entityStore;
	}

	public void apply(GameMap generatedSourceMap, TiledMap targetMap, long seed) throws InvalidMapGenerationException {
		Logger.info("Converting MapGen to game map");
		long startTime = System.currentTimeMillis();
		Random random = new RandomXS128(seed);

		WallType roughStoneWallType = wallTypeDictionary.getByWallTypeName("rough_stone_wall");
		FloorType roughStoneFloorType = floorTypeDictionary.getByFloorTypeName("rough_stone");


		WallType oreWallType = wallTypeDictionary.getByWallTypeName("basic_ore");
		WallType gemWallType = wallTypeDictionary.getByWallTypeName("basic_gems");
		FloorType grassFloorType = floorTypeDictionary.getByFloorTypeName("grass");
		FloorType dirtFloorType = floorTypeDictionary.getByFloorTypeName("dirt");
		FloorType riverEdgeDirt = floorTypeDictionary.getByFloorTypeName("river-edge-dirt");
		FloorType gravelFloorType = floorTypeDictionary.getByFloorTypeName("gravel");
		FloorType stoneSlabFloorType = floorTypeDictionary.getByFloorTypeName("stone_slab"); // FIXME should be a river bed type

		GameMaterial grassMaterial = materialDictionary.getByName("Grass");
		GameMaterial dirtMaterial = materialDictionary.getByName("Dirt");
		GameMaterial gravelMaterial = materialDictionary.getByName("Gravel");
		GameMaterial slateMaterial = materialDictionary.getByName("Slate");
		GameMaterial waterMaterial = materialDictionary.getByName("Water");

		List<Entity> plantsThatReplaceAllInRegion = new ArrayList<>();

		for (int x = 0; x < generatedSourceMap.getWidth(); x++) {
			for (int y = 0; y < generatedSourceMap.getHeight(); y++) {
				GameMapTile sourceTile = generatedSourceMap.get(x, y);
				MapTile targetTile = targetMap.getTile(x, y);

				if (sourceTile.getTileType().equals(TileType.MOUNTAIN)) {
					GameMaterial currentMaterial = materialDictionary.getByName(sourceTile.getRockType().getName());
					targetTile.getRoof().setRoofMaterial(currentMaterial);
					targetTile.getRoof().setState(TileRoofState.MOUNTAIN_ROOF);
					if (sourceTile.hasRiver()) {
						targetTile.getFloor().setFloorType(stoneSlabFloorType);
						targetTile.getFloor().setMaterial(slateMaterial);
					} else if (sourceTile.getFloorType().equals(Rock)) {
						if (sourceTile.getTileSubType().equals(LOAMY_FLOOR_CAVE)) {
							targetTile.getFloor().setFloorType(dirtFloorType);
							targetTile.getFloor().setMaterial(dirtMaterial);
						} else {
							targetTile.getFloor().setFloorType(roughStoneFloorType);
							targetTile.getFloor().setMaterial(currentMaterial);
						}
						targetTile.getRoof().setState(TileRoofState.CAVERN);
					} else {
						targetTile.setWall(new Wall(new WallLayout(0), roughStoneWallType, currentMaterial),
								new TileRoof(TileRoofState.MOUNTAIN_ROOF, currentMaterial));
						targetTile.getFloor().setFloorType(roughStoneFloorType);
						targetTile.getFloor().setMaterial(currentMaterial);
						if (sourceTile.getGem() != null) {
							GemType gemType = sourceTile.getGem();
							targetTile.getWall().changeOre(gemWallType, materialDictionary.getByName(gemType.getName()));
						} else if (sourceTile.hasOre()) {
							OreType oreType = sourceTile.getOre();
							targetTile.getWall().changeOre(oreWallType, materialDictionary.getByName(oreType.getOreName()));
						}
					}
				} else {
					targetTile.getRoof().setState(TileRoofState.OPEN);

					switch (sourceTile.getSubRegion().getSubRegionType()) {
						case FOREST:
						case GRASSLAND:
							targetTile.getFloor().setFloorType(grassFloorType);
							targetTile.getFloor().setMaterial(grassMaterial);
							break;
						case PLAINS:
							if (random.nextBoolean()) {
								targetTile.getFloor().setFloorType(grassFloorType);
								targetTile.getFloor().setMaterial(grassMaterial);
							} else {
								targetTile.getFloor().setFloorType(dirtFloorType);
								targetTile.getFloor().setMaterial(grassMaterial);
							}
							break;
						case TUNDRA:
							targetTile.getFloor().setFloorType(gravelFloorType);
							targetTile.getFloor().setMaterial(gravelMaterial);
							break;
					}


					if (sourceTile.hasRiver()) {
						// TODO Switch to a river-bedrock style floor
						targetTile.getFloor().setFloorType(stoneSlabFloorType);
						targetTile.getFloor().setMaterial(waterMaterial);
					} else if (neighbourHasRiver(generatedSourceMap, x, y)) {
						targetTile.getFloor().setFloorType(riverEdgeDirt);
						targetTile.getFloor().setMaterial(grassMaterial);
					}


				}


				Entity createdPlant = null;
				if (sourceTile.hasTree()) {
					createdPlant = entityStore.createPlantForMap(sourceTile.getTree().getSpeciesName(), sourceTile.getPosition(), random);
				} else if (sourceTile.hasShrub()) {
					createdPlant = entityStore.createPlantForMap(sourceTile.getShrubType().getName(), sourceTile.getPosition(), random);
				} else if (sourceTile.hasMushroom()) {
					createdPlant = entityStore.createPlantForMap(sourceTile.getMushroomType().getName(), sourceTile.getPosition(), random);
				}

				if (createdPlant != null) {
					PlantEntityAttributes attributes = (PlantEntityAttributes) createdPlant.getPhysicalEntityComponent().getAttributes();
					if (attributes.getSpecies().getReplacesOtherPlantsInRegion() != 0) {
						plantsThatReplaceAllInRegion.add(createdPlant);
					}
				}
			}
		}

		for (Entity replacerPlant : plantsThatReplaceAllInRegion) {
			PlantSpecies replacementSpecies = ((PlantEntityAttributes)replacerPlant.getPhysicalEntityComponent().getAttributes()).getSpecies();
			int numToReplace = replacementSpecies.getReplacesOtherPlantsInRegion();
			MapTile originTile = targetMap.getTile(replacerPlant.getLocationComponent().getWorldPosition());
			GameMapTile sourceMapTile = generatedSourceMap.get(originTile.getTileX(), originTile.getTileY());

			List<GameMapTile> regionTiles = new ArrayList<>(sourceMapTile.getSubRegion().getTiles());
			Collections.shuffle(regionTiles, random);

			for (GameMapTile regionTile : regionTiles) {
				MapTile targetTile = targetMap.getTile(regionTile.getPosition());
				if (targetTile.hasPlant()) {
					Entity plant = targetTile.getPlant();
					PlantEntityAttributes attributes = (PlantEntityAttributes) plant.getPhysicalEntityComponent().getAttributes();
					if (!attributes.getSpecies().equals(replacementSpecies)) {
						entityStore.remove(plant.getId(), true);
						entityStore.createPlantForMap(replacementSpecies.getSpeciesName(), targetTile.getTilePosition(), random);
						numToReplace--;
						if (numToReplace <= 0) {
							break;
						}
					}
				}
			}
		}


		// Calculate river flow
		long beforeRiverGen = System.currentTimeMillis();
		waterFlowCalculator.calculateRiverFlow(generatedSourceMap, targetMap, seed);
		waterFlowVertexApplicator.applyFlowToVertices(targetMap, generatedSourceMap.getRiverTiles());
		Logger.info("River generation took " + (System.currentTimeMillis() - beforeRiverGen) + "ms");



		for (int x = 0; x < generatedSourceMap.getWidth() + 1; x++) {
			for (int y = 0; y < generatedSourceMap.getHeight() + 1; y++) {
				float heightmapValue = generatedSourceMap.getVertexGameMap().get(x, y).getHeight();
				targetMap.getVertex(x, y).setHeightmapValue(heightmapValue);
			}
		}

		// Randomise order of infrequent update entities so you don't see them change left to right
		entityStore.shuffle();

		GameMapTile gameMapTile = pickEmbarkPoint(generatedSourceMap);
		targetMap.setEmbarkPoint(gameMapTile.getPosition().cpy());

		for (int x = 0; x < generatedSourceMap.getWidth(); x++) {
			for (int y = 0; y < generatedSourceMap.getHeight(); y++) {
				MapTile targetTile = targetMap.getTile(x, y);
				targetTile.update(targetMap.getNeighbours(x, y), targetMap.getVertices(x, y), null);
				if (targetTile.getRoof().getState().equals(TileRoofState.OPEN)) {
					markAsOutside(targetTile, targetMap);
				}
				if (targetTile.getRegionId() == -1) {
					fillRegion(targetTile, targetMap);
				}
			}
		}

		// Must be done after region assignment
		setupRiverAccessZones(targetMap, generatedSourceMap.getRiverTiles());

		Logger.info("Total map conversion took " + (System.currentTimeMillis() - startTime) + "ms");
	}

	private void setupRiverAccessZones(TiledMap targetMap, List<GridPoint2> riverTiles) {
		Map<Integer, List<ZoneTile>> riverBorderTilesByRegion = new HashMap<>();
		Set<MapTile> visited = new HashSet<>();

		for (GridPoint2 riverTile : riverTiles) {
			for (MapTile riverNeighbour : targetMap.getNeighbours(riverTile.x, riverTile.y).values()) {
				if (visited.contains(riverNeighbour)) {
					continue;
				}

				if (riverNeighbour.isNavigable(null) && !riverNeighbour.isWaterSource()) {
					List<ZoneTile> zoneTiles = riverBorderTilesByRegion.computeIfAbsent(riverNeighbour.getRegionId(), x -> new ArrayList<>());
					zoneTiles.add(new ZoneTile(riverNeighbour, targetMap.getTile(riverTile)));
				}

				visited.add(riverNeighbour);
			}
		}

		// Create zones per region
		for (List<ZoneTile> tilesInSameRegion : riverBorderTilesByRegion.values()) {
			if (tilesInSameRegion.isEmpty()) {
				continue;
			}
			Map<GridPoint2, ZoneTile> mapToTilesInSameRegion = new HashMap<>();
			for (ZoneTile zoneTile : tilesInSameRegion) {
				mapToTilesInSameRegion.put(zoneTile.getAccessLocation(), zoneTile);
			}

			GameMaterial riverFloorMaterial = targetMap.getTile(tilesInSameRegion.get(0).getTargetTile()).getFloor().getMaterial();
			tilesInSameRegion.sort(Comparator.comparingInt(o -> Math.max(o.getAccessLocation().x, o.getAccessLocation().y)));
			Deque<ZoneTile> tilesToAllocate = new ArrayDeque<>(tilesInSameRegion);

			while (!tilesToAllocate.isEmpty()) {
				Set<ZoneTile> tilesForNewZone = new HashSet<>();
				ZoneTile initialZoneTile = tilesToAllocate.pop();
				mapToTilesInSameRegion.remove(initialZoneTile.getAccessLocation());
				tilesForNewZone.add(initialZoneTile);

				Deque<ZoneTile> frontier = new ArrayDeque<>();
				frontier.addAll(
						targetMap.getOrthogonalNeighbours(initialZoneTile.getAccessLocation().x, initialZoneTile.getAccessLocation().y)
								.values().stream()
								.map(mapTile -> mapToTilesInSameRegion.get(mapTile.getTilePosition()))
								.filter(Objects::nonNull)
								.collect(Collectors.toList())
				);

				// Keep adding neighbouring zoneTiles to tilesForNewZone until none left or max size reached
				while (!frontier.isEmpty()) {
					ZoneTile unallocatedTile = frontier.pop();
					tilesForNewZone.add(unallocatedTile);
					tilesToAllocate.remove(unallocatedTile);
					mapToTilesInSameRegion.remove(unallocatedTile.getAccessLocation());

					if (tilesForNewZone.size() >= MAX_TILES_PER_RIVER_ZONE) {
						break;
					}

					frontier.addAll(
							targetMap.getOrthogonalNeighbours(unallocatedTile.getAccessLocation().x, unallocatedTile.getAccessLocation().y)
									.values().stream()
									.map(mapTile -> mapToTilesInSameRegion.get(mapTile.getTilePosition()))
									.filter(Objects::nonNull)
									.collect(Collectors.toList())
					);
				}

				buildRiverZone(tilesForNewZone, targetMap, riverFloorMaterial);

			}

		}

	}

	private void buildRiverZone(Set<ZoneTile> tilesForNewZone, TiledMap targetMap, GameMaterial riverFloorMaterial) {
		if (tilesForNewZone.isEmpty()) {
			return;
		}

		Zone currentZone = new Zone(new ZoneClassification(ZoneClassification.ZoneType.LIQUID_SOURCE, false, riverFloorMaterial, true));
		currentZone.setActive(true);

		for (ZoneTile zoneTile : tilesForNewZone) {
			currentZone.add(
					targetMap.getTile(zoneTile.getAccessLocation()),
					targetMap.getTile(zoneTile.getTargetTile())
			);
		}

		targetMap.addZone(currentZone);
	}

	private static int MAX_TILES_PER_RIVER_ZONE = 12;

	private void fillRegion(MapTile initialTile, TiledMap targetMap) {
		MapTile.RegionType matchingRegionType = initialTile.getRegionType();
		int regionId = targetMap.createNewRegionId();

		Set<MapTile> visited = new HashSet<>();

		Queue<MapTile> frontier = new LinkedList<>();
		frontier.add(initialTile);

		while (!frontier.isEmpty()) {
			MapTile currentTile = frontier.poll();
			if (visited.contains(currentTile)) {
				continue;
			} else {
				visited.add(currentTile);
			}

			if (currentTile.getRegionType().equals(matchingRegionType)) {
				// Add this to region, add neighbours to frontier
				currentTile.setRegionId(regionId);
				frontier.addAll(targetMap.getOrthogonalNeighbours(currentTile.getTileX(), currentTile.getTileY()).values());
			}
		}
	}

	private void markAsOutside(MapTile tile, TiledMap areaMap) {
		tile.getRoof().setState(TileRoofState.OPEN);
		for (MapVertex vertex : areaMap.getVertexNeighboursOfCell(tile).values()) {
			vertex.setOutsideLightAmount(1.0f);
			outdoorLightProcessor.propagateLightFromMapVertex(areaMap, vertex, 1f);
		}
	}

	private boolean neighbourHasRiver(GameMap generatedMap, int x, int y) {
		return
				(generatedMap.get(x - 1, y- 1) != null && generatedMap.get(x - 1, y- 1).hasRiver()) ||
				(generatedMap.get(x , y- 1) != null && generatedMap.get(x, y- 1).hasRiver()) ||
				(generatedMap.get(x + 1, y- 1) != null && generatedMap.get(x + 1, y- 1).hasRiver()) ||
				(generatedMap.get(x - 1, y) != null && generatedMap.get(x - 1, y).hasRiver()) ||
				(generatedMap.get(x + 1, y) != null && generatedMap.get(x + 1, y).hasRiver()) ||
				(generatedMap.get(x - 1, y + 1) != null && generatedMap.get(x - 1, y+ 1).hasRiver()) ||
				(generatedMap.get(x,  y + 1) != null && generatedMap.get(x, y+ 1).hasRiver()) ||
				(generatedMap.get(x + 1, y+ 1) != null && generatedMap.get(x + 1, y+ 1).hasRiver());
	}


	private GameMapTile pickEmbarkPoint(GameMap generatedMap) throws InvalidMapGenerationException {
		MapRegion largestMountainRegion = new MapRegion(TileType.MOUNTAIN);
		for (MapRegion mapRegion : generatedMap.getRegions().values()) {
			if (mapRegion.getTileType().equals(TileType.MOUNTAIN)) {
				if (mapRegion.getTiles().size() > largestMountainRegion.getTiles().size()) {
					largestMountainRegion = mapRegion;
				}
			}
		}



		MapRegion largestOutsideRegion = new MapRegion(TileType.OUTSIDE);
		for (MapRegion mapRegion : generatedMap.getRegions().values()) {
			if (mapRegion.getTileType().equals(TileType.OUTSIDE)) {
				if (mapRegion.getTiles().size() > largestOutsideRegion.getTiles().size() &&
						regionBordersRiverAndSpecificMountain(mapRegion, largestMountainRegion, generatedMap)) {
					largestOutsideRegion = mapRegion;
				}
			}
		}

		if (largestOutsideRegion.size() == 0) {
			throw new InvalidMapGenerationException("Could not find suitable outdoor region");
		}

		// For each tile in the region, find the spot which is:
		// Near a mountain by X tiles
		// Has the most space around it positively and negatively
		Set<GameMapTile> regionTiles = largestOutsideRegion.getTiles();
		GameMapTile bestCandidate = regionTiles.iterator().next();
		float bestCandidateScore = 0;

		for (GameMapTile regionTile : regionTiles) {
			float thisCandidateScore = calculateEmbarkPointScore(regionTile, largestOutsideRegion, generatedMap);
			if (thisCandidateScore > bestCandidateScore) {
				bestCandidateScore = thisCandidateScore;
				bestCandidate = regionTile;
			}
		}

		return bestCandidate;
	}

	private boolean regionBordersRiverAndSpecificMountain(MapRegion mapRegion, MapRegion specificMountain, GameMap gameMap) {
		boolean bordersSpecificMountain = false;
		boolean bordersRiver = false;

		for (GameMapTile regionTile : mapRegion.getTiles()) {
			for (GameMapTile borderingTile : gameMap.getAllNeighbourTiles(regionTile.getPosition().x, regionTile.getPosition().y)) {
				if (borderingTile != null) {
					if (borderingTile.getRegion().equals(specificMountain)) {
						bordersSpecificMountain = true;
					}
					if (borderingTile.getRegion().getTileType().equals(TileType.RIVER)) {
						bordersRiver = true;
					}
					if (bordersSpecificMountain && bordersRiver) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private float calculateEmbarkPointScore(GameMapTile tile, MapRegion containingRegion, GameMap map) {
		if (tile.hasRiver()) {
			return -1; // Can't embark in river
		}
		for (GameMapTile neighbourTile : map.getAllNeighbourTiles(tile.getPosition().x, tile.getPosition().y)) {
			if (neighbourTile.hasRiver()) {
				return -1; // Can't embark next to river, or 3x3 area will cover river
			}
		}

		float mountainNearnessScore = 0;
		float surroundingSpaceScore = 0;

		int numTilesNorth = 0;
		int numTilesEast = 0;
		int numTilesSouth = 0;
		int numTilesWest = 0;
		int numTilesToMountain = Integer.MAX_VALUE;
		int numTilesToEdge = Math.max(map.getWidth(), map.getHeight());

		for (int x = tile.getPosition().x + 1; x < map.getHeight(); x++) {
			GameMapTile cursorTile = map.get(x, tile.getPosition().y);
			if (cursorTile == null) {
				if (numTilesWest < numTilesToEdge) {
					numTilesToEdge = numTilesWest;
				}
				break;
			}
			if (cursorTile.getRegion().getRegionId() == containingRegion.getRegionId()) {
				numTilesWest++;
			} else {
				// No longer in same region
				if (cursorTile.getRegion().getTileType().equals(TileType.MOUNTAIN)) {
					if (numTilesWest < numTilesToMountain) {
						numTilesToMountain = numTilesWest;
					}
				}
				break;
			}
		}

		for (int x = tile.getPosition().x - 1; x >= 0; x--) {
			GameMapTile cursorTile = map.get(x, tile.getPosition().y);
			if (cursorTile == null) {
				if (numTilesEast < numTilesToEdge) {
					numTilesToEdge = numTilesEast;
				}
				break;
			}
			if (cursorTile.getRegion().getRegionId() == containingRegion.getRegionId()) {
				numTilesEast++;
			} else {
				// No longer in same region
				if (cursorTile.getRegion().getTileType().equals(TileType.MOUNTAIN)) {
					if (numTilesEast < numTilesToMountain) {
						numTilesToMountain = numTilesEast;
					}
				}
			}
		}

		for (int y = tile.getPosition().y + 1; y < map.getHeight(); y++) {
			GameMapTile cursorTile = map.get(tile.getPosition().x, y);
			if (cursorTile == null) {
				if (numTilesNorth < numTilesToEdge) {
					numTilesToEdge = numTilesNorth;
				}
				break;
			}
			if (cursorTile.getRegion().getRegionId() == containingRegion.getRegionId()) {
				numTilesNorth++;
			} else {
				// No longer in same region
				if (cursorTile.getRegion().getTileType().equals(TileType.MOUNTAIN)) {
					if (numTilesNorth < numTilesToMountain) {
						numTilesToMountain = numTilesNorth;
					}
				}
			}
		}


		for (int y = tile.getPosition().y - 1; y >= 0; y--) {
			GameMapTile cursorTile = map.get( tile.getPosition().x, y);
			if (cursorTile == null) {
				if (numTilesWest < numTilesToEdge) {
					numTilesToEdge = numTilesWest;
				}
				break;
			}
			if (cursorTile.getRegion().getRegionId() == containingRegion.getRegionId()) {
				numTilesWest++;
			} else {
				// No longer in same region
				if (cursorTile.getRegion().getTileType().equals(TileType.MOUNTAIN)) {
					if (numTilesWest < numTilesToMountain) {
						numTilesToMountain = numTilesWest;
					}
				}
			}
		}

		mountainNearnessScore = Math.abs(DESIRED_NUM_TILES_TO_MOUNTAIN - numTilesToMountain);
		mountainNearnessScore = Math.min(mountainNearnessScore, DESIRED_NUM_TILES_TO_MOUNTAIN);
		mountainNearnessScore = 1 - (mountainNearnessScore / DESIRED_NUM_TILES_TO_MOUNTAIN);

		float edgeNearnessScore = numTilesToEdge / Math.max(map.getWidth(), map.getHeight());

		Vector2 mapCenter = new Vector2(map.getWidth() / 2f, map.getHeight() / 2f);
		Vector2 tileVector = new Vector2(tile.getPosition().x + 0.5f, tile.getPosition().y + 0.5f);
		float mapCenterScore = 1 - (mapCenter.cpy().sub(tileVector).len() / mapCenter.len());

		surroundingSpaceScore =
				((numTilesNorth + numTilesSouth) / (float)map.getHeight()) +
				((numTilesWest + numTilesEast) / (float)map.getWidth()) / 2f;

		return (mountainNearnessScore + surroundingSpaceScore + edgeNearnessScore + mapCenterScore) / 4;
	}

	private static final int DESIRED_NUM_TILES_TO_MOUNTAIN = 12;

}
