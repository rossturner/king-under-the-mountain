package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class EquipItemFromFoodAllocationAction extends Action {

	public EquipItemFromFoodAllocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (parent.getFoodAllocation() == null || parent.getFoodAllocation().getTargetEntity() == null) {
			completionType = FAILURE;
		} else {
			InventoryComponent inventoryComponent = parent.parentEntity.getOrCreateComponent(InventoryComponent.class);
			Entity itemInInventory = inventoryComponent.remove(parent.getFoodAllocation().getTargetEntity().getId());
			if (itemInInventory == null) {
				completionType = FAILURE;
			} else {
				EquippedItemComponent equippedItemComponent = parent.parentEntity.getOrCreateComponent(EquippedItemComponent.class);
				equippedItemComponent.setEquippedItem(itemInInventory, parent.parentEntity, parent.messageDispatcher);
				completionType = SUCCESS;
			}
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state to write
	}
}
