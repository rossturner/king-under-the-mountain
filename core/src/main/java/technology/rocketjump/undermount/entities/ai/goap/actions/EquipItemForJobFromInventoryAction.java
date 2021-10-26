package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class EquipItemForJobFromInventoryAction extends Action {

	public EquipItemForJobFromInventoryAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		// See if item is in inventory to equip
		ItemType requiredItemType = parent.getAssignedJob().getRequiredItemType();
		GameMaterial requiredMaterial = parent.getAssignedJob().getRequiredItemMaterial();
		InventoryComponent inventoryComponent = parent.parentEntity.getOrCreateComponent(InventoryComponent.class);
		InventoryComponent.InventoryEntry itemInInventory;
		if (requiredMaterial != null) {
			itemInInventory = inventoryComponent.findByItemTypeAndMaterial(requiredItemType, requiredMaterial, gameContext.getGameClock());
		} else {
			itemInInventory = inventoryComponent.findByItemType(requiredItemType, gameContext.getGameClock());
		}
		if (itemInInventory != null) {
			inventoryComponent.remove(itemInInventory.entity.getId());
			EquippedItemComponent equippedItemComponent = parent.parentEntity.getOrCreateComponent(EquippedItemComponent.class);
			equippedItemComponent.setEquippedItem(itemInInventory.entity, parent.parentEntity, parent.messageDispatcher);
			completionType = SUCCESS;
		} else {
			// Interrupt entire goal so we don't then GoToLocation
			parent.setInterrupted(true);
			completionType = FAILURE;
		}
	}

	@Override
	public boolean isApplicable(GameContext gameContext) {
		if (parent.getAssignedJob() != null && parent.getAssignedJob().getCraftingRecipe() != null && parent.getAssignedJob().getCraftingRecipe().getCraftingType().isUsesWorkstationTool()) {
			return false;
		} else if (parent.getAssignedJob() != null && parent.getAssignedJob().getRequiredItemType() != null) {
			return parent.getAssignedJob().getRequiredItemType().isEquippedWhileWorkingOnJob();
		} else {
			return false;
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
