package technology.rocketjump.undermount.entities;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Disposable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.constants.ConstantsRepo;
import technology.rocketjump.undermount.entities.factories.PlantEntityAttributesFactory;
import technology.rocketjump.undermount.entities.factories.PlantEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapgen.generators.ShrubPlanter;
import technology.rocketjump.undermount.mapgen.generators.TreePlanter;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileNeighbours;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.PlantCreationRequestMessage;
import technology.rocketjump.undermount.messaging.types.PlantSeedDispersedMessage;

import static technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesType.TREE;
import static technology.rocketjump.undermount.mapgen.generators.TreePlanter.MAX_TREE_HEIGHT_TILES;
import static technology.rocketjump.undermount.mapping.tile.TileRoof.OPEN;
import static technology.rocketjump.undermount.materials.model.GameMaterialType.EARTH;

@Singleton
public class PlantMessageHandler implements GameContextAware, Telegraph, Disposable {

	private final int MAX_NEIGHBOUR_SHRUBS_ALLOWED;
	private final MessageDispatcher messageDispatcher;
	private final PlantEntityAttributesFactory plantEntityAttributesFactory;
	private final PlantEntityFactory plantEntityFactory;
	private final TreePlanter treePlanter = new TreePlanter();
	private final ShrubPlanter shrubPlanter = new ShrubPlanter();
	private final EntityStore entityStore;

	private GameContext gameContext;

	@Inject
	public PlantMessageHandler(MessageDispatcher messageDispatcher,
							   PlantEntityAttributesFactory plantEntityAttributesFactory,
							   PlantEntityFactory plantEntityFactory, EntityStore entityStore,
							   ConstantsRepo constantsRepo) {
		this.messageDispatcher = messageDispatcher;
		this.plantEntityAttributesFactory = plantEntityAttributesFactory;
		this.plantEntityFactory = plantEntityFactory;
		this.entityStore = entityStore;
		MAX_NEIGHBOUR_SHRUBS_ALLOWED = constantsRepo.getWorldConstants().getMaxNeighbouringShrubs();
		messageDispatcher.addListener(this, MessageType.PLANT_SEED_DISPERSED);
		messageDispatcher.addListener(this, MessageType.PLANT_CREATION_REQUEST);
	}

	@Override
	public void dispose() {
		messageDispatcher.removeListener(this, MessageType.PLANT_SEED_DISPERSED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.PLANT_SEED_DISPERSED: {
				PlantSeedDispersedMessage entityMessage = (PlantSeedDispersedMessage) msg.extraInfo;
				handle(entityMessage);
				return true;
			}
			case MessageType.PLANT_CREATION_REQUEST: {
				PlantCreationRequestMessage entityMessage = (PlantCreationRequestMessage) msg.extraInfo;
				handle(entityMessage);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void handle(PlantSeedDispersedMessage message) {
		PlantSpeciesType plantType = message.getPlantSpecies().getPlantType();
		int numAttempts = plantType.equals(TREE) ? 5 : 3;
		int numToCreate = message.isFruit() ? 2 : 1;
		int created = 0;
		int attempted = 0;

		MapTile sourceTile = gameContext.getAreaMap().getTile(message.getOrigin());
		if (sourceTile == null) {
			Logger.error("Source tile for handling message is null in " + this.getClass().getName());
			return;
		}

		GridPoint2 sourcePoint = new GridPoint2(sourceTile.getTileX(), sourceTile.getTileY());

		while (created < numToCreate && attempted < numAttempts) {
			attempted++;
			GridPoint2 nearbyPosition = (plantType.equals(TREE)) ?
					treePlanter.randomPointNear(sourcePoint, gameContext.getRandom()) :
					shrubPlanter.randomPointNear(sourcePoint, gameContext.getRandom());

			MapTile nearbyTile = gameContext.getAreaMap().getTile(nearbyPosition.x, nearbyPosition.y);

			boolean isAllowedToSpawn = (plantType.equals(TREE)) ?
					isTreeAllowedAt(nearbyTile) :
					isShrubAllowedAt(nearbyTile);

			if (isAllowedToSpawn) {
				createPlant(message.getPlantSpecies(), nearbyPosition);
				created++;
			}
		}

	}

	private void handle(PlantCreationRequestMessage creationRequestMessage) {
		PlantEntityAttributes plantAttributes = plantEntityAttributesFactory.createBySeedMaterial(creationRequestMessage.getSeedMaterial(), gameContext.getRandom());
		if (plantAttributes != null) {
			Entity plantEntity = plantEntityFactory.create(plantAttributes, null, gameContext);
			// Not (currently) sending ENTITY_CREATED message
			creationRequestMessage.getCallback().entityCreated(plantEntity);
		} else {
			creationRequestMessage.getCallback().entityCreated(null);
		}
	}

	private boolean isShrubAllowedAt(MapTile targetTile) {
		if (targetTile == null || targetTile.getFloor() == null || targetTile.getRoof() == null || targetTile.getFloor().getMaterial() == null || targetTile.getFloor().hasBridge() ||
				!targetTile.getRoof().equals(OPEN) || !targetTile.isEmpty() || !EARTH.equals(targetTile.getFloor().getMaterial().getMaterialType())) {
			return false;
		}

		// For shrubs, allow max num nearby shrubs
		TileNeighbours tileNeighbours = gameContext.getAreaMap().getNeighbours(targetTile.getTileX(), targetTile.getTileY());
		int numNeighbourShrubs = 0;
		for (MapTile mapTile : tileNeighbours.values()) {
			if (mapTile.hasShrub()) {
				numNeighbourShrubs++;
			}
		}

		if (numNeighbourShrubs > MAX_NEIGHBOUR_SHRUBS_ALLOWED) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * FIXME This is effectively duplicated code from MapGenerator to ensure tree placement is consistent
	 */
	private boolean isTreeAllowedAt(MapTile targetTile) {
		if (targetTile == null || !targetTile.getRoof().equals(OPEN) || !targetTile.isEmpty() || targetTile.getFloor().hasBridge() ||
				!EARTH.equals(targetTile.getFloor().getMaterial().getMaterialType()) || targetTile.isWaterSource()) {
			return false;
		}
		GridPoint2 position = targetTile.getTilePosition();
		TiledMap map = gameContext.getAreaMap();


		// First check no trees nearby
		for (int x = position.x - 1; x <= position.x + 1; x++) {
			for (int y = position.y - MAX_TREE_HEIGHT_TILES; y <= position.y + MAX_TREE_HEIGHT_TILES; y++) {
				MapTile tile = map.getTile(x, y);
				if (tile == null) {
					continue; // If bottom of map is below tree, don't care too much
				}
				if (tile.hasTree()) {
					return false;
				}
			}
		}

		// Then check tree has room to grow into
		for (int x = position.x - 1; x <= position.x + 1; x++) {
			for (int y = position.y - 1; y <= position.y + MAX_TREE_HEIGHT_TILES; y++) {
				MapTile tile = map.getTile(x, y);
				if (tile == null || !tile.getRoof().equals(OPEN)) {
					return false;
				}
			}
		}


		// Extra check, can't be adjacent to wall or river
		for (MapTile neighbourTile : map.getNeighbours(targetTile.getTileX(), targetTile.getTileY()).values()) {
			if (neighbourTile.hasWall() || neighbourTile.hasTree() || neighbourTile.isWaterSource()) {
				return false;
			}
		}

		return true;
	}

	private void createPlant(PlantSpecies plantSpecies, GridPoint2 targetTile) {
		PlantEntityAttributes attributes = plantEntityAttributesFactory.createBySpecies(plantSpecies, gameContext.getRandom());
		Entity newEntity = plantEntityFactory.create(attributes, targetTile, gameContext);
		entityStore.add(newEntity);
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

}
