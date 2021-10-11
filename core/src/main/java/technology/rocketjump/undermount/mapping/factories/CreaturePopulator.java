package technology.rocketjump.undermount.mapping.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.undermount.entities.behaviour.creature.CreatureGroup;
import technology.rocketjump.undermount.entities.behaviour.creature.HerdAnimalBehaviour;
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

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class CreaturePopulator {

	private static final int MIN_ANIMALS_PER_MAP = 29;
	private static final int MAX_ANIMALS_PER_MAP = 57;
	// MODDING expose the below as a global constant
	private static final List<Class<? extends BehaviourComponent>> ANIMAL_BEHAVIOURS = List.of(
			HerdAnimalBehaviour.class
	);
	private static final float MIN_DISTANCE_FROM_EMBARK = 30f;
	private static final float MIN_DISTANCE_FROM_OTHER_CREATURES = 20f;
	private final RaceDictionary raceDictionary;
	private final CreatureEntityAttributesFactory creatureEntityAttributesFactory;
	private final CreatureEntityFactory creatureEntityFactory;

	@Inject
	public CreaturePopulator(RaceDictionary raceDictionary, CreatureEntityAttributesFactory creatureEntityAttributesFactory,
							 CreatureEntityFactory creatureEntityFactory) {
		this.raceDictionary = raceDictionary;
		this.creatureEntityAttributesFactory = creatureEntityAttributesFactory;
		this.creatureEntityFactory = creatureEntityFactory;
	}


	public void initialiseMap(GameContext gameContext, MessageDispatcher messageDispatcher) {
		int animalsToAdd = selectInitialAnimalAmount(gameContext.getRandom());

		List<Race> animalRaces = raceDictionary.getAll().stream()
				.filter(r -> r.getBehaviour().getBehaviourClass() != null && ANIMAL_BEHAVIOURS.contains(r.getBehaviour().getBehaviourClass()))
				.collect(Collectors.toList());
		Set<GridPoint2> creatureSpawnLocations = new HashSet<>();

		Logger.debug("Adding " + animalsToAdd + " animals");

		while (animalsToAdd > 0) {
			Race selectedRace = animalRaces.get(gameContext.getRandom().nextInt(animalRaces.size()));
			MapTile spawnLocation = findSpawnLocation(gameContext, creatureSpawnLocations);
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
					MapTile nearbyTile = pickNearbyTileInRegion(5, spawnLocation, gameContext);
					CreatureEntityAttributes attributes = creatureEntityAttributesFactory.create(selectedRace);
					Entity entity = creatureEntityFactory.create(attributes, nearbyTile.getWorldPositionOfCenter(), new Vector2(), gameContext);
					if (entity.getBehaviourComponent() instanceof CreatureBehaviour) {
						((CreatureBehaviour)entity.getBehaviourComponent()).setCreatureGroup(group);
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

		Logger.debug("Finished adding animals with " + animalsToAdd + " remaining");
	}

	private MapTile findSpawnLocation(GameContext gameContext, Set<GridPoint2> creatureSpawnLocations) {
		for (int attempt = 0; attempt <= 100; attempt++) {
			// pick random map location
			TiledMap map = gameContext.getAreaMap();
			MapTile randomTile = map.getTile(gameContext.getRandom().nextInt(map.getWidth()), gameContext.getRandom().nextInt(map.getHeight()));

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
		return MIN_ANIMALS_PER_MAP + random.nextInt(MAX_ANIMALS_PER_MAP - MIN_ANIMALS_PER_MAP);
	}

	private int selectGroupSize(RaceBehaviourGroup group, Random random) {
		return group.getMinSize() + random.nextInt(group.getMaxSize() - group.getMinSize());
	}
}
