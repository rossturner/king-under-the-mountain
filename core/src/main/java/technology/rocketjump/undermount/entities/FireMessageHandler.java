package technology.rocketjump.undermount.entities;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.FloorTypeDictionary;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.entities.behaviour.BurnedEntityBehaviour;
import technology.rocketjump.undermount.entities.behaviour.effects.FireEffectBehaviour;
import technology.rocketjump.undermount.entities.behaviour.furniture.FurnitureBehaviour;
import technology.rocketjump.undermount.entities.behaviour.humanoids.CorpseBehaviour;
import technology.rocketjump.undermount.entities.components.AttachedEntitiesComponent;
import technology.rocketjump.undermount.entities.components.AttachedLightSourceComponent;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.furniture.DecorationInventoryComponent;
import technology.rocketjump.undermount.entities.components.humanoid.StatusComponent;
import technology.rocketjump.undermount.entities.factories.OngoingEffectAttributesFactory;
import technology.rocketjump.undermount.entities.factories.OngoingEffectEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.humanoid.DeathReason;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.humanoid.status.OnFireStatus;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.designation.TileDesignation;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.*;
import technology.rocketjump.undermount.rendering.utils.ColorMixer;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.settlement.FurnitureTracker;
import technology.rocketjump.undermount.ui.GameInteractionMode;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static technology.rocketjump.undermount.entities.model.EntityType.FURNITURE;
import static technology.rocketjump.undermount.entities.model.EntityType.STATIC_ENTITY_TYPES;
import static technology.rocketjump.undermount.messaging.MessageType.*;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.misc.VectorUtils.toVector;

@Singleton
public class FireMessageHandler implements GameContextAware, Telegraph {

	private static final int NUM_DIRECTIONS_TO_SPREAD_FIRE_IN = 3;
	private static final int MAX_DISTANCE_TO_SPREAD_FIRE_IN = 3;
	private final MessageDispatcher messageDispatcher;
	private final FloorType ashFloor;
	private final GameMaterial ashMaterial;
	private final ItemType ashesItemType;
	private final OngoingEffectAttributesFactory ongoingEffectAttributesFactory;
	private final OngoingEffectEntityFactory ongoingEffectEntityFactory;
	private final EntityStore entityStore;
	private final FurnitureTracker furnitureTracker;

	private GameContext gameContext;
	private GameMaterial boneMaterial;
	private Array<Color> blackenedColors = new Array<>();

	@Inject
	public FireMessageHandler(MessageDispatcher messageDispatcher, FloorTypeDictionary floorTypeDictionary,
							  GameMaterialDictionary gameMaterialDictionary, OngoingEffectAttributesFactory ongoingEffectAttributesFactory,
							  OngoingEffectEntityFactory ongoingEffectEntityFactory, ItemTypeDictionary itemTypeDictionary,
							  EntityStore entityStore, FurnitureTracker furnitureTracker) {
		this.messageDispatcher = messageDispatcher;
		this.ashFloor = floorTypeDictionary.getByFloorTypeName("ash");
		this.ashMaterial = gameMaterialDictionary.getByName("Ash");
		this.boneMaterial = gameMaterialDictionary.getByName("Bone");
		this.ashesItemType = itemTypeDictionary.getByName("Ashes");
		this.ongoingEffectAttributesFactory = ongoingEffectAttributesFactory;
		this.ongoingEffectEntityFactory = ongoingEffectEntityFactory;
		this.entityStore = entityStore;
		this.furnitureTracker = furnitureTracker;

		blackenedColors.add(HexColors.get("#605f5f"));
		blackenedColors.add(HexColors.get("#45403e"));
		blackenedColors.add(HexColors.get("#343231"));

		messageDispatcher.addListener(this, MessageType.SPREAD_FIRE_FROM_LOCATION);
		messageDispatcher.addListener(this, MessageType.SMALL_FIRE_STARTED);
		messageDispatcher.addListener(this, CONSUME_TILE_BY_FIRE);
		messageDispatcher.addListener(this, MessageType.CONSUME_ENTITY_BY_FIRE);
		messageDispatcher.addListener(this, MessageType.ADD_FIRE_TO_ENTITY);
		messageDispatcher.addListener(this, MessageType.FIRE_REMOVED);

	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case SPREAD_FIRE_FROM_LOCATION:
				Vector2 location = (Vector2) msg.extraInfo;
				spreadFireFrom(location, emptySet(), 4, false, MAX_DISTANCE_TO_SPREAD_FIRE_IN);
				return true;
			case SMALL_FIRE_STARTED:
				StartSmallFireMessage message = (StartSmallFireMessage) msg.extraInfo;
				MapTile spreadFromTile = gameContext.getAreaMap().getTile(message.jobLocation);
				if (spreadFromTile != null) {
					Set<Long> entityIdsToIgnore = spreadFromTile.getEntities().stream().map(Entity::getId).collect(Collectors.toSet());
					entityIdsToIgnore.add(message.targetEntityId);
					spreadFireFrom(toVector(message.jobLocation), entityIdsToIgnore, 1, true, 2);
				}
				return true;
			case CONSUME_TILE_BY_FIRE:
				MapTile tile = gameContext.getAreaMap().getTile((Vector2) msg.extraInfo);
				if (tile != null) {
					if (tile.hasWall()) {
						messageDispatcher.dispatchMessage(MessageType.REMOVE_WALL, tile.getTilePosition());
					}
					messageDispatcher.dispatchMessage(MessageType.CHANGE_FLOOR, new ChangeFloorMessage(tile.getTilePosition(), ashFloor, ashMaterial));
				}
				return true;
			case ADD_FIRE_TO_ENTITY:
				Entity targetEntity = (Entity) msg.extraInfo;
				OngoingEffectAttributes attributes = ongoingEffectAttributesFactory.createByTypeName("Fire");
				Entity fireEntity = ongoingEffectEntityFactory.create(attributes, targetEntity.getLocationComponent().getWorldOrParentPosition(), gameContext);

				AttachedEntitiesComponent attachedEntitiesComponent = targetEntity.getOrCreateComponent(AttachedEntitiesComponent.class);
				attachedEntitiesComponent.init(targetEntity, messageDispatcher, gameContext);
				attachedEntitiesComponent.addAttachedEntity(fireEntity);

				return true;
			case CONSUME_ENTITY_BY_FIRE:
				Entity entity = (Entity) msg.extraInfo;
				consumeEntityByFire(entity);
				return true;
			case FIRE_REMOVED:
				GridPoint2 removalLocation = (GridPoint2) msg.extraInfo;
				if (removalLocation != null) {
					checkToRemoveExtinguishDesignation(removalLocation);
				}
				return true;
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void spreadFireFrom(Vector2 location, Set<Long> entityIdsToIgnore, int maxFiresToStart, boolean staticEntitiesOnly, int maxDistanceToSpreadFire) {
		GridPoint2 centre = toGridPoint(location);
		int firesStarted = 0;
		MapTile centreTile = gameContext.getAreaMap().getTile(centre);
		if (centreTile == null) {
			return;
		}
		ArrayList<CompassDirection> directions = new ArrayList<>(CompassDirection.DIAGONAL_DIRECTIONS);
		Collections.shuffle(directions, gameContext.getRandom());
		for (CompassDirection direction : directions) {
			MapTile nextTile = centreTile;
			for (int distance = 1; distance <= maxDistanceToSpreadFire; distance++) {
				nextTile = selectNextTile(nextTile, direction);
				if (nextTile == null) {
					break;
				}

				if (nextTile.hasWall()) {
					if (nextTile.getWall().getMaterial().isCombustible()) {
						createFireInTile(nextTile);
						firesStarted++;
					}
					break;
				} else {

					Optional<Entity> combustibleEntity = nextTile.getEntities().stream()
							.filter(e -> staticEntitiesOnly ? STATIC_ENTITY_TYPES.contains(e.getType()) : true)
							.filter(e -> !entityIdsToIgnore.contains(e.getId()))
							.filter(e -> e.getPhysicalEntityComponent().getAttributes()
									.getMaterials().values().stream().anyMatch(GameMaterial::isCombustible))
							.findFirst();

					if (combustibleEntity.isPresent()) {
						StatusComponent statusComponent = combustibleEntity.get().getOrCreateComponent(StatusComponent.class);
						statusComponent.init(combustibleEntity.get(), messageDispatcher, gameContext);
						statusComponent.apply(new OnFireStatus());
						firesStarted++;
						break;
					} else if (nextTile.getFloor().getMaterial().isCombustible()) {
						createFireInTile(nextTile);
						firesStarted++;
						break;
					}


				}

			}
			if (firesStarted >= maxFiresToStart) {
				return;
			}
		}
	}

	private void checkToRemoveExtinguishDesignation(GridPoint2 removalLocation) {
		MapTile tile = gameContext.getAreaMap().getTile(removalLocation);
		TileDesignation designation = tile.getDesignation();
		if (designation != null) {
			GameInteractionMode interactionMode = GameInteractionMode.getByDesignationName(designation.getDesignationName());
			if (interactionMode != null) {
				if (!interactionMode.designationCheck.shouldDesignationApply(tile)) {
					// designation no longer applies
					messageDispatcher.dispatchMessage(REMOVE_DESIGNATION, new RemoveDesignationMessage(tile, designation));

					gameContext.getAreaMap().getTile(removalLocation).getEntities().stream()
							.filter(e -> e.getType().equals(FURNITURE))
							.forEach(
									e -> {
										FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) e.getPhysicalEntityComponent().getAttributes();
										List<GridPoint2> extraTiles = attributes.getCurrentLayout().getExtraTiles();
										for (GridPoint2 extraTileOffset : extraTiles) {
											checkToRemoveExtinguishDesignation(removalLocation.cpy().add(extraTileOffset));
										}
									}
							);
				}
			}
		}
	}

	private void consumeEntityByFire(Entity entity) {
		AttachedLightSourceComponent lightSourceComponent = entity.getComponent(AttachedLightSourceComponent.class);
		if (lightSourceComponent != null) {
			lightSourceComponent.setEnabled(false);
		}

		switch (entity.getType()) {
			case HUMANOID:
				messageDispatcher.dispatchMessage(MessageType.HUMANOID_DEATH, new HumanoidDeathMessage(entity, DeathReason.BURNING));
				if (entity.getBehaviourComponent() instanceof CorpseBehaviour) {
					HumanoidEntityAttributes attributes = (HumanoidEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					CorpseBehaviour corpseBehaviour = (CorpseBehaviour) entity.getBehaviourComponent();
					corpseBehaviour.setToFullyDecayed(attributes);
					attributes.setBoneColor(blackenedColor());
					attributes.setBodyMaterial(boneMaterial);
				}
				break;
			case ITEM:
				ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				attributes.setMaterial(ashMaterial);
				messageDispatcher.dispatchMessage(TRANSFORM_ITEM_TYPE, new TransformItemMessage(entity, ashesItemType));
				attributes.getMaterials().clear();
				attributes.setMaterial(ashMaterial);
				break;
			case PLANT:
				PlantEntityAttributes plantEntityAttributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				plantEntityAttributes.setBurned(ashMaterial, blackenedColor());
				entityStore.changeBehaviour(entity, new BurnedEntityBehaviour(), messageDispatcher);
				messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entity);
				break;
			case FURNITURE:
				FurnitureEntityAttributes furnitureEntityAttributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				furnitureEntityAttributes.setDestroyed(true);
				for (ColoringLayer coloringLayer : furnitureEntityAttributes.getOtherColors().keySet()) {
					furnitureEntityAttributes.setColor(coloringLayer, blackenedColor());
				}
				for (GameMaterial material : furnitureEntityAttributes.getMaterials().values()) {
					ColoringLayer coloringLayer = ColoringLayer.getByMaterialType(material.getMaterialType());
					furnitureEntityAttributes.setColor(coloringLayer, blackenedColor());
				}
				furnitureEntityAttributes.getMaterials().clear();
				furnitureEntityAttributes.getMaterials().put(furnitureEntityAttributes.getPrimaryMaterialType(), ashMaterial);
				if (!entity.getBehaviourComponent().getClass().equals(FurnitureBehaviour.class)) {
					// remove crafting station or other behaviour
					entityStore.changeBehaviour(entity, new FurnitureBehaviour(), messageDispatcher);
				}

				// Removes from usage such as beds
				furnitureTracker.furnitureRemoved(entity);

				InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
				if (inventoryComponent != null) {
					for (InventoryComponent.InventoryEntry inventoryEntry : new ArrayList<>(inventoryComponent.getInventoryEntries())) {
						messageDispatcher.dispatchMessage(DESTROY_ENTITY, new EntityMessage(inventoryEntry.entity.getId()));
					}
				}
				DecorationInventoryComponent decorationInventoryComponent = entity.getComponent(DecorationInventoryComponent.class);
				if (decorationInventoryComponent != null) {
					for (Entity decorationEntity : new ArrayList<>(decorationInventoryComponent.getDecorationEntities())) {
						messageDispatcher.dispatchMessage(DESTROY_ENTITY, new EntityMessage(decorationEntity.getId()));
					}
					decorationInventoryComponent.clear();
				}

				entity.getLocationComponent().setRotation(slightRotation());

				break;
			default:
				Logger.error("Not yet implemented: Consuming entity of type " + entity.getType() + " by fire");
		}

		entity.setTags(emptySet());
	}

	private float slightRotation() {
		float rotationAmount = gameContext.getRandom().nextFloat() * 15f;
		if (gameContext.getRandom().nextBoolean()) {
			rotationAmount *= -1f;
		}
		return rotationAmount;
	}

	/**
	 * This method is used to pick either the diagonal direction or one of the 2 adjacent orthogonal directions
	 */
	private MapTile selectNextTile(MapTile tile, CompassDirection diagonalDirection) {
		int xOffset = diagonalDirection.getXOffset();
		int yOffset = diagonalDirection.getYOffset();
		float roll = gameContext.getRandom().nextFloat();
		if (roll < 0.33f) {
			xOffset = 0;
		} else if (roll < 0.66f) {
			yOffset = 0;
		}

		return gameContext.getAreaMap().getTile(
				tile.getTileX() + xOffset,
				tile.getTileY() + yOffset
		);
	}

	private void createFireInTile(MapTile targetTile) {
		if (!targetTile.getEntities().stream().anyMatch(e -> e.getBehaviourComponent() instanceof FireEffectBehaviour)) {
			OngoingEffectAttributes attributes = ongoingEffectAttributesFactory.createByTypeName("Fire");
			ongoingEffectEntityFactory.create(attributes, targetTile.getWorldPositionOfCenter(), gameContext);
		}
	}

	private Color blackenedColor() {
		return ColorMixer.randomBlend(gameContext.getRandom(),
				blackenedColors);
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
