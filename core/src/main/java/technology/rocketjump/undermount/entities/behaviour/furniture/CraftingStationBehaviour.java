package technology.rocketjump.undermount.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.NotImplementedException;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.undermount.crafting.model.CraftingRecipe;
import technology.rocketjump.undermount.crafting.model.CraftingRecipeMaterialSelection;
import technology.rocketjump.undermount.entities.ItemEntityMessageHandler;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.components.LiquidContainerComponent;
import technology.rocketjump.undermount.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.undermount.entities.components.furniture.ConstructedEntityComponent;
import technology.rocketjump.undermount.entities.components.furniture.FurnitureParticleEffectsComponent;
import technology.rocketjump.undermount.entities.components.humanoid.SteeringComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.undermount.entities.tags.CraftingOverrideTag;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.*;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.*;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rooms.HaulingAllocation;
import technology.rocketjump.undermount.settlement.production.ProductionAssignment;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nWord;

import java.util.*;

import static technology.rocketjump.undermount.entities.behaviour.furniture.FillLiquidContainerBehaviour.relatedContainerCapacity;
import static technology.rocketjump.undermount.entities.components.ItemAllocation.AllocationState.CANCELLED;
import static technology.rocketjump.undermount.entities.components.ItemAllocation.Purpose.HELD_IN_INVENTORY;
import static technology.rocketjump.undermount.entities.tags.CraftingOverrideTag.CraftingOverrideSetting.DO_NOT_HAUL_OUTPUT;
import static technology.rocketjump.undermount.jobs.model.JobState.ASSIGNABLE;
import static technology.rocketjump.undermount.jobs.model.JobState.REMOVED;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.misc.VectorUtils.toVector;
import static technology.rocketjump.undermount.rooms.HaulingAllocation.AllocationPositionType.FURNITURE;
import static technology.rocketjump.undermount.ui.i18n.I18nTranslator.oneDecimalFormat;

public class CraftingStationBehaviour extends FurnitureBehaviour
		implements ProductionAssignmentRequestMessage.ProductionAssignmentCallback,
		RequestHaulingAllocationMessage.ItemAllocationCallback,
		SelectableDescription,
		Destructible,
		OnJobCompletion,
		Prioritisable,
		ParentDependentEntityComponent {

	private CraftingType craftingType;
	private JobType craftItemJobType;
	private JobType haulingJobType;
	private GameMaterialDictionary gameMaterialDictionary;

	private ProductionAssignment currentProductionAssignment;
	private Job craftingJob;
	private boolean requiresExtraTime;
	private Double extraTimeToProcess;
	private double lastUpdateGameTime;
	private final List<HaulingAllocation> haulingInputAllocations = new ArrayList<>();
	private final List<Job> liquidTransferJobs = new ArrayList<>();


	public CraftingStationBehaviour() {

	}

	public CraftingStationBehaviour(CraftingType craftingType, JobType craftItemJobType, JobType haulingJobType,
									GameMaterialDictionary gameMaterialDictionary) {
		this.craftingType = craftingType;
		this.craftItemJobType = craftItemJobType;
		this.haulingJobType = haulingJobType;
		this.gameMaterialDictionary = gameMaterialDictionary;
	}

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		super.init(parentEntity, messageDispatcher, gameContext);
		if (currentProductionAssignment != null) {
			currentProductionAssignment.setAssignedCraftingStation(parentEntity);
		}
		lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (currentProductionAssignment != null) {
			messageDispatcher.dispatchMessage(MessageType.PRODUCTION_ASSIGNMENT_CANCELLED, currentProductionAssignment);
			currentProductionAssignment = null;
			requiresExtraTime = false;
		}
		if (craftingJob != null && !craftingJob.getJobState().equals(REMOVED)) {
			messageDispatcher.dispatchMessage(MessageType.JOB_CANCELLED, craftingJob);
			craftingJob = null;
		}
	}


	@Override
	public CraftingStationBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		throw new NotImplementedException("Not yet implemented clone() in " + this.getClass().getSimpleName());
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);
		clearCompletedLiquidJobs();

		if (extraTimeToProcess != null) {
			FurnitureParticleEffectsComponent particleEffectsComponent = parentEntity.getComponent(FurnitureParticleEffectsComponent.class);
			if (particleEffectsComponent != null) {
				particleEffectsComponent.triggerProcessingEffects(
						Optional.ofNullable( currentProductionAssignment != null ? new JobTarget(currentProductionAssignment.targetRecipe, parentEntity) : null));
			}

			double elapsedTime = gameContext.getGameClock().getCurrentGameTime() - lastUpdateGameTime;
			extraTimeToProcess -= elapsedTime;
			if (extraTimeToProcess < 0) {
				requiresExtraTime = false;
				extraTimeToProcess = null;
				jobCompleted(gameContext);

				if (particleEffectsComponent != null) {
					for (ParticleEffectInstance effectInstance : particleEffectsComponent.getCurrentParticleInstances()) {
						messageDispatcher.dispatchMessage(MessageType.PARTICLE_RELEASE, effectInstance);
					}
				}
			}
		}
		lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();

		ConstructedEntityComponent constructedEntityComponent = parentEntity.getComponent(ConstructedEntityComponent.class);
		if (constructedEntityComponent != null && constructedEntityComponent.isBeingDeconstructed()) {
			this.destroy(parentEntity, messageDispatcher, gameContext);
			return;
		}

		if (craftingJob != null) {
			return; // Waiting for entity to craft item with materials
		}

		if (currentProductionAssignment == null) {

			InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);
			if (!inventoryComponent.isEmpty()) {
				clearInventoryItems();
				// Still waiting for items to be collected
				return;
			}

			LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);
			if (liquidContainerComponent != null && liquidContainerComponent.getLiquidQuantity() > 0) {
				clearLiquid(gameContext);
				// Still waiting for liquid to be moved
				return;
			}

			messageDispatcher.dispatchMessage(MessageType.REQUEST_PRODUCTION_ASSIGNMENT, new ProductionAssignmentRequestMessage(craftingType, this));
			if (currentProductionAssignment == null) {
				// Couldn't find a valid crafting recipe yet
				return;
			}
			haulingInputAllocations.clear();
			liquidTransferJobs.clear();

		} else {
			// Crafting in progress, check if all requirements met on item or liquid addition
		}

		if (currentProductionAssignment.getInputSelectionsLastUpdated() < gameContext.getGameClock().getCurrentGameTime() - gameContext.getGameClock().HOURS_IN_DAY &&
			haulingInputAllocations.isEmpty()) {
			// More than one day since last updated selections, perhaps we ran out of that material
			currentProductionAssignment.getInputMaterialSelections().clear();
			currentProductionAssignment.setInputSelectionsLastUpdated(gameContext.getGameClock().getCurrentGameTime());
		}

		if (currentProductionAssignment.inputMaterialSelections.size() != currentProductionAssignment.targetRecipe.getInput().size()) {
			selectMaterials(gameContext);
		}

		for (QuantifiedItemTypeWithMaterial inputRequirement : currentProductionAssignment.getInputMaterialSelections()) {
			createMissingAllocationIfNeeded(inputRequirement, gameContext);
		}

	}

	@Override
	public void productionAssignmentCallback(List<CraftingRecipe> potentialCraftingRecipes) {
		for (CraftingRecipe potentialCraftingRecipe : potentialCraftingRecipes) {
			if (potentialCraftingRecipe.getInput().stream().anyMatch(QuantifiedItemTypeWithMaterial::isLiquid) ||
					potentialCraftingRecipe.getOutput().stream().anyMatch(QuantifiedItemTypeWithMaterial::isLiquid)) {
				// Crafting requires liquid container
				LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);
				if (liquidContainerComponent == null) {
					Logger.warn("Crafting station received receipe requiring liquid but has no liquid container component");
					continue;
				}
			}

			// Selecting potentialCraftingRecipe for currentProductionAssignment

			potentialCraftingRecipe.getInput().stream().filter(QuantifiedItemTypeWithMaterial::isLiquid).findFirst().ifPresent(liquidInput -> {
				LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);
				liquidContainerComponent.setLiquidQuantity(0);
				liquidContainerComponent.setTargetLiquidMaterial(liquidInput.getMaterial());
			});

			currentProductionAssignment = new ProductionAssignment(potentialCraftingRecipe, this.parentEntity);
			requiresExtraTime = currentProductionAssignment.targetRecipe.getExtraGameHoursToComplete() != null;
			messageDispatcher.dispatchMessage(MessageType.PRODUCTION_ASSIGNMENT_ACCEPTED, currentProductionAssignment);
			break;
		}

	}

	public void allocationCancelled(HaulingAllocation allocation) {
		haulingInputAllocations.remove(allocation);
	}

	private void createMissingAllocationIfNeeded(QuantifiedItemTypeWithMaterial requirement, GameContext gameContext) {
		if (requirement.isLiquid()) {
			createLiquidTransferJobIfNeeded(requirement, gameContext);
			return;
		}

		int amountRequired = requirement.getQuantity();
		Iterator<HaulingAllocation> iterator = haulingInputAllocations.iterator();
		while (iterator.hasNext()) {
			HaulingAllocation haulingAllocation = iterator.next();
			if (haulingAllocation.getItemAllocation().getState().equals(CANCELLED)) {
				iterator.remove();
				continue;
			}
			Entity incomingEntity = gameContext.getEntities().get(haulingAllocation.getItemAllocation().getTargetItemEntityId());
			if (incomingEntity != null) {
				ItemEntityAttributes incomingAttributes = (ItemEntityAttributes) incomingEntity.getPhysicalEntityComponent().getAttributes();

				if (incomingAttributes.getItemType().equals(requirement.getItemType()) &&
						(requirement.getMaterial() == null || incomingAttributes.getMaterial(incomingAttributes.getItemType().getPrimaryMaterialType()).equals(requirement.getMaterial()))) {
					amountRequired -= haulingAllocation.getItemAllocation().getAllocationAmount();
				}
			}
		}



		InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);
		for (InventoryComponent.InventoryEntry entry : inventoryComponent.getInventoryEntries()) {
			if (entry.entity.getType().equals(EntityType.ITEM)) {
				ItemEntityAttributes inventoryItemAttributes = (ItemEntityAttributes) entry.entity.getPhysicalEntityComponent().getAttributes();

				if (inventoryItemAttributes.getItemType().equals(requirement.getItemType()) &&
						(requirement.getMaterial() == null || inventoryItemAttributes.getMaterial(inventoryItemAttributes.getItemType().getPrimaryMaterialType()).equals(requirement.getMaterial()))) {
					amountRequired -= inventoryItemAttributes.getQuantity();
				}
			}
		}


		if (amountRequired > 0) {
			// TODO Probably want to keep hold of these jobs and cancel them when destroyed
			createHaulingJob(requirement, amountRequired, gameContext);
		}
	}

	private void clearCompletedLiquidJobs() {
		liquidTransferJobs.removeIf(job -> job.getJobState().equals(JobState.REMOVED));
	}

	private void createLiquidTransferJobIfNeeded(QuantifiedItemTypeWithMaterial requirement, GameContext gameContext) {
		LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);
		if (liquidContainerComponent == null) {
			Logger.error("Crafting recipe requires liquid container but crafting station parent does not have one");
		} else if (relatedItemTypes.isEmpty()) {
			Logger.error("Crafting recipe with liquid input requires related item type for liquid transfer but is not set");
		} else {
			float assignedQuantity = liquidContainerComponent.getLiquidQuantity();
			for (Job incomingLiquidTransferJob : liquidTransferJobs) {
				assignedQuantity += relatedContainerCapacity(relatedItemTypes.get(0));
			}

			if (assignedQuantity < requirement.getQuantity()) {
				// Create new job
				Profession professionRequired = currentProductionAssignment.targetRecipe.getCraftingType().getProfessionRequired();
				FurnitureLayout.Workspace navigableWorkspace = getAnyNavigableWorkspace(parentEntity, gameContext.getAreaMap());
				if (navigableWorkspace != null) {
					messageDispatcher.dispatchMessage(MessageType.REQUEST_LIQUID_TRANSFER, new RequestLiquidTransferMessage(
							requirement.getMaterial(), true, parentEntity,
							toVector(navigableWorkspace.getLocation()), relatedItemTypes.get(0), professionRequired, priority, (job) -> {
						if (job != null) {
							this.liquidTransferJobs.add(job);
						}
					}));
				}
			}
		}
	}

	private HaulingAllocation haulingAllocation;

	private Job createHaulingJob(QuantifiedItemTypeWithMaterial requirement, int amountRequired, GameContext gameContext) {
		FurnitureLayout.Workspace workspace = getAnyNavigableWorkspace(parentEntity, gameContext.getAreaMap());
		if (workspace == null) {
			Logger.warn("Could not find a navigable workspace to create crafting job");
			return null;
		}

		// Clear temporary itemAllocation to avoid issues with lambdas
		haulingAllocation = null;
		GameMaterial requiredMaterial = requirement.getMaterial();
		if (requiredMaterial == null) {
			Optional<GameMaterial> selection = gameContext.getSettlementState().craftingRecipeMaterialSelections.computeIfAbsent(this.currentProductionAssignment.targetRecipe,
					a -> new CraftingRecipeMaterialSelection(this.currentProductionAssignment.targetRecipe))
					.getSelection(requirement);
			if (selection.isPresent()) {
				requiredMaterial = selection.get();
			}
		}

		messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION,
				new RequestHaulingAllocationMessage(parentEntity, parentEntity.getLocationComponent().getWorldOrParentPosition(), requirement.getItemType(), requiredMaterial,
				true, amountRequired, null, this));

		if (haulingAllocation != null) {
			haulingAllocation.setTargetPositionType(FURNITURE);
			haulingAllocation.setTargetId(parentEntity.getId());
			haulingAllocation.setTargetPosition(toGridPoint(parentEntity.getLocationComponent().getWorldPosition()));

			haulingInputAllocations.add(haulingAllocation);

			Job haulingJob = ItemEntityMessageHandler.createHaulingJob(haulingAllocation,
					gameContext.getEntities().get(haulingAllocation.getHauledEntityId()), haulingJobType, priority);
			haulingJob.setRequiredProfession(craftingType.getProfessionRequired());
			messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJob);
			haulingAllocation = null;
			return haulingJob;
		} else {
			// No items of this type available
			return null;
		}
	}

	private void selectMaterials(GameContext gameContext) {
		for (int inputCursor = 0; inputCursor < currentProductionAssignment.targetRecipe.getInput().size(); inputCursor++) {
			if (currentProductionAssignment.getInputMaterialSelections().size() > inputCursor) {
				// Already have this one selected
				continue;
			}

			QuantifiedItemTypeWithMaterial inputRequirement = currentProductionAssignment.targetRecipe.getInput().get(inputCursor).clone();

			if (inputRequirement.getMaterial() != null) {
				// already have a material set
				currentProductionAssignment.getInputMaterialSelections().add(inputRequirement);
			} else if (inputRequirement.isLiquid()) {
				Logger.error("Not yet implemented: Material selection for liquid crafting inputs");
			} else {
				// Need to pick a material
				messageDispatcher.dispatchMessage(MessageType.SELECT_AVAILABLE_MATERIAL_FOR_ITEM_TYPE, new ItemMaterialSelectionMessage(
						inputRequirement.getItemType(),
						(gameMaterial) -> {
							if (gameMaterial != null) {
								inputRequirement.setMaterial(gameMaterial);
							}
						}
				));

				if (inputRequirement.getMaterial() != null) {
					currentProductionAssignment.getInputMaterialSelections().add(inputRequirement);
					currentProductionAssignment.setInputSelectionsLastUpdated(gameContext.getGameClock().getCurrentGameTime());
				} else {
					// Couldn't find an appropriate material for this item, break out of loop to match order of entries with targetRecipe.inputs
					break;
				}
			}
		}
	}

	@Override
	public void allocationFound(HaulingAllocation haulingAllocation) {
		this.haulingAllocation = haulingAllocation;
	}

	public void itemAdded(TiledMap areaMap) {
		if (currentProductionAssignment == null) {
			Logger.error("Item added to " + this.getClass().getSimpleName() + " without a current production assignement");
			return;
		}

		checkInputRequirementsMet(areaMap);
	}

	public void liquidAdded(float quantityAdded, TiledMap areaMap) {
		if (currentProductionAssignment == null) {
			Logger.error("Liquid added to " + this.getClass().getSimpleName() + " without a current production assignement");
			return;
		}

		LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);
		liquidContainerComponent.assignCraftingAllocation(quantityAdded);

		checkInputRequirementsMet(areaMap);
	}

	private void checkInputRequirementsMet(TiledMap areaMap) {
		if (allLiquidRequirementsMet()) {
			checkItemRequirementsMet(areaMap);
		}
	}

	private boolean allLiquidRequirementsMet() {
		LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);

		boolean allLiquidRequirementsMatched = true;
		for (QuantifiedItemTypeWithMaterial input : currentProductionAssignment.targetRecipe.getInput()) {
			if (input.isLiquid()) {
				if (liquidContainerComponent.getLiquidQuantity() < input.getQuantity() ||
						!liquidContainerComponent.getTargetLiquidMaterial().equals(input.getMaterial())) {
					allLiquidRequirementsMatched = false;
					break;
				}
			}
		}
		return allLiquidRequirementsMatched;
	}

	private void checkItemRequirementsMet(TiledMap areaMap) {
		InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);
		boolean allRequirementsMet = true;
		for (QuantifiedItemTypeWithMaterial requirement : currentProductionAssignment.targetRecipe.getInput()) {
			if (requirement.isLiquid()) {
				continue;
			}

			boolean thisRequirementMet = false;
			int quantityInInventory = 0;
			for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
				if (inventoryEntry.entity.getType().equals(EntityType.ITEM)) {
					ItemEntityAttributes itemAttributes = (ItemEntityAttributes) inventoryEntry.entity.getPhysicalEntityComponent().getAttributes();
					if (itemAttributes.getItemType().equals(requirement.getItemType())) {
						if (requirement.getMaterial() == null || itemAttributes.getPrimaryMaterial().equals(requirement.getMaterial())) {
							quantityInInventory += itemAttributes.getQuantity();
							if (quantityInInventory >= requirement.getQuantity()) {
								thisRequirementMet = true;
								break;
							}
						}
					}
				}
			}

			if (!thisRequirementMet) {
				allRequirementsMet = false;
				break;
			}
		}

		if (allRequirementsMet) {
			// Create crafting job to produce output
			Job craftingJob = new Job(craftItemJobType);
			craftingJob.setJobPriority(priority);
			craftingJob.setJobState(ASSIGNABLE);
			updateJobLocation(craftingJob, areaMap);
			craftingJob.setTargetId(parentEntity.getId());
			craftingJob.setRequiredProfession(currentProductionAssignment.targetRecipe.getCraftingType().getProfessionRequired());
			ItemType itemTypeRequired = Optional.ofNullable(currentProductionAssignment.targetRecipe.getItemTypeRequired()).orElse(currentProductionAssignment.targetRecipe.getCraftingType().getDefaultItemType());
			craftingJob.setRequiredItemType(itemTypeRequired);
			craftingJob.setCraftingRecipe(currentProductionAssignment.targetRecipe);
			messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, craftingJob);
			this.craftingJob = craftingJob;
		}
	}

	private boolean updateJobLocation(Job craftingJob, TiledMap areaMap) {
		FurnitureLayout.Workspace navigableWorkspace = getAnyNavigableWorkspace(parentEntity, areaMap);
		if (navigableWorkspace == null) {
			Logger.warn("Could not access workstation at " + parentEntity.getLocationComponent().getWorldPosition());
			return false;
		} else {
			craftingJob.setJobLocation(navigableWorkspace.getAccessedFrom());
			craftingJob.setSecondaryLocation(navigableWorkspace.getLocation());
			return true;
		}
	}

	@Override
	public void jobCompleted(GameContext gameContext) {
		if (requiresExtraTime) {
			extraTimeToProcess = currentProductionAssignment.targetRecipe.getExtraGameHoursToComplete();
			return;
		}

		// Replace input in inventory with output
		List<Entity> output = new ArrayList<>();
		InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
		LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);

		if (currentProductionAssignment == null || currentProductionAssignment.targetRecipe == null) {
			// FIXME Not seen this happen but has come up in a bug report
			Logger.error("Null production assignment in jobCompleted() in " + this.getClass().getSimpleName());
		} else {
			for (QuantifiedItemTypeWithMaterial outputRequirement : currentProductionAssignment.targetRecipe.getOutput()) {
				if (outputRequirement.isLiquid()) {
					// Just switching contents of liquid container, no material transfer possible? Implies liquids should be more than a material
					// I.e. current materials (water, wort) are equivalent to ItemType (LiquidType?) which have one or more materials, same for soup
					liquidContainerComponent.cancelAllAllocations();
					liquidContainerComponent.setLiquidQuantity(outputRequirement.getQuantity());
					liquidContainerComponent.setTargetLiquidMaterial(outputRequirement.getMaterial());
				} else {
					addOutputItemToList(outputRequirement, gameContext, output);
				}
			}
		}

		if (liquidContainerComponent != null &&
				currentProductionAssignment.targetRecipe.getOutput().stream().filter(QuantifiedItemTypeWithMaterial::isLiquid).findAny().isEmpty()) {
			// No liquid outputs but liquid container, should clear
			liquidContainerComponent.cancelAllAllocations();
			liquidContainerComponent.setLiquidQuantity(0);
			liquidContainerComponent.setTargetLiquidMaterial(null);
		}

		for (InventoryComponent.InventoryEntry inventoryEntry : new ArrayList<>(inventoryComponent.getInventoryEntries())) {
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, new EntityMessage(inventoryEntry.entity.getId()));
		}
		for (Entity outputEntity : output) {
			inventoryComponent.add(outputEntity, parentEntity, messageDispatcher, gameContext.getGameClock());
			// Unallocate from inventory
			outputEntity.getOrCreateComponent(ItemAllocationComponent.class).cancelAll(HELD_IN_INVENTORY);

			if (outputHaulingAllowed()) {
				// Request hauling to remove items in output
				RequestHaulingMessage requestHaulingMessage = new RequestHaulingMessage(outputEntity, parentEntity, true, priority, null);
				if (craftingJob != null) {
					requestHaulingMessage.setSpecificProfessionRequired(craftingJob.getRequiredProfession());
				}
				messageDispatcher.dispatchMessage(MessageType.REQUEST_ITEM_HAULING, requestHaulingMessage);
			}
		}

		messageDispatcher.dispatchMessage(MessageType.PRODUCTION_ASSIGNMENT_COMPLETED, currentProductionAssignment);
		this.craftingJob = null;
		this.currentProductionAssignment = null;
		this.requiresExtraTime = false;
		this.haulingInputAllocations.clear();
		this.liquidTransferJobs.clear();

		// rerun update to trigger export item/liquid jobs
		infrequentUpdate(gameContext);
	}

	@Override
	public void setPriority(JobPriority jobPriority) {
		super.setPriority(jobPriority);
		if (craftingJob != null) {
			craftingJob.setJobPriority(priority);
		}
		for (Job job : liquidTransferJobs) {
			job.setJobPriority(jobPriority);
		}
	}

	private void addOutputItemToList(QuantifiedItemTypeWithMaterial outputRequirement, GameContext gameContext, List<Entity> output) {
		InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
		ItemEntityAttributes outputAttributes = new ItemEntityAttributes(gameContext.getRandom().nextLong());
		outputAttributes.setItemType(outputRequirement.getItemType());
		outputAttributes.setItemPlacement(ItemPlacement.ON_GROUND);


		if (currentProductionAssignment.targetRecipe.getMaterialTypesToCopyOver() != null) {
			for (GameMaterialType materialTypeToCopy : currentProductionAssignment.targetRecipe.getMaterialTypesToCopyOver()) {
				GameMaterial materialToAdd = null;
				for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
					ItemEntityAttributes inventoryAttributes = (ItemEntityAttributes) inventoryEntry.entity.getPhysicalEntityComponent().getAttributes();
					materialToAdd = inventoryAttributes.getMaterial(materialTypeToCopy);
					if (materialToAdd != null) {
						break;
					}
				}
				if (materialToAdd != null) {
					outputAttributes.setMaterial(materialToAdd);
				}
			}
		}

		// Force any specified output materials
		if (outputRequirement.getMaterial() != null) {
			outputAttributes.setMaterial(outputRequirement.getMaterial());
		}

		// Randomly add any missing material types
		for (GameMaterialType requiredMaterialType : outputAttributes.getItemType().getMaterialTypes()) {
			if (outputAttributes.getMaterial(requiredMaterialType) == null) {
				List<GameMaterial> materialsToPickFrom = gameMaterialDictionary.getByType(requiredMaterialType);
				GameMaterial material = materialsToPickFrom.get(gameContext.getRandom().nextInt(materialsToPickFrom.size()));
				outputAttributes.setMaterial(material);
			}
		}

		outputAttributes.setQuantity(outputRequirement.getQuantity());

		messageDispatcher.dispatchMessage(MessageType.ITEM_CREATION_REQUEST, new ItemCreationRequestMessage(outputAttributes, (outputItem) -> {
			output.add(outputItem);
		}));
	}

	private boolean outputHaulingAllowed() {
		CraftingOverrideTag craftingOverrideTag = parentEntity.getTag(CraftingOverrideTag.class);
		return craftingOverrideTag == null || !craftingOverrideTag.includes(DO_NOT_HAUL_OUTPUT);
	}

	private void clearInventoryItems() {
		if (!outputHaulingAllowed()) {
			return;
		}

		InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
		for (InventoryComponent.InventoryEntry entry : inventoryComponent.getInventoryEntries()) {
			ItemAllocationComponent itemAllocationComponent = entry.entity.getOrCreateComponent(ItemAllocationComponent.class);
			itemAllocationComponent.cancelAll(HELD_IN_INVENTORY);
			if (itemAllocationComponent.getNumUnallocated() > 0) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_ITEM_HAULING, new RequestHaulingMessage(entry.entity, parentEntity, true, priority, job -> {
					// Do nothing with job
				}));
			}
		}

	}

	private void clearLiquid(GameContext gameContext) {
		if (!outputHaulingAllowed()) {
			return;
		}

		LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);
		if (liquidContainerComponent.getNumUnallocated() > 0) {
			float amount = Math.min(liquidContainerComponent.getNumUnallocated(), relatedContainerCapacity(relatedItemTypes.get(0)));
			FurnitureLayout.Workspace navigableWorkspace = getAnyNavigableWorkspace(parentEntity, gameContext.getAreaMap());
			if (navigableWorkspace != null) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_LIQUID_REMOVAL, new RequestLiquidRemovalMessage(parentEntity,
						navigableWorkspace.getAccessedFrom(), amount, relatedItemTypes.get(0), priority, job -> {
					if (job != null) {
						liquidTransferJobs.add(job);
					}
				}));
			}
		}
	}

	public static FurnitureLayout.Workspace getNearestNavigableWorkspace(Entity furnitureEntity, TiledMap areaMap, GridPoint2 origin) {
		if (furnitureEntity == null) {
			Logger.error("Null entity passed to getNearestNavigableWorkspace");
			return null;
		}
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();

		GridPoint2 furniturePosition = toGridPoint(furnitureEntity.getLocationComponent().getWorldPosition());
		List<FurnitureLayout.Workspace> navigableWorkspaces = new ArrayList<>();
		for (FurnitureLayout.Workspace workspace : attributes.getCurrentLayout().getWorkspaces()) {
			GridPoint2 accessedFromLocation = furniturePosition.cpy().add(workspace.getAccessedFrom());
			MapTile accessedFromTile = areaMap.getTile(accessedFromLocation);
			if (accessedFromTile != null && accessedFromTile.isNavigable()) {
				FurnitureLayout.Workspace worldPositionedWorkspace = new FurnitureLayout.Workspace();
				worldPositionedWorkspace.setLocation(furniturePosition.cpy().add(workspace.getLocation()));
				worldPositionedWorkspace.setAccessedFrom(furniturePosition.cpy().add(workspace.getAccessedFrom()));
				navigableWorkspaces.add(worldPositionedWorkspace);
			}
		}

		if (navigableWorkspaces.isEmpty()) {
			return null;
		} else {
			return navigableWorkspaces.stream().sorted((o1, o2) -> {
				float distanceTo1 = o1.getAccessedFrom().dst2(origin);
				float distanceTo2 = o2.getAccessedFrom().dst2(origin);
				return Math.round(distanceTo1 - distanceTo2);
			}).findFirst().get();
		}
	}


	public static FurnitureLayout.Workspace getAnyNavigableWorkspace(Entity furnitureEntity, TiledMap areaMap) {
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();

		GridPoint2 furniturePosition = toGridPoint(furnitureEntity.getLocationComponent().getWorldPosition());
		List<FurnitureLayout.Workspace> workspaces = attributes.getCurrentLayout().getWorkspaces();
		Collections.shuffle(workspaces);
		for (FurnitureLayout.Workspace workspace : workspaces) {
			GridPoint2 accessedFromLocation = furniturePosition.cpy().add(workspace.getAccessedFrom());
			MapTile accessedFromTile = areaMap.getTile(accessedFromLocation);
			if (accessedFromTile != null && accessedFromTile.isNavigable()) {
				FurnitureLayout.Workspace worldPositionedWorkspace = new FurnitureLayout.Workspace();
				worldPositionedWorkspace.setLocation(furniturePosition.cpy().add(workspace.getLocation()));
				worldPositionedWorkspace.setAccessedFrom(furniturePosition.cpy().add(workspace.getAccessedFrom()));
				return worldPositionedWorkspace;
			}
		}

		return null;
	}

	public ProductionAssignment getCurrentProductionAssignment() {
		return currentProductionAssignment;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		// Do nothing every frame
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return false;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return true;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
	}

	@Override
	public SteeringComponent getSteeringComponent() {
		return null;
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext) {
		List<I18nText> descriptions = new ArrayList<>(2);

		if (currentProductionAssignment != null) {
			I18nText targetDescription;
			// FIXME assuming 1 output for now
			QuantifiedItemTypeWithMaterial output = currentProductionAssignment.targetRecipe.getOutput().get(0);
			if (output.isLiquid()) {
				targetDescription = i18nTranslator.getLiquidDescription(output.getMaterial(), output.getQuantity());
			} else {
				targetDescription = i18nTranslator.getItemDescription(
						output.getQuantity(),
						output.getMaterial(),
						output.getItemType());
			}
			descriptions.add(i18nTranslator.getTranslatedWordWithReplacements("ACTION.JOB.CREATE_GENERIC",
					ImmutableMap.of("targetDescription", targetDescription)));
		}
		if (requiresExtraTime) {
			Double totalExtraHours = currentProductionAssignment.targetRecipe.getExtraGameHoursToComplete();
			double progress = (totalExtraHours - (extraTimeToProcess == null ? totalExtraHours : extraTimeToProcess)) / totalExtraHours;
			descriptions.add(i18nTranslator.getTranslatedWordWithReplacements("FURNITURE.DESCRIPTION.GENERIC_PROGRESS",
					ImmutableMap.of("progress", new I18nWord(oneDecimalFormat.format(100f * progress)))));
		}
		return descriptions;
	}

	public CraftingType getCraftingType() {
		return this.craftingType;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		asJson.put("craftingType", craftingType.getName());
		asJson.put("craftItemJobType", craftItemJobType.getName());
		asJson.put("haulingJobType", haulingJobType.getName());
		if (requiresExtraTime) {
			asJson.put("requiresExtraTime", true);
		}
		if (extraTimeToProcess != null) {
			asJson.put("extraTimeToProcess", extraTimeToProcess);
		}

		if (currentProductionAssignment != null) {
			currentProductionAssignment.writeTo(savedGameStateHolder);
			asJson.put("currentProductionAssignment", currentProductionAssignment.productionAssignmentId);
		}

		if (craftingJob != null) {
			craftingJob.writeTo(savedGameStateHolder);
			asJson.put("craftingJob", craftingJob.getJobId());
		}

		if (!haulingInputAllocations.isEmpty()) {
			JSONArray inputAllocationsJson = new JSONArray();
			for (HaulingAllocation inputAllocation : haulingInputAllocations) {
				inputAllocation.writeTo(savedGameStateHolder);
				inputAllocationsJson.add(inputAllocation.getHaulingAllocationId());
			}
			asJson.put("inputAllocations", inputAllocationsJson);
		}

		if (!liquidTransferJobs.isEmpty()) {
			JSONArray liquidTransferJobsJson = new JSONArray();
			for (Job outstandingJob : liquidTransferJobs) {
				outstandingJob.writeTo(savedGameStateHolder);
				liquidTransferJobsJson.add(outstandingJob.getJobId());
			}
			asJson.put("liquidTransferJobs", liquidTransferJobsJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.craftingType = relatedStores.craftingTypeDictionary.getByName(asJson.getString("craftingType"));
		if (this.craftingType == null) {
			throw new InvalidSaveException("Could not find crafting type by name " + asJson.getString("craftingType"));
		}
		this.craftItemJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("craftItemJobType"));
		if (craftItemJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("craftItemJobType"));
		}
		this.haulingJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("haulingJobType"));
		if (haulingJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("haulingJobType"));
		}
		this.requiresExtraTime = asJson.getBooleanValue("requiresExtraTime");
		this.extraTimeToProcess = asJson.getDouble("extraTimeToProcess");

		this.gameMaterialDictionary = relatedStores.gameMaterialDictionary;

		Long productionAssignmentId = asJson.getLong("currentProductionAssignment");
		if (productionAssignmentId != null) {
			this.currentProductionAssignment = savedGameStateHolder.productionAssignments.get(productionAssignmentId);
			if (this.currentProductionAssignment == null) {
				throw new InvalidSaveException("Could not find production assignment with ID " + productionAssignmentId);
			} else {
				this.currentProductionAssignment.setAssignedCraftingStation(parentEntity);
			}
		}

		Long craftingJobId = asJson.getLong("craftingJob");
		if (craftingJobId != null) {
			this.craftingJob = savedGameStateHolder.jobs.get(craftingJobId);
			if (this.craftingJob == null) {
				throw new InvalidSaveException("Could not find job with ID " + craftingJobId);
			}
		}

		JSONArray inputAllocationsJson = asJson.getJSONArray("inputAllocations");
		if (inputAllocationsJson != null) {
			for (int cursor = 0; cursor < inputAllocationsJson.size(); cursor++) {
				long allocationId = inputAllocationsJson.getLongValue(cursor);
				HaulingAllocation allocation = savedGameStateHolder.haulingAllocations.get(allocationId);
				if (allocation == null) {
					throw new InvalidSaveException("Could not find hauling allocation with ID " + allocationId);
				} else {
					this.haulingInputAllocations.add(allocation);
				}
			}
		}

		JSONArray liquidTransferJobsJson = asJson.getJSONArray("liquidTransferJobs");
		if (liquidTransferJobsJson != null) {
			for (int cursor = 0; cursor < liquidTransferJobsJson.size(); cursor++) {
				Job job = savedGameStateHolder.jobs.get(liquidTransferJobsJson.getLong(cursor));
				if (job == null) {
					throw new InvalidSaveException("Could not find job with ID " + liquidTransferJobsJson.getLong(cursor));
				} else {
					liquidTransferJobs.add(job);
				}
			}
		}

	}
}
