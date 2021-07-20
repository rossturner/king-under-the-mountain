package technology.rocketjump.undermount.cooking;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.constants.ConstantsRepo;
import technology.rocketjump.undermount.entities.behaviour.furniture.CollectItemFurnitureBehaviour;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.ItemAllocation;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.components.LiquidContainerComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.undermount.entities.tags.CollectItemsBehaviourTag;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.jobs.JobTypeDictionary;
import technology.rocketjump.undermount.jobs.ProfessionDictionary;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.jobs.model.Profession;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.CookingCompleteMessage;
import technology.rocketjump.undermount.messaging.types.EntityMessage;
import technology.rocketjump.undermount.rooms.HaulingAllocation;
import technology.rocketjump.undermount.rooms.constructions.Construction;
import technology.rocketjump.undermount.rooms.constructions.ConstructionStore;
import technology.rocketjump.undermount.settlement.FurnitureTracker;
import technology.rocketjump.undermount.settlement.ItemTracker;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static technology.rocketjump.undermount.entities.EntityUpdater.TIME_BETWEEN_INFREQUENT_UPDATE_SECONDS;
import static technology.rocketjump.undermount.entities.behaviour.furniture.CraftingStationBehaviour.getAnyNavigableWorkspace;
import static technology.rocketjump.undermount.entities.components.ItemAllocation.Purpose.DUE_TO_BE_HAULED;
import static technology.rocketjump.undermount.entities.model.EntityType.ITEM;
import static technology.rocketjump.undermount.entities.tags.ConstructionOverrideTag.ConstructionOverrideSetting.REQUIRES_EDIBLE_LIQUID;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.rooms.HaulingAllocation.AllocationPositionType.CONSTRUCTION;
import static technology.rocketjump.undermount.rooms.HaulingAllocation.AllocationPositionType.FLOOR;
import static technology.rocketjump.undermount.rooms.constructions.ConstructionState.SELECTING_MATERIALS;
import static technology.rocketjump.undermount.rooms.constructions.ConstructionState.WAITING_FOR_COMPLETION;

/**
 * This class is responsible for moving cooked food (currently only in cauldrons) from any kitchen
 * To any construction site that requires an item to contain edible contents
 */
@Singleton
public class KitchenManager implements Telegraph, Updatable {

	private final MessageDispatcher messageDispatcher;
	private final ConstructionStore constructionStore;
	private final Profession cookingProfession;
	private final FurnitureTracker furnitureTracker;
	private final JobType haulingJobType;
	private final ItemTracker itemTracker;

	private GameContext gameContext;
	private float timeSinceLastUpdate = 0f;


	@Inject
	public KitchenManager(MessageDispatcher messageDispatcher, ConstructionStore constructionStore,
						  ProfessionDictionary professionDictionary, FurnitureTracker furnitureTracker,
						  JobTypeDictionary jobTypeDictionary, ConstantsRepo constantsRepo, ItemTracker itemTracker) {
		this.messageDispatcher = messageDispatcher;
		this.constructionStore = constructionStore;
		this.itemTracker = itemTracker;
		this.cookingProfession = professionDictionary.getByName(constantsRepo.getSettlementConstants().getKitchenProfession());
		this.furnitureTracker = furnitureTracker;
		this.haulingJobType = jobTypeDictionary.getByName(constantsRepo.getSettlementConstants().getHaulingJobType());

		messageDispatcher.addListener(this, MessageType.COOKING_COMPLETE);
		messageDispatcher.addListener(this, MessageType.DESTROY_ENTITY);
		messageDispatcher.addListener(this, MessageType.HAULING_ALLOCATION_CANCELLED);
	}

	@Override
	public void update(float deltaTime) {
		timeSinceLastUpdate += deltaTime;
		if (gameContext != null && timeSinceLastUpdate > TIME_BETWEEN_INFREQUENT_UPDATE_SECONDS) {
			timeSinceLastUpdate = 0f;

			// Look for unallocated constructions of REQUIRES_EDIBLE_LIQUID tagged furniture
			for (Construction construction : constructionStore.getAll()) {
				// Only those in SELECTING_MATERIALS state have not yet been allocated to
				if (construction.getConstructionOverrideSettings().contains(REQUIRES_EDIBLE_LIQUID) && construction.getState().equals(SELECTING_MATERIALS)) {
					// FIXME this assumes that a REQUIRES_EDIBLE_LIQUID construction only has a construction requirement of a single entity/item
					Entity matchingEntity = getMatchingInput(construction.getPrimaryMaterialType(), construction.getRequirements());
					if (matchingEntity != null) {
						// found a match

						Job haulingJob = createHaulingJob(matchingEntity, construction);
						if (haulingJob != null) {
							construction.getIncomingHaulingAllocations().add(haulingJob.getHaulingAllocation()); // To track when allocation is cancelled
							messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJob);

							// TODO would all of this be better served by adding a new FurnitureBehaviour to the cauldron containing soup?

							gameContext.getSettlementState().furnitureHoldingCompletedCooking.remove(matchingEntity.getId());
							construction.setState(WAITING_FOR_COMPLETION);
						}
					}
				}
			}

			List<Entity> furnitureWithItemsToMove = new LinkedList<>();
			for (Entity furnitureHoldingCooking : gameContext.getSettlementState().furnitureHoldingCompletedCooking.values()) {
				InventoryComponent inventoryComponent = furnitureHoldingCooking.getComponent(InventoryComponent.class);
				if (!inventoryComponent.isEmpty()) {
					InventoryComponent.InventoryEntry inventoryEntry = inventoryComponent.getInventoryEntries().iterator().next();
					ItemAllocationComponent itemAllocationComponent = inventoryEntry.entity.getOrCreateComponent(ItemAllocationComponent.class);
					if (itemAllocationComponent.getNumUnallocated() > 0) {
						furnitureWithItemsToMove.add(furnitureHoldingCooking);
					}
				}
			}
			// Help along furniture with CollectItemsBehaviour to move completed cooking out
			setupHaulingToCollectionFurniture(furnitureWithItemsToMove);

		}
	}

	private void setupHaulingToCollectionFurniture(List<Entity> furnitureWithItemsToMove) {
		for (Entity collectItemsFurniture : furnitureTracker.findByTag(CollectItemsBehaviourTag.class, false)) {
			if (collectItemsFurniture.getBehaviourComponent() instanceof CollectItemFurnitureBehaviour) {
				CollectItemFurnitureBehaviour behaviour = (CollectItemFurnitureBehaviour) collectItemsFurniture.getBehaviourComponent();

				for (Entity cookingFurniture : new ArrayList<>(furnitureWithItemsToMove)) {
					InventoryComponent inventoryComponent = cookingFurniture.getComponent(InventoryComponent.class);
					InventoryComponent.InventoryEntry inventoryEntry = inventoryComponent.getInventoryEntries().iterator().next();

					if (behaviour.canAccept(inventoryEntry.entity)) {


						ItemEntityAttributes attributes = (ItemEntityAttributes) inventoryEntry.entity.getPhysicalEntityComponent().getAttributes();
						ItemAllocationComponent itemAllocationComponent = inventoryEntry.entity.getOrCreateComponent(ItemAllocationComponent.class);

						HaulingAllocation allocation = new HaulingAllocation();

						allocation.setSourceContainerId(cookingFurniture.getId());
						allocation.setSourcePositionType(HaulingAllocation.AllocationPositionType.FURNITURE);
						allocation.setSourcePosition(toGridPoint(cookingFurniture.getLocationComponent().getWorldPosition()));

						int numToAllocate = Math.min(itemAllocationComponent.getNumUnallocated(), attributes.getItemType().getMaxHauledAtOnce());
						ItemAllocation itemAllocation = itemAllocationComponent.createAllocation(numToAllocate, cookingFurniture, ItemAllocation.Purpose.HELD_IN_INVENTORY);
						allocation.setItemAllocation(itemAllocation);

						behaviour.finaliseAllocation(behaviour.getMatch(attributes), allocation);
						furnitureWithItemsToMove.remove(cookingFurniture);
					}
				}
			}
		}
	}

	private Job createHaulingJob(Entity matchingEntity, Construction construction) {
		Job haulingJob = new Job(haulingJobType);
		haulingJob.setJobPriority(construction.getPriority());
		haulingJob.setTargetId(matchingEntity.getId());
		haulingJob.setRequiredProfession(cookingProfession);

		HaulingAllocation haulingAllocation = new HaulingAllocation();
		haulingJob.setHaulingAllocation(haulingAllocation);

		haulingAllocation.setSourcePosition(toGridPoint(matchingEntity.getLocationComponent().getWorldOrParentPosition()));
		if (matchingEntity.getType().equals(EntityType.FURNITURE)) {
			haulingAllocation.setSourcePositionType(HaulingAllocation.AllocationPositionType.FURNITURE);
			haulingAllocation.setHauledEntityType(EntityType.FURNITURE);

			FurnitureLayout.Workspace navigableWorkspace = getAnyNavigableWorkspace(matchingEntity, gameContext.getAreaMap());
			if (navigableWorkspace == null) {
				Logger.warn("Could not find navigable workspace of " + matchingEntity.getPhysicalEntityComponent().getAttributes().toString());
				return null;
			} else {
				haulingJob.setJobLocation(navigableWorkspace.getAccessedFrom());
			}
		} else if (matchingEntity.getType().equals(ITEM)) {
			if (matchingEntity.getLocationComponent().getContainerEntity() != null) {
				Logger.error("Not yet implemented, hauling from container in " + this.getClass().getSimpleName());
				return null;
			} else {
				haulingAllocation.setSourcePositionType(FLOOR);
				haulingAllocation.setHauledEntityType(ITEM);
				ItemAllocationComponent itemAllocationComponent = matchingEntity.getComponent(ItemAllocationComponent.class);
				ItemAllocation itemAllocation = itemAllocationComponent.createAllocation(itemAllocationComponent.getNumUnallocated(), construction.getEntity(), DUE_TO_BE_HAULED);
				if (itemAllocation == null) {
					Logger.error("Could not allocate item in " + this.getClass().getSimpleName());
					return null;
				} else {
					haulingAllocation.setItemAllocation(itemAllocation);
				}
				haulingJob.setJobLocation(toGridPoint(matchingEntity.getLocationComponent().getWorldOrParentPosition()));
			}
		} else {
			Logger.error("Unrecognised entity type in " + this.getClass().getSimpleName());
			return null;
		}
		haulingAllocation.setHauledEntityId(matchingEntity.getId());

		haulingAllocation.setTargetPosition(construction.getPrimaryLocation());
		haulingAllocation.setTargetId(construction.getId());
		haulingAllocation.setTargetPositionType(CONSTRUCTION);

		return haulingJob;
	}

	/**
	 * This only handles REQUIRES_EDIBLE_LIQUID for now
	 */
	private Entity getMatchingInput(GameMaterialType primaryMaterialType, List<QuantifiedItemTypeWithMaterial> constructionRequirements) {
		if (constructionRequirements.size() != 1) {
			Logger.error("Not expecting list of requirements with 0 or more than 1 item");
			return null;
		}
		QuantifiedItemTypeWithMaterial constructionRequirement = constructionRequirements.get(0);

		for (Entity furnitureEntity : gameContext.getSettlementState().furnitureHoldingCompletedCooking.values()) {
			LiquidContainerComponent liquidContainerComponent = furnitureEntity.getComponent(LiquidContainerComponent.class);
			if (liquidContainerComponent != null && liquidContainerComponent.getTargetLiquidMaterial() != null) {
				GameMaterial liquidMaterial = liquidContainerComponent.getTargetLiquidMaterial();
				if (liquidMaterial.isEdible()) {

					FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();
					List<QuantifiedItemType> furnitureRequirements = attributes.getFurnitureType().getRequirements().get(primaryMaterialType);
					if (furnitureRequirements != null) {
						if (furnitureRequirements.size() != 1) {
							Logger.error("Not expecting list of requirements with 0 or more than 1 item");
						} else {
							QuantifiedItemType furnitureRequirement = furnitureRequirements.get(0);

							if (constructionRequirement.getItemType().equals(furnitureRequirement.getItemType())) {
								// Everything matches, good to go
								return furnitureEntity;
							}
						}
					}  // else furniture is of wrong material type

				}
			}
		}

		// Try any lost items containing edible liquid
		for (Entity itemEntity : itemTracker.getItemsByType(constructionRequirement.getItemType(), true)) {
			LiquidContainerComponent liquidContainerComponent = itemEntity.getComponent(LiquidContainerComponent.class);
			if (liquidContainerComponent != null && liquidContainerComponent.getTargetLiquidMaterial() != null) {
				if (liquidContainerComponent.getTargetLiquidMaterial().isEdible() && liquidContainerComponent.getNumUnallocated() > 0) {
					return itemEntity;
				}
			}
		}

		return null;
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.COOKING_COMPLETE: {
				CookingCompleteMessage cookingCompleteMessage = (CookingCompleteMessage) msg.extraInfo;
				gameContext.getSettlementState().furnitureHoldingCompletedCooking.put(cookingCompleteMessage.targetFurnitureEntity.getId(), cookingCompleteMessage.targetFurnitureEntity);
				return true;
			}
			case MessageType.DESTROY_ENTITY: {
				EntityMessage entityMessage = (EntityMessage) msg.extraInfo;
				gameContext.getSettlementState().furnitureHoldingCompletedCooking.remove(entityMessage.getEntityId());
				return false; // Not the primary handler for this message type
			}
			case MessageType.HAULING_ALLOCATION_CANCELLED: {
				HaulingAllocation allocation = (HaulingAllocation) msg.extraInfo;
				if (allocation.getHauledEntityType().equals(EntityType.FURNITURE)) {
					Entity hauledEntity = gameContext.getEntities().get(allocation.getHauledEntityId());
					if (hauledEntity != null) {
						LiquidContainerComponent liquidContainerComponent = hauledEntity.getComponent(LiquidContainerComponent.class);
						if (liquidContainerComponent != null && liquidContainerComponent.getTargetLiquidMaterial() != null
								&& liquidContainerComponent.getTargetLiquidMaterial().isEdible()) {
							// If this is a liquid container holding an edible liquid, it was probably some cooked soup to be hauled
							if (hauledEntity.getType().equals(EntityType.FURNITURE)) {
								gameContext.getSettlementState().furnitureHoldingCompletedCooking.put(hauledEntity.getId(), hauledEntity);
							} else {
								Logger.warn("Hauling of item with completed cooking cancelled, currently this will be lost and not put back in furnitureHoldingCompletedCooking");
							}
							return true;
						}
					}
				}
				return false; // Not the primary handler for this message type
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;

		// Nothing to do, just look in gameContext.settlementState.furnitureHoldingCompletedCooking
	}

	@Override
	public void clearContextRelatedState() {
		gameContext = null;
		timeSinceLastUpdate = 0f;
	}
}
