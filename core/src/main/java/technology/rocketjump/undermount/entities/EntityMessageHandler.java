package technology.rocketjump.undermount.entities;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.entities.behaviour.furniture.*;
import technology.rocketjump.undermount.entities.behaviour.humanoids.BrokenDwarfBehaviour;
import technology.rocketjump.undermount.entities.behaviour.humanoids.CorpseBehaviour;
import technology.rocketjump.undermount.entities.behaviour.humanoids.SettlerBehaviour;
import technology.rocketjump.undermount.entities.components.AttachedLightSourceComponent;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.components.furniture.ConstructedEntityComponent;
import technology.rocketjump.undermount.entities.components.furniture.FurnitureParticleEffectsComponent;
import technology.rocketjump.undermount.entities.components.humanoid.HistoryComponent;
import technology.rocketjump.undermount.entities.components.humanoid.NeedsComponent;
import technology.rocketjump.undermount.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.undermount.entities.components.humanoid.StatusComponent;
import technology.rocketjump.undermount.entities.factories.ItemEntityAttributesFactory;
import technology.rocketjump.undermount.entities.factories.ItemEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.undermount.entities.model.physical.humanoid.DeathReason;
import technology.rocketjump.undermount.entities.model.physical.humanoid.EquippedItemComponent;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HaulingComponent;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.humanoid.status.Death;
import technology.rocketjump.undermount.entities.model.physical.humanoid.status.StatusEffect;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesItem;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.jobs.JobFactory;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobState;
import technology.rocketjump.undermount.jobs.model.JobTarget;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.designation.TileDesignation;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.*;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;
import technology.rocketjump.undermount.rooms.HaulingAllocation;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.rooms.RoomStore;
import technology.rocketjump.undermount.rooms.components.StockpileComponent;
import technology.rocketjump.undermount.rooms.constructions.Construction;
import technology.rocketjump.undermount.settlement.FurnitureTracker;
import technology.rocketjump.undermount.settlement.ItemTracker;
import technology.rocketjump.undermount.settlement.SettlerTracker;
import technology.rocketjump.undermount.settlement.notifications.Notification;
import technology.rocketjump.undermount.settlement.notifications.NotificationType;
import technology.rocketjump.undermount.ui.GameInteractionMode;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;

import java.util.*;

import static technology.rocketjump.undermount.assets.entities.model.ColoringLayer.SKIN_COLOR;
import static technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation.DOWN;
import static technology.rocketjump.undermount.entities.ai.goap.actions.CancelLiquidAllocationAction.cancelLiquidAllocation;
import static technology.rocketjump.undermount.entities.ai.goap.actions.SleepOnFloorAction.changeToConsciousnessOnFloor;
import static technology.rocketjump.undermount.entities.components.ItemAllocation.AllocationState.CANCELLED;
import static technology.rocketjump.undermount.entities.components.ItemAllocation.Purpose.HAULING;
import static technology.rocketjump.undermount.entities.components.ItemAllocation.Purpose.HELD_IN_INVENTORY;
import static technology.rocketjump.undermount.entities.model.EntityType.*;
import static technology.rocketjump.undermount.entities.model.physical.humanoid.Consciousness.DEAD;
import static technology.rocketjump.undermount.jobs.JobMessageHandler.deconstructFurniture;
import static technology.rocketjump.undermount.jobs.ProfessionDictionary.NULL_PROFESSION;
import static technology.rocketjump.undermount.misc.VectorUtils.toVector;
import static technology.rocketjump.undermount.rooms.HaulingAllocation.AllocationPositionType.FURNITURE;
import static technology.rocketjump.undermount.rooms.HaulingAllocation.AllocationPositionType.*;

@Singleton
public class EntityMessageHandler implements GameContextAware, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final EntityAssetUpdater entityAssetUpdater;
	private final JobFactory jobFactory;
	private final EntityStore entityStore;
	private final ItemTracker itemTracker;
	private final FurnitureTracker furnitureTracker;
	private final SettlerTracker settlerTracker;
	private final RoomStore roomStore;
	private final ItemEntityAttributesFactory itemEntityAttributesFactory;
	private final ItemEntityFactory itemEntityFactory;
	private final ItemTypeDictionary itemTypeDictionary;
	private final I18nTranslator i18nTranslator;
	private final JobStore jobStore;
	private final SoundAssetDictionary soundAssetDictionary;
	private final SoundAsset treeFallSoundEffect;
	private GameContext gameContext;
	private ParticleEffectType leafExplosionParticleType;
	private ParticleEffectType chipExplosionParticleType;
	private ParticleEffectType treeShedLeafEffect;
	private ParticleEffectType liquidSplashEffect;
	private ParticleEffectType deconstructParticleEffect;

	@Inject
	public EntityMessageHandler(MessageDispatcher messageDispatcher, EntityAssetUpdater entityAssetUpdater,
								JobFactory jobFactory, EntityStore entityStore, ItemTracker itemTracker,
								FurnitureTracker furnitureTracker, SettlerTracker settlerTracker, RoomStore roomStore,
								ItemEntityAttributesFactory itemEntityAttributesFactory, ItemEntityFactory itemEntityFactory,
								ItemTypeDictionary itemTypeDictionary, I18nTranslator i18nTranslator, JobStore jobStore,
								SoundAssetDictionary soundAssetDictionary, ParticleEffectTypeDictionary particleEffectTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.entityAssetUpdater = entityAssetUpdater;
		this.jobFactory = jobFactory;
		this.entityStore = entityStore;
		this.itemTracker = itemTracker;
		this.furnitureTracker = furnitureTracker;
		this.settlerTracker = settlerTracker;
		this.roomStore = roomStore;
		this.itemEntityAttributesFactory = itemEntityAttributesFactory;
		this.itemEntityFactory = itemEntityFactory;
		this.itemTypeDictionary = itemTypeDictionary;
		this.i18nTranslator = i18nTranslator;
		this.jobStore = jobStore;
		this.soundAssetDictionary = soundAssetDictionary;

		this.leafExplosionParticleType = particleEffectTypeDictionary.getByName("Leaf explosion"); // MODDING expose this
		this.chipExplosionParticleType = particleEffectTypeDictionary.getByName("Chip explosion"); // MODDING expose this
		treeShedLeafEffect = particleEffectTypeDictionary.getByName("Falling leaf");
		liquidSplashEffect = particleEffectTypeDictionary.getByName("Liquid splash");
		deconstructParticleEffect = particleEffectTypeDictionary.getByName("Dust cloud above");
		this.treeFallSoundEffect = this.soundAssetDictionary.getByName("Mining Drop");

		messageDispatcher.addListener(this, MessageType.DESTROY_ENTITY);
		messageDispatcher.addListener(this, MessageType.JOB_REMOVED);
		messageDispatcher.addListener(this, MessageType.JOB_CANCELLED);
		messageDispatcher.addListener(this, MessageType.TREE_FELLED);
		messageDispatcher.addListener(this, MessageType.ENTITY_ASSET_UPDATE_REQUIRED);
		messageDispatcher.addListener(this, MessageType.ENTITY_CREATED);
		messageDispatcher.addListener(this, MessageType.ENTITY_DO_NOT_TRACK);
		messageDispatcher.addListener(this, MessageType.ITEM_PRIMARY_MATERIAL_CHANGED);
		messageDispatcher.addListener(this, MessageType.REQUEST_DOOR_OPEN);
		messageDispatcher.addListener(this, MessageType.REQUEST_FURNITURE_REMOVAL);
		messageDispatcher.addListener(this, MessageType.HAULING_ALLOCATION_CANCELLED);
		messageDispatcher.addListener(this, MessageType.CHANGE_PROFESSION);
		messageDispatcher.addListener(this, MessageType.APPLY_STATUS);
		messageDispatcher.addListener(this, MessageType.REMOVE_STATUS);
		messageDispatcher.addListener(this, MessageType.TRANSFORM_FURNITURE_TYPE);
		messageDispatcher.addListener(this, MessageType.TRANSFORM_ITEM_TYPE);
		messageDispatcher.addListener(this, MessageType.HUMANOID_DEATH);
		messageDispatcher.addListener(this, MessageType.HUMANOID_INSANITY);
		messageDispatcher.addListener(this, MessageType.LIQUID_SPLASH);
		messageDispatcher.addListener(this, MessageType.TREE_SHED_LEAVES);
		messageDispatcher.addListener(this, MessageType.FURNITURE_IN_USE);
		messageDispatcher.addListener(this, MessageType.FURNITURE_NO_LONGER_IN_USE);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.ENTITY_CREATED: {
				Entity createdEntity = (Entity) msg.extraInfo;
				entityStore.add(createdEntity);

				if (createdEntity.getBehaviourComponent() instanceof SettlerBehaviour) {
					settlerTracker.settlerAdded(createdEntity);
					for (InventoryComponent.InventoryEntry inventoryEntry : createdEntity.getComponent(InventoryComponent.class).getInventoryEntries()) {
						if (inventoryEntry.entity.getType().equals(ITEM)) {
							itemTracker.itemAdded(inventoryEntry.entity);
						}
					}
				} else if (createdEntity.getType().equals(ITEM)) {
					itemTracker.itemAdded(createdEntity);
				} else if (createdEntity.getType().equals(EntityType.FURNITURE)) {
					furnitureTracker.furnitureAdded(createdEntity);
				}

				return true;
			}
			case MessageType.ENTITY_DO_NOT_TRACK: {
				Entity entity = (Entity) msg.extraInfo;
				entity.getLocationComponent().setUntracked(true);

				if (entity.getType().equals(ITEM)) {
					itemTracker.itemRemoved(entity);
				} else if (entity.getType().equals(EntityType.FURNITURE)) {
					furnitureTracker.furnitureRemoved(entity);
				} else if (entity.getBehaviourComponent() instanceof SettlerBehaviour) {
					settlerTracker.settlerRemoved(entity);
				}

				return true;
			}
			case MessageType.ITEM_PRIMARY_MATERIAL_CHANGED: {
				ItemPrimaryMaterialChangedMessage message = (ItemPrimaryMaterialChangedMessage)msg.extraInfo;
				itemTracker.primaryMaterialChanged(message.item, message.oldPrimaryMaterial);
				return true;
			}
			case MessageType.DESTROY_ENTITY: {
				EntityMessage entityMessage = (EntityMessage) msg.extraInfo;
				long entityId = entityMessage.getEntityId();
				Entity removedEntity = entityStore.getById(entityId);
				if (removedEntity != null) {
					removedEntity.destroy(messageDispatcher, gameContext);
					// Need to remove after destroy() so things can clean their state up while the entity still exists
					entityStore.remove(entityId);
					if (removedEntity.getType().equals(ITEM)) {
						itemTracker.itemRemoved(removedEntity);
					} else if (removedEntity.getType().equals(EntityType.FURNITURE)) {
						furnitureTracker.furnitureRemoved(removedEntity);
					} else if (removedEntity.getBehaviourComponent() instanceof SettlerBehaviour) {
						settlerTracker.settlerRemoved(removedEntity);
						HumanoidEntityAttributes humanoidAttributes = (HumanoidEntityAttributes) removedEntity.getPhysicalEntityComponent().getAttributes();
						if (!humanoidAttributes.getConsciousness().equals(DEAD)) {
							// Destroying non-dead settler entity
							handle(new HumanoidDeathMessage(removedEntity, DeathReason.UNKNOWN));
						}
					}

					if (removedEntity.getLocationComponent().getWorldPosition() != null) {
						List<MapTile> allTiles = new ArrayList<>();
						MapTile mapTile = gameContext.getAreaMap().getTile(removedEntity.getLocationComponent().getWorldPosition());
						if (mapTile != null) {
							mapTile.removeEntity(entityId);
						}

						// TODO Maybe this should be refactored into a MultiTileEntityComponent which defines how an entity bridges extra tiles
						if (removedEntity.getType().equals(EntityType.FURNITURE)) {
							FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) removedEntity.getPhysicalEntityComponent().getAttributes();
							for (GridPoint2 extraTileOffset : attributes.getCurrentLayout().getExtraTiles()) {
								MapTile extraTile = gameContext.getAreaMap().getTile(mapTile.getTilePosition().cpy().add(extraTileOffset));
								if (extraTile != null) {
									extraTile.removeEntity(entityId);
								}
							}
						} else if (removedEntity.getType().equals(PLANT)) {
							PlantEntityAttributes attributes = (PlantEntityAttributes) removedEntity.getPhysicalEntityComponent().getAttributes();
							if (attributes.getRemovePestsJob() != null && !attributes.getRemovePestsJob().getJobState().equals(JobState.REMOVED)) {
								messageDispatcher.dispatchMessage(MessageType.JOB_CANCELLED, attributes.getRemovePestsJob());
							}
						}
					}
					Entity containerEntity = removedEntity.getLocationComponent().getContainerEntity();
					if (containerEntity != null) {
						InventoryComponent containerInventory = containerEntity.getComponent(InventoryComponent.class);
						EquippedItemComponent equippedItemComponent = containerEntity.getComponent(EquippedItemComponent.class);
						HaulingComponent haulingComponent = containerEntity.getComponent(HaulingComponent.class);

						if (haulingComponent != null && haulingComponent.getHauledEntity() != null && haulingComponent.getHauledEntity().getId() == removedEntity.getId()) {
							haulingComponent.clearHauledEntity();
							containerEntity.removeComponent(HaulingComponent.class);
						}
						if (equippedItemComponent != null && equippedItemComponent.getEquippedItem() != null && equippedItemComponent.getEquippedItem().getId() == removedEntity.getId()) {
							equippedItemComponent.clearEquippedItem();
							containerEntity.removeComponent(EquippedItemComponent.class);
						}
						if (containerInventory != null) {
							containerInventory.remove(removedEntity.getId());
						}
					}
				}
				return true;
			}
			case MessageType.JOB_REMOVED: {
				Job removedJob = (Job) msg.extraInfo;
				Long entityId = removedJob.getAssignedToEntityId();
				if (entityId != null) {
					Entity entity = entityStore.getById(entityId);
					if (entity != null && entity.getBehaviourComponent() instanceof SettlerBehaviour) {
						SettlerBehaviour behaviour = (SettlerBehaviour) entity.getBehaviourComponent();
						if (behaviour.getCurrentGoal() != null) {
							behaviour.getCurrentGoal().setInterrupted(true);
						}
					}
				}

				jobStore.remove(removedJob);
				// Fall through to job cancelled behaviour
			}
			case MessageType.JOB_CANCELLED: {
				Job removedJob = (Job) msg.extraInfo;

				if (removedJob.getHaulingAllocation() != null) {
					messageDispatcher.dispatchMessage(MessageType.HAULING_ALLOCATION_CANCELLED, removedJob.getHaulingAllocation());
					removedJob.setHaulingAllocation(null);
				}

				if (removedJob.getLiquidAllocation() != null) {
					cancelLiquidAllocation(removedJob.getLiquidAllocation(), gameContext);
					removedJob.setLiquidAllocation(null);
				}


				if (removedJob.getType().getName().equals("DECONSTRUCT")) {
					long potentialTargetEntityId = removedJob.getTargetId();
					Entity entity = entityStore.getById(potentialTargetEntityId);
					if (entity != null) {
						ConstructedEntityComponent constructedEntityComponent = entity.getComponent(ConstructedEntityComponent.class);
						if (constructedEntityComponent != null) {
							constructedEntityComponent.setDeconstructionJob(null);
						}
					}
					return true;
				}
				return true;
			}
			case MessageType.TREE_FELLED: {
				return handle((TreeFallenMessage)msg.extraInfo);
			}
			case MessageType.TREE_SHED_LEAVES: {
				ShedLeavesMessage message = (ShedLeavesMessage) msg.extraInfo;
				if (message.leafColor != null && !message.leafColor.equals(Color.CLEAR)) {
					messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(treeShedLeafEffect,
							Optional.of(message.parentEntity), Optional.of(new JobTarget(message.parentEntity)), (p) -> {
						p.getGdxParticleEffect().setTint(message.leafColor);
					}));
				}
				return true;
			}
			case MessageType.ENTITY_ASSET_UPDATE_REQUIRED: {
				Entity entity = (Entity) msg.extraInfo;
				if (entity != null) {
					entityAssetUpdater.updateEntityAssets(entity);
				}
				return true;
			}
			case MessageType.REQUEST_DOOR_OPEN: {
				EntityMessage entityMessage = (EntityMessage)msg.extraInfo;
				Entity doorEntity = entityStore.getById(entityMessage.getEntityId());
				DoorBehaviour doorBehaviour = (DoorBehaviour) doorEntity.getBehaviourComponent();
				doorBehaviour.doorOpenRequested();
				return true;
			}
			case MessageType.REQUEST_FURNITURE_REMOVAL: {
				Entity entity = (Entity) msg.extraInfo;
				ConstructedEntityComponent constructedEntityComponent = entity.getComponent(ConstructedEntityComponent.class);
				MapTile entityTile = gameContext.getAreaMap().getTile(entity.getLocationComponent().getWorldPosition());
				if (entityTile != null) {
					if (constructedEntityComponent.isAutoConstructed()) {
						// FIXME This and its shared usage would be better dealt with by a ACTUALLY_DO_THE_DECONSTRUCT type message
						deconstructFurniture(entity, entityTile, messageDispatcher, gameContext, itemTypeDictionary, itemEntityAttributesFactory, itemEntityFactory,
								deconstructParticleEffect);
					} else if (!constructedEntityComponent.isBeingDeconstructed()){
						Job deconstructionJob = jobFactory.deconstructionJob(entityTile);
						if (deconstructionJob != null) {
							constructedEntityComponent.setDeconstructionJob(deconstructionJob);
							messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, deconstructionJob);

							// also apply designation to other tiles
							TileDesignation deconstructDesignation = GameInteractionMode.DECONSTRUCT.getDesignationToApply();
							if (deconstructDesignation != null) {
								Set<MapTile> locations = new HashSet<>();
								locations.add(entityTile);
								for (GridPoint2 extraOffset : ((FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getCurrentLayout().getExtraTiles()) {
									locations.add(gameContext.getAreaMap().getTile(entityTile.getTilePosition().cpy().add(extraOffset)));
								}


								for (MapTile location : locations) {
									if (location.getDesignation() == null) {
										location.setDesignation(deconstructDesignation);
									}
								}
							}
						}
					}
				}
				return true;
			}
			case MessageType.CHANGE_PROFESSION:
				return handle((ChangeProfessionMessage)msg.extraInfo);
			case MessageType.HAULING_ALLOCATION_CANCELLED: {
				HaulingAllocation allocation = (HaulingAllocation) msg.extraInfo;

				if (allocation.getLiquidAllocation() != null) {
					messageDispatcher.dispatchMessage(MessageType.LIQUID_ALLOCATION_CANCELLED, allocation.getLiquidAllocation());
				}

				if (allocation.getHauledEntityType().equals(HUMANOID)) {
					// Probably assigned to a piece of furniture somewhere
					if (allocation.getTargetPositionType().equals(HaulingAllocation.AllocationPositionType.FURNITURE)) {
						Entity targetFurniture = entityStore.getById(allocation.getTargetId());
						if (targetFurniture == null) {
							Logger.error("Could not find target furniture of cancelled hauling allocation for type " + HUMANOID);
						} else {
							((FurnitureEntityAttributes)targetFurniture.getPhysicalEntityComponent().getAttributes()).setAssignedToEntityId(null);
						}
					}
					return true;
				} else if (allocation.getHauledEntityType().equals(ITEM)) {
					if (allocation.getItemAllocation() == null) {
						Logger.warn("Item Hauling allocation does not have an item allocation");
						return true;
					}

					Entity targetItemEntity = entityStore.getById(allocation.getHauledEntityId());
					if (targetItemEntity == null) {
						// Entity must have been destroyed already
						return true;
					}
					if (!allocation.getItemAllocation().getState().equals(CANCELLED) &&
						!allocation.getItemAllocation().getPurpose().equals(HAULING) &&
						!allocation.getItemAllocation().getPurpose().equals(HELD_IN_INVENTORY)) {
						ItemAllocationComponent itemAllocationComponent = targetItemEntity.getOrCreateComponent(ItemAllocationComponent.class);
						itemAllocationComponent.cancel(allocation.getItemAllocation());
					}

					if (ROOM.equals(allocation.getTargetPositionType())) {
						Room targetRoom = roomStore.getById(allocation.getTargetId());
						if (targetRoom != null && targetRoom.getComponent(StockpileComponent.class) != null) {
							targetRoom.getComponent(StockpileComponent.class).allocationCancelled(allocation, targetItemEntity);
						}
						return true;
					} else if (CONSTRUCTION.equals(allocation.getTargetPositionType())) {
						Construction targetConstruction = gameContext.getConstructions().get(allocation.getTargetId());
						if (targetConstruction != null) {
							targetConstruction.allocationCancelled(allocation);
						}
						return true; // This is handled by ConstructionMessageHandler
					} else if (allocation.getTargetPosition() == null) {
						// Not hauling to anywhere in particular so allocation cancelled message doesn't matter
						return true;
					} else if (FURNITURE.equals(allocation.getTargetPositionType())) {
						Entity targetFurnitureEntity = entityStore.getById(allocation.getTargetId());
						if (targetFurnitureEntity != null && targetFurnitureEntity.getBehaviourComponent() instanceof CraftingStationBehaviour) {
							((CraftingStationBehaviour) targetFurnitureEntity.getBehaviourComponent()).allocationCancelled(allocation);
						} else if (targetFurnitureEntity != null && targetFurnitureEntity.getBehaviourComponent() instanceof CollectItemFurnitureBehaviour ||
								targetFurnitureEntity != null && targetFurnitureEntity.getBehaviourComponent() instanceof InnoculationLogBehaviour) {
							// Do nothing, CollectItemFurnitureBehaviour will deal with cancelled allocations, eventually, might want to improve this
						} else {
							// FIXME perhaps this is fine and we can do nothing
							// Currently this could be a target of a cooking cauldron or baked bread, which KitchenBehaviour would deal with
							Logger.error("Unrecognised item allocation cancelled with target of furniture");
						}
						return true;
					} else if (ZONE.equals(allocation.getTargetPositionType())) {
						// Hauling to zone doesn't matter about cancelling
						return true;
					} else {
						Logger.error("HAULING_ALLOCATION_CANCELLED message received with unrecognised targetPositionType");
						return false;
					}
				} else {
					// FURNITURE-type hauling handled elsewhere e.g. KitchenManager
					return false;
				}
			}
			case MessageType.APPLY_STATUS: {
				StatusMessage message = (StatusMessage) msg.extraInfo;
				try {
					StatusEffect statusEffect = message.statusClass.getDeclaredConstructor().newInstance();
					if (statusEffect instanceof Death) {
						((Death)statusEffect).setDeathReason(message.deathReason);
					}
					message.entity.getComponent(StatusComponent.class).apply(statusEffect);
				} catch (ReflectiveOperationException e) {
					Logger.error("Could not instantiate " + message.statusClass.getSimpleName() + " with expected constructor");
				}
				return true;
			}
			case MessageType.REMOVE_STATUS: {
				StatusMessage message = (StatusMessage) msg.extraInfo;
				message.entity.getComponent(StatusComponent.class).remove(message.statusClass);
				return true;
			}
			case MessageType.TRANSFORM_FURNITURE_TYPE: {
				return handle((TransformFurnitureMessage)msg.extraInfo);
			}
			case MessageType.TRANSFORM_ITEM_TYPE: {
				return handle((TransformItemMessage)msg.extraInfo);
			}
			case MessageType.HUMANOID_DEATH: {
				return handle((HumanoidDeathMessage) msg.extraInfo);
			}
			case MessageType.HUMANOID_INSANITY: {
				return handleInsanity((Entity) msg.extraInfo);
			}
			case MessageType.LIQUID_SPLASH: {
				return handleLiquidSplash((LiquidSplashMessage) msg.extraInfo);
			}
			case MessageType.FURNITURE_IN_USE: {
				Entity furnitureEntity = (Entity)msg.extraInfo;
				FurnitureParticleEffectsComponent particleEffectsComponent = furnitureEntity.getComponent(FurnitureParticleEffectsComponent.class);
				if (particleEffectsComponent != null) {
					Optional<JobTarget> targetItem = Optional.empty();
					InventoryComponent inventoryComponent = furnitureEntity.getComponent(InventoryComponent.class);
					if (inventoryComponent != null && !inventoryComponent.isEmpty()) {
						InventoryComponent.InventoryEntry entry = inventoryComponent.getInventoryEntries().stream().findFirst().get();
						targetItem = Optional.of(new JobTarget(entry.entity));
					}

					for (ParticleEffectType particleEffectType : particleEffectsComponent.getParticleEffectsWhenInUse()) {
						messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(particleEffectType,
								Optional.of(furnitureEntity),
								targetItem,
								particleEffectsComponent.getCurrentParticleInstances()::add));
					}
				}
				return true;
			}
			case MessageType.FURNITURE_NO_LONGER_IN_USE: {
				Entity furnitureEntity = (Entity)msg.extraInfo;
				FurnitureParticleEffectsComponent particleEffectsComponent = furnitureEntity.getComponent(FurnitureParticleEffectsComponent.class);
				if (particleEffectsComponent != null) {
					for (ParticleEffectInstance particleInstance : particleEffectsComponent.getCurrentParticleInstances()) {
						messageDispatcher.dispatchMessage(MessageType.PARTICLE_RELEASE, particleInstance);
					}
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private boolean handle(ChangeProfessionMessage changeProfessionMessage) {
		if (changeProfessionMessage.entity == null || changeProfessionMessage.entity.getComponent(ProfessionsComponent.class) == null) {
			throw new RuntimeException("No entity in " + changeProfessionMessage.getClass().getSimpleName() + " handled in " + this.getClass().getSimpleName());
		}

		ProfessionsComponent professionsComponent = changeProfessionMessage.entity.getComponent(ProfessionsComponent.class);

		if (changeProfessionMessage.professionToReplace != null && !changeProfessionMessage.professionToReplace.equals(NULL_PROFESSION)) {
			professionsComponent.deactivate(changeProfessionMessage.professionToReplace);
		}

		if (!changeProfessionMessage.newProfession.getName().equals("VILLAGER")) {
			if (professionsComponent.hasInactiveProfession(changeProfessionMessage.newProfession)) {
				professionsComponent.activate(changeProfessionMessage.newProfession);
			} else {
				professionsComponent.add(changeProfessionMessage.newProfession, 0.23f);
			}
		}

		// Remove any attached light sources so changing between mining helmet and not does not leave a rogue lightsource
		changeProfessionMessage.entity.removeComponent(AttachedLightSourceComponent.class);

		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, changeProfessionMessage.entity);
		return true;
	}

	private boolean handle(TreeFallenMessage treeFallenMessage) {
		GridPoint2 treeTilePosition = new GridPoint2(
				(int)Math.floor(treeFallenMessage.getTreeWorldPosition().x),
				(int)Math.floor(treeFallenMessage.getTreeWorldPosition().y)
		);

		messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(treeFallSoundEffect, -1L,
				toVector(treeTilePosition)));


		for (PlantSpeciesItem itemToCreate : treeFallenMessage.getItemsToCreate()) {
			int logsToCreateAtNextTile = 1;
			int logsLeftToCreate = itemToCreate.getQuantity();

			for (int xOffset = logsLeftToCreate; xOffset > 0; xOffset--) {
				GridPoint2 targetTilePosition;
				if (treeFallenMessage.isFallToWest()) {
					targetTilePosition = treeTilePosition.cpy().sub(xOffset, 0);
				} else {
					targetTilePosition = treeTilePosition.cpy().add(xOffset, 0);
				}

				MapTile targetTile = gameContext.getAreaMap().getTile(targetTilePosition);
				if (targetTile != null && targetTile.isNavigable()) {

					if (treeFallenMessage.getLeafColor().isPresent()) {
						messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(leafExplosionParticleType,
								Optional.empty(), Optional.of(new JobTarget(targetTile)), (p) -> {
							p.getGdxParticleEffect().setTint(treeFallenMessage.getLeafColor().get());
						}));
					}
					if (treeFallenMessage.getBranchColor() != null) {
						messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(chipExplosionParticleType,
								Optional.empty(), Optional.of(new JobTarget(targetTile)), (p) -> {
							p.getGdxParticleEffect().setTint(treeFallenMessage.getBranchColor());
						}));
					}

					ItemEntityAttributes itemEntityAttributes = new ItemEntityAttributes(gameContext.getRandom().nextLong());
					itemEntityAttributes.setItemSize(itemToCreate.getItemSize());
					itemEntityAttributes.setItemStyle(itemToCreate.getItemStyle());
					itemEntityAttributes.setColor(ColoringLayer.BRANCHES_COLOR, treeFallenMessage.getBranchColor());
					itemEntityAttributes.setQuantity(logsToCreateAtNextTile);
					itemEntityAttributes.setMaterial(itemToCreate.getMaterial());
					itemEntityAttributes.setItemType(itemToCreate.getItemType());


					Entity matchingItem = targetTile.getItemMatching(itemEntityAttributes);
					if (matchingItem == null && targetTile.hasItem()) {
						// There's a different kind of item here
					} else {
						// Else the item matches or the target doesn't have an item
						entityStore.createResourceItem(itemEntityAttributes, targetTilePosition);
						logsToCreateAtNextTile = 0;
					}
				}

				logsToCreateAtNextTile++;
			}

		}
		return true;
	}

	private boolean handle(TransformFurnitureMessage transformFurnitureMessage) {
		entityStore.remove(transformFurnitureMessage.furnitureEntity.getId(), true);
		furnitureTracker.furnitureRemoved(transformFurnitureMessage.furnitureEntity);

		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) transformFurnitureMessage.furnitureEntity.getPhysicalEntityComponent().getAttributes();
		FurnitureLayout originalLayout = attributes.getCurrentLayout();
		attributes.setFurnitureType(transformFurnitureMessage.transformToFurnitureType);
		if (!attributes.getCurrentLayout().equals(originalLayout)) {
			for (int i = 0; i < 4; i++) {
				attributes.setCurrentLayout(attributes.getCurrentLayout().getRotatesTo());
				if (attributes.getCurrentLayout().equals(originalLayout)) {
					break;
				}
			}
		}

		if (!transformFurnitureMessage.transformToFurnitureType.getRequirements().containsKey(attributes.getPrimaryMaterialType())) {
			for (GameMaterialType materialType : transformFurnitureMessage.transformToFurnitureType.getRequirements().keySet()) {
				if (attributes.getMaterials().get(materialType) != null) {
					attributes.setPrimaryMaterialType(materialType);
					break;
				}
			}
		}

		// Reset behaviour component
		BehaviourComponent oldBehaviour = transformFurnitureMessage.furnitureEntity.getBehaviourComponent();
		if (oldBehaviour instanceof Destructible) {
			((Destructible)oldBehaviour).destroy(transformFurnitureMessage.furnitureEntity, messageDispatcher, gameContext);
		}
		FurnitureBehaviour newBehaviour = new FurnitureBehaviour();
		newBehaviour.init(transformFurnitureMessage.furnitureEntity, messageDispatcher, gameContext);
		transformFurnitureMessage.furnitureEntity.replaceBehaviourComponent(newBehaviour);

		// Also re-applies any tags e.g. behaviour type
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, transformFurnitureMessage.furnitureEntity);

		if (oldBehaviour instanceof Prioritisable && transformFurnitureMessage.furnitureEntity.getBehaviourComponent() instanceof Prioritisable) {
			((Prioritisable)transformFurnitureMessage.furnitureEntity.getBehaviourComponent()).setPriority(((Prioritisable)oldBehaviour).getPriority());
		}

		entityStore.add(transformFurnitureMessage.furnitureEntity);
		furnitureTracker.furnitureAdded(transformFurnitureMessage.furnitureEntity);
		return true;
	}

	private boolean handle(TransformItemMessage transformItemMessage) {
		itemTracker.itemRemoved(transformItemMessage.itemEntity);

		ItemEntityAttributes attributes = (ItemEntityAttributes) transformItemMessage.itemEntity.getPhysicalEntityComponent().getAttributes();
		attributes.setItemType(transformItemMessage.transformToItemType);

		// Also re-applies any tags
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, transformItemMessage.itemEntity);

		itemTracker.itemAdded(transformItemMessage.itemEntity);
		return true;
	}

	private boolean handle(HumanoidDeathMessage deathMessage) {
		Entity deceased = deathMessage.deceased;
		HumanoidEntityAttributes attributes = (HumanoidEntityAttributes) deceased.getPhysicalEntityComponent().getAttributes();
		if (attributes.getConsciousness().equals(DEAD)) {
			// Already dead! Doesn't need killing again
			return true;
		}

		attributes.setConsciousness(DEAD);
		CorpseBehaviour corpseBehaviour = new CorpseBehaviour();
		corpseBehaviour.setOriginalSkinColor(attributes.getColor(SKIN_COLOR));
		entityStore.changeBehaviour(deceased, corpseBehaviour, messageDispatcher);

		Vector2 deceasedPosition = deceased.getLocationComponent().getWorldOrParentPosition();
		Notification deathNotification = new Notification(NotificationType.DEATH, deceasedPosition);
		deathNotification.addTextReplacement("character", i18nTranslator.getDescription(deceased));
		deathNotification.addTextReplacement("reason", i18nTranslator.getTranslatedString(deathMessage.reason.getI18nKey()));
		messageDispatcher.dispatchMessage(MessageType.POST_NOTIFICATION, deathNotification);

		settlerTracker.settlerDied(deceased);

		dropEquippedItems(deceased, deceasedPosition);
		deceased.removeComponent(NeedsComponent.class);

		HistoryComponent historyComponent = deceased.getOrCreateComponent(HistoryComponent.class);
		historyComponent.setDeathReason(deathMessage.reason);

		// Rotate and change orientation of deceased
		changeToConsciousnessOnFloor(deceased, DEAD, gameContext, messageDispatcher);

		// TODO check for game-over state
		boolean allDead = true;
		for (Entity settler : settlerTracker.getAll()) {
			HumanoidEntityAttributes otherSettlerAttributes = (HumanoidEntityAttributes) settler.getPhysicalEntityComponent().getAttributes();
			if (!otherSettlerAttributes.getConsciousness().equals(DEAD)) {
				allDead = false;
				break;
			}
		}

		if (allDead) {
			Notification gameOverNotification = new Notification(NotificationType.GAME_OVER, null);
			messageDispatcher.dispatchMessage(MessageType.POST_NOTIFICATION, gameOverNotification);
			gameContext.getSettlementState().setGameOver(true);
		}


		return true;
	}

	private boolean handleInsanity(Entity entity) {
		BehaviourComponent currentBehaviour = entity.getBehaviourComponent();
		if (currentBehaviour instanceof Destructible) {
			((Destructible)currentBehaviour).destroy(entity, messageDispatcher, gameContext);
		}

		BrokenDwarfBehaviour brokenDwarfBehaviour = new BrokenDwarfBehaviour();
		entityStore.changeBehaviour(entity, brokenDwarfBehaviour, messageDispatcher);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entity);

		Vector2 entityPosition = entity.getLocationComponent().getWorldOrParentPosition();
		dropEquippedItems(entity, entityPosition);

		Notification brokenNotification = new Notification(NotificationType.SETTLER_MENTAL_BREAK, entityPosition);
		brokenNotification.addTextReplacement("character", i18nTranslator.getDescription(entity));
		messageDispatcher.dispatchMessage(MessageType.POST_NOTIFICATION, brokenNotification);

		return true;
	}

	private boolean handleLiquidSplash(LiquidSplashMessage message) {
		if (message.targetEntity != null && message.liquidMaterial != null) {
			messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(liquidSplashEffect,
					Optional.of(message.targetEntity), Optional.empty(), (p) -> {
				p.getGdxParticleEffect().setTint(message.liquidMaterial.getColor());
			}));
		}
		return true;
	}

	private void dropEquippedItems(Entity entity, Vector2 entityPosition) {
		HaulingComponent haulingComponent = entity.getComponent(HaulingComponent.class);
		if (haulingComponent != null && haulingComponent.getHauledEntity() != null) {
			Entity hauledEntity = haulingComponent.getHauledEntity();
			haulingComponent.clearHauledEntity();
			placeOnGround(hauledEntity, entityPosition);
			entity.removeComponent(HaulingComponent.class);
		}
		EquippedItemComponent equippedItemComponent = entity.getComponent(EquippedItemComponent.class);
		if (equippedItemComponent != null && equippedItemComponent.getEquippedItem() != null) {
			Entity equippedItem = equippedItemComponent.getEquippedItem();
			equippedItemComponent.clearEquippedItem();
			placeOnGround(equippedItem, entityPosition);
			entity.removeComponent(EquippedItemComponent.class);
		}
	}

	private void placeOnGround(Entity hauledEntity, Vector2 position) {
		if (hauledEntity.getType().equals(ITEM)) {
			ItemEntityAttributes itemAttributes = (ItemEntityAttributes) hauledEntity.getPhysicalEntityComponent().getAttributes();
			itemAttributes.setItemPlacement(ItemPlacement.ON_GROUND);
		}

		hauledEntity.getLocationComponent().setWorldPosition(position, false);
		hauledEntity.getLocationComponent().setFacing(DOWN.toVector2());
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, hauledEntity);
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
