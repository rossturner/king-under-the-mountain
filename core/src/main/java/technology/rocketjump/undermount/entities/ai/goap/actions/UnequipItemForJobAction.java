package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.furniture.DecorationInventoryComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.humanoid.EquippedItemComponent;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class UnequipItemForJobAction extends Action {
	public UnequipItemForJobAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public boolean isInterruptible() {
		return false;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		EquippedItemComponent equippedItemComponent = parent.parentEntity.getOrCreateComponent(EquippedItemComponent.class);
		Entity currentItem = equippedItemComponent.clearEquippedItem();
		if (currentItem != null) {
			if (new EquipItemForJobFromFurnitureAction(parent).isApplicable()) {
				unequipItemToFurniture(currentItem, gameContext);
			} else {
				unequipItemToInventory(currentItem, gameContext);
			}
		}
		completionType = SUCCESS;
	}

	private void unequipItemToFurniture(Entity currentItem, GameContext gameContext) {
		if (currentItem != null) {
			EquipItemForJobFromFurnitureAction.getTargetFurniture(gameContext, parent)
					.ifPresentOrElse(furniture -> {
						DecorationInventoryComponent decorationInventoryComponent = furniture.getOrCreateComponent(DecorationInventoryComponent.class);
						decorationInventoryComponent.add(currentItem);
						((ItemEntityAttributes)currentItem.getPhysicalEntityComponent().getAttributes()).setItemPlacement(ItemPlacement.ON_GROUND);
						currentItem.getLocationComponent().setOrientation(EntityAssetOrientation.DOWN);
						parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, currentItem);
					}, () -> {
						unequipItemToInventory(currentItem, gameContext);
						// Change entity to be tracked and add to own inventory
						currentItem.getLocationComponent().setUntracked(false);
						parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, currentItem);
					});
		}
	}

	private void unequipItemToInventory(Entity currentItem, GameContext gameContext) {
		if (currentItem != null) {
			InventoryComponent inventoryComponent = parent.parentEntity.getOrCreateComponent(InventoryComponent.class);
			inventoryComponent.add(currentItem, parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state to read
	}

}
