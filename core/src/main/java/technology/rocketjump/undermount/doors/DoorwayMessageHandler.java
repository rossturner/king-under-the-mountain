package technology.rocketjump.undermount.doors;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.NotImplementedException;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.assets.entities.wallcap.WallCapAssetDictionary;
import technology.rocketjump.undermount.assets.entities.wallcap.model.WallCapAsset;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.behaviour.furniture.DoorBehaviour;
import technology.rocketjump.undermount.entities.components.furniture.ConstructedEntityComponent;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureLayoutDictionary;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.factories.FurnitureEntityFactory;
import technology.rocketjump.undermount.entities.factories.ItemEntityAttributesFactory;
import technology.rocketjump.undermount.entities.factories.ItemEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.DoorwayEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.jobs.model.JobTarget;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.DoorwayPlacementMessage;
import technology.rocketjump.undermount.messaging.types.EntityMessage;
import technology.rocketjump.undermount.messaging.types.ParticleRequestMessage;
import technology.rocketjump.undermount.messaging.types.RoofConstructionMessage;
import technology.rocketjump.undermount.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;

import java.util.Optional;

import static technology.rocketjump.undermount.doors.DoorwayOrientation.EAST_WEST;
import static technology.rocketjump.undermount.doors.DoorwayOrientation.NORTH_SOUTH;
import static technology.rocketjump.undermount.mapping.MapMessageHandler.updateTile;
import static technology.rocketjump.undermount.misc.VectorUtils.toVector;
import static technology.rocketjump.undermount.rendering.entities.EntityRenderer.PIXELS_PER_TILE;

@Singleton
public class DoorwayMessageHandler implements GameContextAware, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final WallCapAssetDictionary wallCapAssetDictionary;
	private final FurnitureEntityFactory furnitureEntityFactory;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final FurnitureLayoutDictionary furnitureLayoutDictionary;
	private final ItemEntityFactory itemEntityFactory;
	private final ItemEntityAttributesFactory itemEntityAttributesFactory;
	private final SoundAssetDictionary soundAssetDictionary;
	private final ParticleEffectType dustCloudParticleEffect;
	private GameContext gameContext;

	@Inject
	public DoorwayMessageHandler(MessageDispatcher messageDispatcher, WallCapAssetDictionary wallCapAssetDictionary,
								 FurnitureEntityFactory furnitureEntityFactory, FurnitureTypeDictionary furnitureTypeDictionary,
								 FurnitureLayoutDictionary furnitureLayoutDictionary, ItemEntityFactory itemEntityFactory,
								 ItemEntityAttributesFactory itemEntityAttributesFactory, SoundAssetDictionary soundAssetDictionary,
								 ParticleEffectTypeDictionary particleEffectTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.wallCapAssetDictionary = wallCapAssetDictionary;
		this.furnitureEntityFactory = furnitureEntityFactory;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.furnitureLayoutDictionary = furnitureLayoutDictionary;
		this.itemEntityFactory = itemEntityFactory;
		this.itemEntityAttributesFactory = itemEntityAttributesFactory;
		this.soundAssetDictionary = soundAssetDictionary;

		messageDispatcher.addListener(this, MessageType.CREATE_DOORWAY);
		messageDispatcher.addListener(this, MessageType.DECONSTRUCT_DOOR);

		dustCloudParticleEffect = particleEffectTypeDictionary.getByName("Dust cloud above"); // MODDING expose this
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.CREATE_DOORWAY: {
				return handle((DoorwayPlacementMessage) msg.extraInfo);
			}
			case MessageType.DECONSTRUCT_DOOR: {
				Doorway target = (Doorway) msg.extraInfo;
				MapTile targetTile = gameContext.getAreaMap().getTile(target.getTileLocation());
				if (targetTile != null && targetTile.hasDoorway()) {
					Doorway doorway = targetTile.getDoorway();

					ItemEntityAttributes itemAttributes = itemEntityAttributesFactory.resourceFromDoorway(doorway);
					itemEntityFactory.create(itemAttributes, target.getTileLocation(), true, gameContext);

					messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(dustCloudParticleEffect,
							Optional.empty(), Optional.of(new JobTarget(targetTile)), (p) -> {}));

					messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, new EntityMessage(doorway.getDoorEntity().getId()));
					targetTile.setDoorway(null);
					updateTile(targetTile, gameContext);

					for (MapTile neighbourTile : gameContext.getAreaMap().getOrthogonalNeighbours(targetTile.getTileX(), targetTile.getTileY()).values()) {
						if (neighbourTile.hasRoom()) {
							neighbourTile.getRoomTile().getRoom().checkIfEnclosed(gameContext.getAreaMap());
						}
					}
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private boolean handle(DoorwayPlacementMessage message) {
		GridPoint2 doorwayLocation = message.getTilePosition();

		MapTile targetTile = gameContext.getAreaMap().getTile(doorwayLocation);

		Doorway doorway = new Doorway();
		doorway.setOrientation(message.getOrientation());
		doorway.setDoorwayMaterialType(message.getDoorwayMaterial().getMaterialType());
		doorway.setDoorwaySize(message.getDoorwaySize());
		doorway.setTileLocation(doorwayLocation);

		targetTile.setDoorway(doorway);

		GameMaterial attachedWallMaterial;

		if (message.getOrientation().equals(NORTH_SOUTH)) {
			Entity southCap = buildWallCap(doorwayLocation.cpy().add(0, -1), doorway, EntityAssetOrientation.DOWN, -0.5f + (1f / PIXELS_PER_TILE));
			if (southCap != null) {
				doorway.getWallCapEntities().add(southCap);
			}
			attachedWallMaterial = getAttachedWallMaterial(southCap);
		} else {
			Entity westCap = buildWallCap(doorwayLocation.cpy().add(-1, 0), doorway, EntityAssetOrientation.LEFT, 0.5f);
			Entity eastCap = buildWallCap(doorwayLocation.cpy().add(1, 0), doorway, EntityAssetOrientation.RIGHT, 0.5f);

			if (westCap != null) {
				doorway.getWallCapEntities().add(westCap);
			}
			if (eastCap != null) {
				doorway.getWallCapEntities().add(eastCap);
			}
			attachedWallMaterial = getAttachedWallMaterial(westCap);
		}

		doorway.setFrameEntity(createFrameEntity(message, attachedWallMaterial));
		doorway.setDoorEntity(createDoorEntity(message));

		if (targetTile.getRoof().getState().equals(TileRoofState.OPEN)) {
			messageDispatcher.dispatchMessage(MessageType.ROOF_CONSTRUCTED, new RoofConstructionMessage(
					doorwayLocation, message.getDoorwayMaterial()
			));
		}

		for (CompassDirection direction : CompassDirection.CARDINAL_DIRECTIONS) {
			MapTile neighbourTile = gameContext.getAreaMap().getTile(doorwayLocation.x + direction.getXOffset(), doorwayLocation.y + direction.getYOffset());
			if (neighbourTile != null && neighbourTile.hasRoom()) {
				neighbourTile.getRoomTile().getRoom().checkIfEnclosed(gameContext.getAreaMap());
			}
		}


		return true;
	}

	private Entity createFrameEntity(DoorwayPlacementMessage message, GameMaterial attachedWallMaterial) {

		FurnitureEntityAttributes attributes = new FurnitureEntityAttributes(SequentialIdGenerator.nextId());
		attributes.setPrimaryMaterialType(message.getDoorwayMaterial().getMaterialType());

		if (message.getDoorwayMaterial().getMaterialType().equals(attachedWallMaterial.getMaterialType())) {
			// Same frame material type as attached wall, so use walls material as frame material
			attributes.getMaterials().put(attachedWallMaterial.getMaterialType(), attachedWallMaterial);
		} else {
			// Different material type to wall, so use door's material
			attributes.getMaterials().put(message.getDoorwayMaterial().getMaterialType(), message.getDoorwayMaterial());
		}
		attributes.setFurnitureType(furnitureTypeDictionary.getByName(selectFrameFurnitureTypeName(message)));
		attributes.setCurrentLayout(pickFurnitureLayout(message));
		Entity entity = furnitureEntityFactory.create(attributes, message.getTilePosition(), null, gameContext);

		// Frame should be bottom of tile to overlap entities
		entity.getLocationComponent().setWorldPosition(toVector(message.getTilePosition()).add(0, -0.5f), false, false);
		return entity;
	}

	private Entity createDoorEntity(DoorwayPlacementMessage message) {

		FurnitureEntityAttributes attributes = new FurnitureEntityAttributes(SequentialIdGenerator.nextId());
		attributes.setPrimaryMaterialType(message.getDoorwayMaterial().getMaterialType());
		attributes.getMaterials().put(message.getDoorwayMaterial().getMaterialType(), message.getDoorwayMaterial());
		attributes.setFurnitureType(furnitureTypeDictionary.getByName(selectDoorFurnitureTypeName(message)));
		attributes.setCurrentLayout(pickFurnitureLayout(message));

		DoorBehaviour doorBehaviour = new DoorBehaviour();
		doorBehaviour.setSoundAssets(soundAssetDictionary.getByName("DoorOpen"), null/*soundAssetDictionary.getByName("DoorClose")*/);

		Entity entity = furnitureEntityFactory.create(attributes, message.getTilePosition(), doorBehaviour, gameContext);
		entity.addComponent(new ConstructedEntityComponent());

		messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, entity);

		// Remove from map
		entity.getLocationComponent().setWorldPosition(null, false, true);
		// Door should be top of tile so entities overlap it
		entity.getLocationComponent().setWorldPosition(toVector(message.getTilePosition()).add(0, 0.5f - (1f/PIXELS_PER_TILE)), false, false);
		return entity;
	}

	private Entity buildWallCap(GridPoint2 wallTileLocation, Doorway doorway, EntityAssetOrientation assetOrientation, float positionYOffset) {
		DoorwayEntityAttributes attributes = new DoorwayEntityAttributes(SequentialIdGenerator.nextId());

		MapTile tileAtPosition = gameContext.getAreaMap().getTile(wallTileLocation);
		if (!tileAtPosition.hasWall()) {
			Logger.error("No wall in expected location for doorway");
			return null;
		}
		attributes.setAttachedWallMaterial(tileAtPosition.getWall().getMaterial());
		attributes.setAttachedWallType(tileAtPosition.getWall().getWallType());
		attributes.setOrientation(assetOrientation);

		Entity wallCapEntity = furnitureEntityFactory.create(attributes, doorway.getTileLocation(), null, gameContext);

		WallCapAsset baseAsset = wallCapAssetDictionary.getMatching(doorway, attributes);
		wallCapEntity.getPhysicalEntityComponent().setBaseAsset(baseAsset);
		wallCapEntity.getPhysicalEntityComponent().getTypeMap().put(baseAsset.getType(), baseAsset);
		wallCapEntity.getLocationComponent().setOrientation(assetOrientation);

		// Wall cap should be bottom of tile  (+1 pixel) to overlap entities
		wallCapEntity.getLocationComponent().setWorldPosition(toVector(doorway.getTileLocation()).add(0, positionYOffset), false, false);

		tileAtPosition.update(gameContext.getAreaMap().getNeighbours(wallTileLocation), gameContext.getAreaMap().getVertices(wallTileLocation.x, wallTileLocation.y));
		return wallCapEntity;
	}

	private FurnitureLayout pickFurnitureLayout(DoorwayPlacementMessage message) {
		if (message.getDoorwaySize().equals(DoorwaySize.SINGLE)) {
			if (message.getOrientation().equals(EAST_WEST)) {
				return furnitureLayoutDictionary.getByName("1x1EW");
			} else {
				return furnitureLayoutDictionary.getByName("1x1NS");
			}
		} else {
			throw new NotImplementedException("Not yet implemented non-single doorway sizes");
		}
	}

	private String selectFrameFurnitureTypeName(DoorwayPlacementMessage message) {
		if (message.getDoorwaySize().equals(DoorwaySize.SINGLE)) {
			return "SINGLE_DOOR_FRAME";
		} else {
			throw new NotImplementedException("Not yet implemented non-single doorway sizes");
		}
	}

	private String selectDoorFurnitureTypeName(DoorwayPlacementMessage message) {
		if (message.getDoorwaySize().equals(DoorwaySize.SINGLE)) {
			return "SINGLE_DOOR";
		} else {
			throw new NotImplementedException("Not yet implemented non-single doorway sizes");
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

	private GameMaterial getAttachedWallMaterial(Entity wallCapEntity) {
		if (wallCapEntity != null) {
			DoorwayEntityAttributes attributes = (DoorwayEntityAttributes) wallCapEntity.getPhysicalEntityComponent().getAttributes();
			return attributes.getAttachedWallMaterial();
		} else {
			return GameMaterial.NULL_MATERIAL;
		}
	}
}
