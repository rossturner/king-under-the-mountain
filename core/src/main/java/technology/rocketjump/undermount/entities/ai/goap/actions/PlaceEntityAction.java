package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.behaviour.furniture.CraftingStationBehaviour;
import technology.rocketjump.undermount.entities.behaviour.furniture.MushroomShockTankBehaviour;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HaulingComponent;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.EntityMessage;
import technology.rocketjump.undermount.messaging.types.RequestSoundAssetMessage;
import technology.rocketjump.undermount.messaging.types.RequestSoundMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rooms.HaulingAllocation;
import technology.rocketjump.undermount.rooms.components.StockpileComponent;

import java.util.LinkedList;
import java.util.List;

import static technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation.DOWN;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.undermount.entities.behaviour.furniture.CraftingStationBehaviour.getNearestNavigableWorkspace;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.rooms.HaulingAllocation.AllocationPositionType.FURNITURE;

public class PlaceEntityAction extends Action {
	public PlaceEntityAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		// Should already be in correct tile
		MapTile currentTile = gameContext.getAreaMap().getTile(parent.parentEntity.getLocationComponent().getWorldPosition());
		HaulingAllocation haulingAllocation = parent.getAssignedHaulingAllocation();

		EntityComponent containerComponent = pickContainerComponent(haulingAllocation);
		if (containerComponent == null) {
			Logger.error("Expecting to place item from inventory which can't be found");
			completionType = FAILURE;
			return;
		}

		if (haulingAllocation != null) {
			if (FURNITURE.equals(haulingAllocation.getTargetPositionType())) {
				// Target position should be a workspace of furniture
				Entity targetFurniture = gameContext.getAreaMap().getTile(haulingAllocation.getTargetPosition()).getEntity(haulingAllocation.getTargetId());
				if (targetFurniture == null) {
					Logger.warn("Furniture not found for hauling allocation");
					completionType = FAILURE;
				} else if (hasWorkspaces(targetFurniture)) {
					FurnitureLayout.Workspace nearestNavigableWorkspace = getNearestNavigableWorkspace(targetFurniture, gameContext.getAreaMap(), currentTile.getTilePosition());
					if (nearestNavigableWorkspace == null || !nearestNavigableWorkspace.getAccessedFrom().equals(currentTile.getTilePosition())) {
						Logger.error("Not in nearest workspace for furniture to place item into");
						completionType = FAILURE;
					} else {
						placeEntityInFurniture(containerComponent, targetFurniture, gameContext);
					}
				} else if (adjacentTo(targetFurniture)) {
					placeEntityInFurniture(containerComponent, targetFurniture, gameContext);
				} else {
					// Not adjacent or not in workspace
					completionType = FAILURE;
				}
			} else if (!currentTile.getTilePosition().equals(haulingAllocation.getTargetPosition())) {
				Logger.error("Not in correct tile to place item into");
				completionType = FAILURE;
			} else {
				// In correct tile to place item
				placeEntityIntoTile(containerComponent, currentTile);
			}
		} else {
			// No item allocation, just place item down
			placeEntityIntoTile(containerComponent, currentTile);
		}
	}

	private boolean adjacentTo(Entity targetFurniture) {
		GridPoint2 parentPosition = toGridPoint(parent.parentEntity.getLocationComponent().getWorldOrParentPosition());

		List<GridPoint2> furnitureLocations = new LinkedList<>();
		GridPoint2 furniturePosition = toGridPoint(targetFurniture.getLocationComponent().getWorldOrParentPosition());
		furnitureLocations.add(furniturePosition);

		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) targetFurniture.getPhysicalEntityComponent().getAttributes();
		for (GridPoint2 extraTileOffset : attributes.getCurrentLayout().getExtraTiles()) {
			furnitureLocations.add(furniturePosition.cpy().add(extraTileOffset));
		}

		for (GridPoint2 furnitureLocation : furnitureLocations) {
			if (Math.abs(furnitureLocation.x - parentPosition.x) <= 1 && Math.abs(furnitureLocation.y - parentPosition.y) <= 1) {
				return true;
			}
		}
		return false;
	}

	private boolean hasWorkspaces(Entity targetFurniture) {
		FurnitureEntityAttributes furnitureEntityAttributes = (FurnitureEntityAttributes) targetFurniture.getPhysicalEntityComponent().getAttributes();
		return !furnitureEntityAttributes.getCurrentLayout().getWorkspaces().isEmpty();
	}

	@Override
	public boolean isApplicable() {
		return pickContainerComponent(parent.getAssignedHaulingAllocation()) != null;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state to read
	}

	private void placeEntityInFurniture(EntityComponent currentContainer, Entity targetFurniture, GameContext gameContext) {
		Entity itemToPlace = getTargetFrom(currentContainer);
		if (targetFurniture == null) {
			completionType = FAILURE; // Could not find furniture to place item into
		} else {
			removeTargetFrom(currentContainer);
			InventoryComponent targetInventory = targetFurniture.getOrCreateComponent(InventoryComponent.class);
			targetInventory.add(itemToPlace, targetFurniture, parent.messageDispatcher, gameContext.getGameClock());
			if (targetFurniture.getBehaviourComponent() instanceof CraftingStationBehaviour) {
				CraftingStationBehaviour craftingStationBehaviour = (CraftingStationBehaviour) targetFurniture.getBehaviourComponent();
				craftingStationBehaviour.itemAdded(gameContext.getAreaMap());
			} else if (targetFurniture.getBehaviourComponent() instanceof MushroomShockTankBehaviour) {
				// MODDING expose this, maybe move to MushroomShockTankBehavious (as above for craftingStationBehaviour.itemAdded)
				parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND_ASSET, new RequestSoundAssetMessage("DropItemInWater", (asset) -> {
					if (asset != null) {
						parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(asset, parent.parentEntity));
					}
				}));
			}
			completionType = SUCCESS;
		}
	}

	private void placeEntityIntoTile(EntityComponent containerComponent, MapTile currentTile) {
		Entity entityToPlace = getTargetFrom(containerComponent);
		if (entityToPlace.getType().equals(EntityType.ITEM)) {
			ItemEntityAttributes itemToPlaceAttributes = (ItemEntityAttributes) entityToPlace.getPhysicalEntityComponent().getAttributes();
			int quantityToPlace = itemToPlaceAttributes.getQuantity();
			if (currentTile.hasItem()) {
				Entity itemAlreadyInTile = currentTile.getItemMatching(itemToPlaceAttributes);
				if (itemAlreadyInTile == null) {
					// Non-matching item is in tile
					completionType = FAILURE; // Some other item already in target placement tile
				} else {
					ItemEntityAttributes itemInTileAttributes = (ItemEntityAttributes) itemAlreadyInTile.getPhysicalEntityComponent().getAttributes();
					if (parent.getAssignedHaulingAllocation() != null) {
						quantityToPlace = parent.getAssignedHaulingAllocation().getItemAllocation().getAllocationAmount();
					}
					if (itemInTileAttributes.getQuantity() + quantityToPlace > itemInTileAttributes.getItemType().getMaxStackSize()) {
						// Too many items already in tile, stack size would be exceeded
						quantityToPlace = itemInTileAttributes.getItemType().getMaxStackSize() - itemInTileAttributes.getQuantity();
					}

					// Merge item into tile
					itemInTileAttributes.setQuantity(itemInTileAttributes.getQuantity() + quantityToPlace);
					if (currentTile.hasConstruction() && currentTile.getConstruction().isItemUsedInConstruction(itemAlreadyInTile)) {
						// This item is used as part of this construction, so set it to be fully allocated
						currentTile.getConstruction().placedItemQuantityIncreased(parent.getAssignedHaulingAllocation(), itemAlreadyInTile);
					}
					parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, itemAlreadyInTile);

					itemToPlaceAttributes.setQuantity(itemToPlaceAttributes.getQuantity() - quantityToPlace);

					if (itemToPlaceAttributes.getQuantity() <= 0) {
						removeTargetFrom(containerComponent);
						parent.messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, new EntityMessage(entityToPlace.getId()));
					} else if (parent.getAssignedHaulingAllocation() != null) {
						// decrement allocation amount
						parent.getAssignedHaulingAllocation().getItemAllocation().setAllocationAmount(itemToPlaceAttributes.getQuantity());
					}
					completionType = SUCCESS;
				}
			} else {
				// Doesn't already have an item in tile
				if (!currentTile.isEmpty() && !currentTile.hasConstruction()) {
					// Something else is in tile
					completionType = FAILURE; // Something already in target placement tile
				} else {
					// Place item into tile, including if it is a construction
					removeTargetFrom(containerComponent);
					entityToPlace.getLocationComponent().setWorldPosition(currentTile.getWorldPositionOfCenter().cpy(), false);
					entityToPlace.getLocationComponent().setFacing(DOWN.toVector2());

					itemToPlaceAttributes.setItemPlacement(ItemPlacement.ON_GROUND);

					if (currentTile.hasConstruction() && currentTile.getConstruction().isItemUsedInConstruction(entityToPlace) &&
							parent.getAssignedHaulingAllocation() != null) {
						// This item is used as part of this construction, so set it to be fully allocated
						currentTile.getConstruction().newItemPlaced(parent.getAssignedHaulingAllocation(), entityToPlace);
					}

					parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entityToPlace);
					completionType = SUCCESS;
				}
			}

			if (completionType.equals(SUCCESS) && currentTile.getRoomTile() != null && currentTile.getRoomTile().getRoom().getComponent(StockpileComponent.class) != null) {
				StockpileComponent stockpileComponent = currentTile.getRoomTile().getRoom().getComponent(StockpileComponent.class);
				stockpileComponent.itemPlaced(currentTile, itemToPlaceAttributes, quantityToPlace);
			}

			// Disabling place item sounds as it is getting to be a bit too much
//			if (completionType.equals(SUCCESS) && itemToPlaceAttributes.getItemType().getPlacementSoundAsset() != null) {
//				parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(itemToPlaceAttributes.getItemType().getPlacementSoundAsset(), parent.parentEntity));
//			}

		} else { // not of type ITEM

			// Place entity into tile regardless
			removeTargetFrom(containerComponent);
			entityToPlace.getLocationComponent().setWorldPosition(currentTile.getWorldPositionOfCenter().cpy(), false);
			entityToPlace.getLocationComponent().setFacing(DOWN.toVector2());
			parent.parentEntity.removeComponent(HaulingComponent.class);
			completionType = SUCCESS;
		}
	}

	/**
	 * This method determines if the hauled item is being hauled or is in the inventory
	 */
	private EntityComponent pickContainerComponent(HaulingAllocation haulingAllocation) {
		HaulingComponent haulingComponent = parent.parentEntity.getComponent(HaulingComponent.class);
		if (haulingComponent != null && haulingComponent.getHauledEntity() != null) {
			return haulingComponent;
		}
		InventoryComponent inventoryComponent = parent.parentEntity.getComponent(InventoryComponent.class);
		if (inventoryComponent != null && haulingAllocation != null &&
				haulingAllocation.getHauledEntityId() != null &&
				inventoryComponent.getById(haulingAllocation.getHauledEntityId()) != null) {
			return inventoryComponent;
		}
		return null;
	}

	private Entity getTargetFrom(EntityComponent currentContainer) {
		Entity itemToPlace = null;
		if (currentContainer instanceof HaulingComponent) {
			itemToPlace = ((HaulingComponent)currentContainer).getHauledEntity();
		} else if (currentContainer instanceof InventoryComponent) {
			itemToPlace = ((InventoryComponent)currentContainer).getById(parent.getAssignedHaulingAllocation().getHauledEntityId());
		}
		return itemToPlace;
	}

	private void removeTargetFrom(EntityComponent currentContainer) {
		if (currentContainer instanceof HaulingComponent) {
			((HaulingComponent)currentContainer).clearHauledEntity();
			parent.parentEntity.removeComponent(HaulingComponent.class);
		} else if (currentContainer instanceof InventoryComponent) {
			((InventoryComponent)currentContainer).remove(parent.getAssignedHaulingAllocation().getHauledEntityId());
		}
	}

}
