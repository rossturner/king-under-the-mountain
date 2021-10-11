package technology.rocketjump.undermount.mapping.factories;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.undermount.entities.behaviour.creature.CreatureGroup;
import technology.rocketjump.undermount.entities.behaviour.creature.HerdAnimalBehaviour;
import technology.rocketjump.undermount.entities.behaviour.creature.SolitaryAnimalBehaviour;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.factories.CreatureEntityAttributesFactory;
import technology.rocketjump.undermount.entities.factories.CreatureEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.Race;
import technology.rocketjump.undermount.entities.model.physical.creature.RaceBehaviourGroup;
import technology.rocketjump.undermount.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileExploration;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;
import technology.rocketjump.undermount.settlement.CreatureTracker;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class CreaturePopulator {

	private static final int MIN_ANIMALS_ON_SPAWN = 29;
	private static final int MAX_ANIMALS_ON_SPAWN = 57;
	// MODDING expose the below as a global constant
	private static final List<Class<? extends BehaviourComponent>> ANIMAL_BEHAVIOURS = List.of(
			HerdAnimalBehaviour.class,
			SolitaryAnimalBehaviour.class
	);
	private static final float MIN_DISTANCE_FROM_EMBARK = 30f;
	private static final float MIN_DISTANCE_FROM_OTHER_CREATURES = 20f;
	private static final int MAP_SIZE_ANIMAL_RATIO = 1500;
	private final RaceDictionary raceDictionary;
	private final CreatureEntityAttributesFactory creatureEntityAttributesFactory;
	private final CreatureEntityFactory creatureEntityFactory;
	private final CreatureTracker creatureTracker;

	@Inject
	public CreaturePopulator(RaceDictionary raceDictionary, CreatureEntityAttributesFactory creatureEntityAttributesFactory,
							 CreatureEntityFactory creatureEntityFactory, CreatureTracker creatureTracker) {
		this.raceDictionary = raceDictionary;
		this.creatureEntityAttributesFactory = creatureEntityAttributesFactory;
		this.creatureEntityFactory = creatureEntityFactory;
		this.creatureTracker = creatureTracker;
	}


	public void initialiseMap(GameContext gameContext) {
		int animalsToAdd = selectInitialAnimalAmount(gameContext.getRandom());

		Logger.debug("Adding " + animalsToAdd + " animals");

		addAnimalsToMap(animalsToAdd, false, gameContext);
	}

	public void addAnimalsAtEdge(GameContext gameContext) {
		int maxAnimalsForMapSize = gameContext.getAreaMap().getWidth() * gameContext.getAreaMap().getHeight() / MAP_SIZE_ANIMAL_RATIO;
		int maxAnimalsToAdd = maxAnimalsForMapSize - creatureTracker.count();
		if (maxAnimalsToAdd > 0) {
			int animalsToAdd = gameContext.getRandom().nextInt(maxAnimalsToAdd);
			addAnimalsToMap(animalsToAdd, true, gameContext);
		}
	}

	private void addAnimalsToMap(int animalsToAdd, boolean addingAtMapEdge, GameContext gameContext) {
		List<Race> animalRaces = raceDictionary.getAll().stream()
				.filter(r -> r.getBehaviour().getBehaviourClass() != null && ANIMAL_BEHAVIOURS.contains(r.getBehaviour().getBehaviourClass()))
				.collect(Collectors.toList());
		Set<GridPoint2> creatureSpawnLocations = new HashSet<>();

		Logger.debug("Adding " + animalsToAdd + " animals");

		while (animalsToAdd > 0) {
			Race selectedRace = animalRaces.get(gameContext.getRandom().nextInt(animalRaces.size()));
			MapTile spawnLocation = findSpawnLocation(gameContext, creatureSpawnLocations, addingAtMapEdge);
			if (spawnLocation == null) {
				Logger.warn("Could not find valid spawn location for more animals");
				break;
			} else {
				creatureSpawnLocations.add(spawnLocation.getTilePosition());
			}

			if (selectedRace.getBehaviour().getGroup() != null) {
				CreatureGroup group = new CreatureGroup();
				group.setGroupId(SequentialIdGenerator.nextId());
				group.setHomeLocation(spawnLocation.getTilePosition());

				int numToAddInGroup = selectGroupSize(selectedRace.getBehaviour().getGroup(), gameContext.getRandom());
				while (numToAddInGroup > 0) {
					MapTile spawnTile = addingAtMapEdge ? spawnLocation : pickNearbyTileInRegion(5, spawnLocation, gameContext);
					CreatureEntityAttributes attributes = creatureEntityAttributesFactory.create(selectedRace);
					Entity entity = creatureEntityFactory.create(attributes, spawnTile.getWorldPositionOfCenter(), new Vector2(), gameContext);
					if (entity.getBehaviourComponent() instanceof CreatureBehaviour) {
						((CreatureBehaviour) entity.getBehaviourComponent()).setCreatureGroup(group);
					}
					numToAddInGroup--;
					animalsToAdd--;
				}
			} else {
				// add individual to map
				CreatureEntityAttributes attributes = creatureEntityAttributesFactory.create(selectedRace);
				creatureEntityFactory.create(attributes, spawnLocation.getWorldPositionOfCenter(), new Vector2(), gameContext);
				animalsToAdd--;
			}

		}
	}

	private MapTile findSpawnLocation(GameContext gameContext, Set<GridPoint2> creatureSpawnLocations, boolean addingAtMapEdge) {
		for (int attempt = 0; attempt <= 100; attempt++) {
			TiledMap map = gameContext.getAreaMap();
			MapTile randomTile;
			if (addingAtMapEdge) {
				if (gameContext.getRandom().nextBoolean()) {
					// Adding at top or bottom
					if (gameContext.getRandom().nextBoolean()) {
						// adding as top edge
						randomTile = map.getTile(gameContext.getRandom().nextInt(map.getWidth()), map.getHeight() - 1);
					} else {
						// adding at bottom edge
						randomTile = map.getTile(gameContext.getRandom().nextInt(map.getWidth()), 0);
					}
				} else {
					// adding at left or right
					if (gameContext.getRandom().nextBoolean()) {
						// adding at left edge
						randomTile = map.getTile(0, gameContext.getRandom().nextInt(map.getHeight()));
					} else {
						// adding at right edge
						randomTile = map.getTile(map.getWidth() - 1, gameContext.getRandom().nextInt(map.getHeight()));
					}
				}
			} else {
				randomTile = map.getTile(gameContext.getRandom().nextInt(map.getWidth()), gameContext.getRandom().nextInt(map.getHeight()));
			}
			// pick random map location


			if (!randomTile.getExploration().equals(TileExploration.EXPLORED)) {
				continue;
			}
			if (!randomTile.isNavigable() || !randomTile.getRoof().getState().equals(TileRoofState.OPEN)) {
				continue;
			}

			if (withinDistance(MIN_DISTANCE_FROM_EMBARK, randomTile.getTilePosition(), gameContext.getAreaMap().getEmbarkPoint())) {
				continue;
			}

			boolean tooCloseToOtherAnimalSpawn = false;
			for (GridPoint2 creatureSpawnLocation : creatureSpawnLocations) {
				if (withinDistance(MIN_DISTANCE_FROM_OTHER_CREATURES, randomTile.getTilePosition(), creatureSpawnLocation)) {
					tooCloseToOtherAnimalSpawn = true;
					break;
				}
			}

			if (tooCloseToOtherAnimalSpawn) {
				continue;
			}

			return randomTile;
		}
		return null;
	}

	private MapTile pickNearbyTileInRegion(int radius, MapTile centralPoint, GameContext gameContext) {
		MapTile tileFound = null;
		while (tileFound == null) {
			tileFound = gameContext.getAreaMap().getTile(
					centralPoint.getTileX() - radius + (gameContext.getRandom().nextInt((radius * 2) + 1)),
					centralPoint.getTileY() - radius + (gameContext.getRandom().nextInt((radius * 2) + 1))
			);
			if (tileFound != null && (tileFound.getRegionId() != centralPoint.getRegionId() || !tileFound.isNavigable())) {
				tileFound = null;
			}
		}
		return tileFound;
	}

	private boolean withinDistance(float minDistance, GridPoint2 positionA, GridPoint2 positionB) {
		return positionA.dst(positionB) < minDistance;
	}

	private int selectInitialAnimalAmount(Random random) {
		return MIN_ANIMALS_ON_SPAWN + random.nextInt(MAX_ANIMALS_ON_SPAWN - MIN_ANIMALS_ON_SPAWN);
	}

	private int selectGroupSize(RaceBehaviourGroup group, Random random) {
		return group.getMinSize() + random.nextInt(group.getMaxSize() - group.getMinSize());
	}
}
