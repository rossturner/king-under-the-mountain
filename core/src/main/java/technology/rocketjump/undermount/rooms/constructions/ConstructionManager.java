package technology.rocketjump.undermount.rooms.constructions;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.NotImplementedException;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.components.ItemAllocation;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.jobs.JobFactory;
import technology.rocketjump.undermount.jobs.JobTypeDictionary;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobState;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.ItemMaterialSelectionMessage;
import technology.rocketjump.undermount.messaging.types.RequestHaulingAllocationMessage;
import technology.rocketjump.undermount.messaging.types.RequestHaulingMessage;
import technology.rocketjump.undermount.messaging.types.RequestPlantRemovalMessage;
import technology.rocketjump.undermount.rooms.HaulingAllocation;
import technology.rocketjump.undermount.settlement.ItemTracker;

import java.util.ArrayList;
import java.util.List;

import static technology.rocketjump.undermount.entities.ItemEntityMessageHandler.createHaulingJob;
import static technology.rocketjump.undermount.entities.components.ItemAllocation.Purpose.ON_FIRE;
import static technology.rocketjump.undermount.entities.components.ItemAllocation.Purpose.PLACED_FOR_CONSTRUCTION;
import static technology.rocketjump.undermount.entities.tags.ConstructionOverrideTag.ConstructionOverrideSetting.DO_NOT_ALLOCATE;
import static technology.rocketjump.undermount.misc.VectorUtils.toVector;
import static technology.rocketjump.undermount.rooms.HaulingAllocation.AllocationPositionType.CONSTRUCTION;
import static technology.rocketjump.undermount.rooms.constructions.ConstructionState.*;

@Singleton
public class ConstructionManager implements Updatable {

	private final ConstructionStore constructionStore;
	private final ItemTracker itemTracker;
	private final MessageDispatcher messageDispatcher;
	private final JobFactory jobFactory;
	private final JobType haulingJobType;
	private final JobType constructWoodenFurnitureJobType;
	private final JobType constructStoneFurnitureJobType;
	private GameContext gameContext;

	@Inject
	public ConstructionManager(ConstructionStore constructionStore, ItemTracker itemTracker, MessageDispatcher messageDispatcher,
							   JobFactory jobFactory, JobTypeDictionary jobTypeDictionary) {
		this.constructionStore = constructionStore;
		this.itemTracker = itemTracker;
		this.messageDispatcher = messageDispatcher;
		this.jobFactory = jobFactory;
		this.haulingJobType = jobTypeDictionary.getByName("HAULING");
		this.constructWoodenFurnitureJobType = jobTypeDictionary.getByName("CONSTRUCT_WOODEN_FURNITURE");
		this.constructStoneFurnitureJobType = jobTypeDictionary.getByName("CONSTRUCT_STONE_FURNITURE");
	}

	@Override
	public void update(float deltaTime) {
		if (gameContext != null) {
			Construction construction = constructionStore.next();
			if (construction != null) {
				update(construction);
			}
		}
	}

	private void update(Construction construction) {
		if (otherItemsNeedRemoving(construction)) {
			construction.setState(CLEARING_WORK_SITE);
			return;
		}

		if (construction.getConstructionOverrideSettings().contains(DO_NOT_ALLOCATE)) {
			if (construction.getRequirements().isEmpty()) {
				// Special case for graves
				construction.setState(WAITING_FOR_COMPLETION);
				return;
			}

			if (construction.getState().equals(WAITING_FOR_COMPLETION)) {
				if (construction.getIncomingHaulingAllocations().isEmpty()) {
					// Hauling allocation must have been cancelled
					construction.setState(SELECTING_MATERIALS);
				} // else do not reset construction state to SELECTING_MATERIALS
			} else {
				construction.setState(SELECTING_MATERIALS);
			}
			completeConstructionIfAllRequirementsInPlace(construction);
			return;
		}


		if (construction.getState().equals(CLEARING_WORK_SITE) || construction.getState().equals(SELECTING_MATERIALS)) {
			// work site cleared
			construction.setState(SELECTING_MATERIALS);
			reselectMaterials(construction); // sets to WAITING_FOR_RESOURCES when all materials chosen
			return;
		}

		refreshPlacedAllocations(construction);

		if (construction.getState().equals(WAITING_FOR_RESOURCES)) {
			createItemAllocations(construction);
			completeConstructionIfAllRequirementsInPlace(construction);
		} // else waiting for job to complete or similar
	}

	private void completeConstructionIfAllRequirementsInPlace(Construction construction) {
		if (allRequirementsInPosition(construction)) {
			if (construction.isAutoCompleted()) {
				messageDispatcher.dispatchMessage(MessageType.CONSTRUCTION_COMPLETED, construction);
			} else {
				createJob(construction);
			}
		}
	}

	private void reselectMaterials(Construction construction) {
		boolean allMaterialsSelected = true;
		for (QuantifiedItemTypeWithMaterial requirement : construction.getRequirements()) {
			requirement.setMaterial(null);

			if (construction.getPlayerSpecifiedPrimaryMaterial().isPresent() &&
				construction.getPlayerSpecifiedPrimaryMaterial().get().getMaterialType().equals(requirement.getItemType().getPrimaryMaterialType())) {
				requirement.setMaterial(construction.getPlayerSpecifiedPrimaryMaterial().get());
				continue;
			}

			messageDispatcher.dispatchMessage(MessageType.SELECT_AVAILABLE_MATERIAL_FOR_ITEM_TYPE, new ItemMaterialSelectionMessage(
					requirement.getItemType(),
					(gameMaterial) -> {
						if (gameMaterial != null) {
							requirement.setMaterial(gameMaterial);
						}
					}
			));

			if (requirement.getMaterial() == null) {
				allMaterialsSelected = false;
			}
		}

		if (allMaterialsSelected) {
			construction.setState(WAITING_FOR_RESOURCES);
		}
	}

	private void refreshPlacedAllocations(Construction furnitureConstruction) {
		furnitureConstruction.getPlacedItemAllocations().clear();

		for (GridPoint2 tileLocation : furnitureConstruction.getTileLocations()) {
			MapTile tileAtLocation = gameContext.getAreaMap().getTile(tileLocation);
			if (tileAtLocation != null) {
				for (Entity entity : tileAtLocation.getEntities()) {
					if (entity.getType().equals(EntityType.ITEM)) {
						ItemAllocationComponent itemAllocationComponent = entity.getOrCreateComponent(ItemAllocationComponent.class);
						if (itemAllocationComponent.getAllocationForPurpose(ON_FIRE) != null) {
							// Item is on fire so ignore this for now
							break;
						}
						ItemAllocation placedForConstructionAllocation = itemAllocationComponent.getAllocationForPurpose(PLACED_FOR_CONSTRUCTION);
						if (placedForConstructionAllocation == null || !furnitureConstruction.isItemUsedInConstruction(entity)) {
							if (placedForConstructionAllocation != null) {
								itemAllocationComponent.cancelAll(PLACED_FOR_CONSTRUCTION);
							}
							messageDispatcher.dispatchMessage(MessageType.REQUEST_ENTITY_HAULING, new RequestHaulingMessage(entity, entity, true, furnitureConstruction.getPriority(), null));
						} else {
							furnitureConstruction.getPlacedItemAllocations().put(tileLocation, placedForConstructionAllocation);
						}
					}
				}
			}
		}
	}

	private boolean otherItemsNeedRemoving(Construction furnitureConstruction) {
		boolean somethingNeedsRemoving = false;

		for (GridPoint2 tileLocation : furnitureConstruction.getTileLocations()) {
			MapTile tileAtLocation = gameContext.getAreaMap().getTile(tileLocation);
			if (tileAtLocation != null) {
				for (Entity entity : tileAtLocation.getEntities()) {
					if (entity.getType().equals(EntityType.ITEM)) {
						ItemAllocationComponent itemAllocationComponent = entity.getOrCreateComponent(ItemAllocationComponent.class);
						if (furnitureConstruction.isItemUsedInConstruction(entity)) {
							break;
						} else {
							itemAllocationComponent.cancelAll(PLACED_FOR_CONSTRUCTION);
						}
						if (itemAllocationComponent.getNumUnallocated() > 0) {
							messageDispatcher.dispatchMessage(MessageType.REQUEST_ENTITY_HAULING, new RequestHaulingMessage(entity, entity, true, furnitureConstruction.getPriority(), null));
						}
						somethingNeedsRemoving = true;
					} else if (entity.getType().equals(EntityType.PLANT)) {
						messageDispatcher.dispatchMessage(MessageType.REQUEST_PLANT_REMOVAL, new RequestPlantRemovalMessage(entity, tileLocation, furnitureConstruction.getPriority(), null));
						somethingNeedsRemoving = true;
					}
				}
			}
		}

		return somethingNeedsRemoving;
	}

	private void createItemAllocations(Construction construction) {
		for (QuantifiedItemTypeWithMaterial requirement : construction.getRequirements()) {
			int amountRequired = requirement.getQuantity();

			List<ItemAllocation> placedAllocations = new ArrayList<>();
			for (ItemAllocation placedAllocation : construction.getPlacedItemAllocations().values()) {
				Entity itemEntity = gameContext.getEntities().get(placedAllocation.getTargetItemEntityId());
				if (itemEntity == null) {
					Logger.error("Null item from construction's placed item allocations");
				} else {
					if (matchesRequirement(itemEntity, requirement)) {
						placedAllocations.add(placedAllocation);
					}
				}
			}
			construction.getIncomingHaulingAllocations().removeIf(h -> h.getItemAllocation().isCancelled());

			for (HaulingAllocation haulingAllocation : construction.getIncomingHaulingAllocations()) {
				Entity itemEntity = gameContext.getEntities().get(haulingAllocation.getItemAllocation().getTargetItemEntityId());
				if (itemEntity == null) {
					Logger.error("Null item from construction's incoming hauling allocations");
				} else {
					if (matchesRequirement(itemEntity, requirement)) {
						placedAllocations.add(haulingAllocation.getItemAllocation());
					}
				}
			}

			for (ItemAllocation placedAllocation : placedAllocations) {
				amountRequired -= placedAllocation.getAllocationAmount();
			}

			while (amountRequired > 0) {
				Job haulingJobForNewAllocation = createNewIncomingHaulingAllocation(construction, requirement, amountRequired);
				if (haulingJobForNewAllocation == null) {
					if (amountRequired == requirement.getQuantity()) {
						// None of this item available, go back to selecting materials
						construction.setState(SELECTING_MATERIALS);
						reselectMaterials(construction);
					}
					break;
				} else {
					messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJobForNewAllocation);
					amountRequired -= haulingJobForNewAllocation.getHaulingAllocation().getItemAllocation().getAllocationAmount();
				}
			}
		}
	}

	private boolean matchesRequirement(Entity itemEntity, QuantifiedItemTypeWithMaterial requirement) {
		ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
		return attributes.getPrimaryMaterial().equals(requirement.getMaterial()) && attributes.getItemType().equals(requirement.getItemType());
	}

	private HaulingAllocation tempAllocation;

	private Job createNewIncomingHaulingAllocation(Construction construction, QuantifiedItemTypeWithMaterial requirement,
												   int amountRequired) {
		final Vector2 constructionLocation = toVector(construction.getPrimaryLocation());
		if (construction.getEntity() != null) {
			constructionLocation.set(construction.getEntity().getLocationComponent().getWorldPosition());
		}

		tempAllocation = null;
		messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION, new RequestHaulingAllocationMessage(
				null, constructionLocation, requirement.getItemType(), requirement.getMaterial(), true,
				amountRequired, null, haulingAllocation -> tempAllocation = haulingAllocation));


		if (tempAllocation != null) {
			HaulingAllocation allocation = tempAllocation;
			tempAllocation = null;

			allocation.setTargetPositionType(CONSTRUCTION);
			allocation.setTargetId(construction.getId());
			allocation.setTargetPosition(getTargetPositionForAllocation(allocation, construction));

			construction.getIncomingHaulingAllocations().add(allocation);

			Entity targetItem = gameContext.getEntities().get(allocation.getItemAllocation().getTargetItemEntityId());
			return createHaulingJob(allocation, targetItem, haulingJobType, construction.getPriority());
		} else {
			return null;
		}
	}

	private boolean allRequirementsInPosition(Construction construction) {
		boolean allRequirementsMet = true;
		for (QuantifiedItemTypeWithMaterial requirement : construction.getRequirements()) {
			boolean thisRequirementMet = false;
			int quantityOfRequirement = 0;
			for (GridPoint2 tileLocation : construction.getTileLocations()) {
				MapTile tileAtLocation = gameContext.getAreaMap().getTile(tileLocation);
				if (tileAtLocation != null) {
					for (Entity entity : tileAtLocation.getEntities()) {
						if (entity.getType().equals(EntityType.ITEM)) {
							ItemEntityAttributes itemAttributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
							if (requirement.matches(entity)) {
								quantityOfRequirement += itemAttributes.getQuantity();
								if (quantityOfRequirement >= requirement.getQuantity()) {
									thisRequirementMet = true;
									break;
								}
							}
						}
					}
				}

				if (thisRequirementMet) {
					break;
				}
			}

			if (!thisRequirementMet) {
				allRequirementsMet = false;
				break;
			}
		}
		return allRequirementsMet;
	}

	private void createJob(Construction construction) {
		if (construction instanceof FurnitureConstruction) {
			createFurnitureConstructionJob((FurnitureConstruction) construction);
		} else if (construction instanceof WallConstruction) {
			createWallConstructionJob((WallConstruction) construction);
		} else if (construction instanceof BridgeConstruction) {
			createBridgeConstructionJob((BridgeConstruction) construction);
		} else {
			throw new NotImplementedException("Not yet implemented - create construction job for " + construction.toString());
		}
		construction.setState(ConstructionState.WAITING_FOR_COMPLETION);
	}

	private void createWallConstructionJob(WallConstruction wallConstruction) {
		CraftingType craftingType = wallConstruction.getWallTypeToConstruct().getCraftingType();
		Job constructionJob = jobFactory.constructionJob(craftingType);
		constructionJob.setJobPriority(wallConstruction.getPriority());
		constructionJob.setJobLocation(wallConstruction.getPrimaryLocation());
		constructionJob.setWorkDoneSoFar(0);
		constructionJob.setJobState(JobState.POTENTIALLY_ACCESSIBLE);
		constructionJob.setTargetId(wallConstruction.getId());

		wallConstruction.setConstructionJob(constructionJob);
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, constructionJob);
	}

	private void createBridgeConstructionJob(BridgeConstruction bridgeConstruction) {
		CraftingType craftingType = bridgeConstruction.getBridge().getBridgeType().getCraftingType();
		Job constructionJob = jobFactory.constructionJob(craftingType);
		constructionJob.setJobPriority(bridgeConstruction.getPriority());
		// TODO below was empty and caused a crash, but can't see how that would happen at the moment
		constructionJob.setJobLocation(bridgeConstruction.placedItemAllocations.keySet().iterator().next());
		constructionJob.setWorkDoneSoFar(0);
		constructionJob.setJobState(JobState.POTENTIALLY_ACCESSIBLE);
		constructionJob.setTargetId(bridgeConstruction.getId());

		bridgeConstruction.setConstructionJob(constructionJob);
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, constructionJob);
	}

	private void createFurnitureConstructionJob(FurnitureConstruction furnitureConstruction) {
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureConstruction.getEntity().getPhysicalEntityComponent().getAttributes();
		// FIXME Ideally these would be replaced with the generic CONSTRUCTION job type
		JobType jobType = constructWoodenFurnitureJobType;
		if (attributes.getPrimaryMaterialType().equals(GameMaterialType.STONE)) {
			jobType = constructStoneFurnitureJobType;
		}

		Job constructionJob = new Job(jobType);
		constructionJob.setJobPriority(furnitureConstruction.getPriority());
		constructionJob.setJobLocation(furnitureConstruction.getPrimaryLocation());

		furnitureConstruction.setConstructionJob(constructionJob);
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, constructionJob);
	}

	private GridPoint2 getTargetPositionForAllocation(HaulingAllocation allocation, Construction construction) {
		List<GridPoint2> positionsToCheck = new ArrayList<>();
		positionsToCheck.add(construction.getPrimaryLocation());
		for (GridPoint2 location : construction.getTileLocations()) {
			if (!location.equals(construction.getPrimaryLocation())) {
				positionsToCheck.add(location);
			}
		}
		MapTile sourceTile = gameContext.getAreaMap().getTile(allocation.getSourcePosition());
		if (sourceTile == null) {
			Logger.error("Allocation source position not set");
			return construction.getPrimaryLocation();
		}
		int sourceRegionId = sourceTile.getRegionId();

		Entity incomingItemEntity = gameContext.getEntities().get(allocation.getItemAllocation().getTargetItemEntityId());
		ItemEntityAttributes incomingAttributes = (ItemEntityAttributes) incomingItemEntity.getPhysicalEntityComponent().getAttributes();

		for (GridPoint2 positionToCheck : positionsToCheck) {
			MapTile tileAtPosition = gameContext.getAreaMap().getTile(positionToCheck);
			if (tileAtPosition == null || tileAtPosition.getRegionId() != sourceRegionId) {
				continue;
			}

			List<HaulingAllocation> haulingAllocationsToPosition = getExistingAllocations(positionToCheck, construction);
			ItemAllocation itemAllocationAtPosition = construction.getPlacedItemAllocations().get(positionToCheck);

			List<ItemAllocation> allocationsToPosition = new ArrayList<>();
			if (itemAllocationAtPosition != null) {
				allocationsToPosition.add(itemAllocationAtPosition);
			}
			for (HaulingAllocation haulingAllocation : haulingAllocationsToPosition) {
				allocationsToPosition.add(haulingAllocation.getItemAllocation());
			}

			if (allocationsToPosition.isEmpty()) {
				// Nothing in this position
				return positionToCheck;
			} else {
				Entity firstEntityAtPosition = gameContext.getEntities().get(allocationsToPosition.get(0).getTargetItemEntityId());
				if (firstEntityAtPosition == null) {
					Logger.error("Entity not found for incoming hauling allocation");
				} else {
					ItemEntityAttributes firstEntityAttributes = (ItemEntityAttributes) firstEntityAtPosition.getPhysicalEntityComponent().getAttributes();
					if (firstEntityAttributes.getPrimaryMaterial().equals(incomingAttributes.getPrimaryMaterial()) &&
							firstEntityAttributes.getItemType().equals(incomingAttributes.getItemType())) {
						// Might be able to fit here
						int quantityAssignedToPosition = 0;
						for (ItemAllocation itemAllocation : allocationsToPosition) {
							quantityAssignedToPosition += itemAllocation.getAllocationAmount();
						}

						if (quantityAssignedToPosition + allocation.getItemAllocation().getAllocationAmount() <= incomingAttributes.getItemType().getMaxStackSize()) {
							return positionToCheck;
						}
					}
				}
			}
		}

		// Some kind of error - must be using more types of ingredients than tile covered
		Logger.error("More construction ingredients than tile locations - the construction recipe should be simplified to use fewer ingredients or cover more tiles");
		return construction.getPrimaryLocation();
	}

	private List<HaulingAllocation> getExistingAllocations(GridPoint2 positionToCheck, Construction construction) {
		List<HaulingAllocation> allocationsAtPosition = new ArrayList<>();
		for (HaulingAllocation haulingAllocation : construction.getIncomingHaulingAllocations()) {
			if (haulingAllocation.getTargetPosition().equals(positionToCheck)) {
				allocationsAtPosition.add(haulingAllocation);
			}
		}
		return allocationsAtPosition;
	}


	@Override
	public boolean runWhilePaused() {
		return false;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
