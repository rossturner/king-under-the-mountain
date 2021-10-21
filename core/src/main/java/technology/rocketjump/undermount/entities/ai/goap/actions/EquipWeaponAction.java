package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.WeaponSelectionComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.Optional;

import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class EquipWeaponAction extends Action {

	public EquipWeaponAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		InventoryComponent inventoryComponent = parent.parentEntity.getOrCreateComponent(InventoryComponent.class);
		EquippedItemComponent equippedItemComponent = parent.parentEntity.getOrCreateComponent(EquippedItemComponent.class);
		WeaponSelectionComponent weaponSelectionComponent = parent.parentEntity.getOrCreateComponent(WeaponSelectionComponent.class);

		Entity currentlyEquipped = equippedItemComponent.clearEquippedItem();
		if (currentlyEquipped != null) {
			inventoryComponent.add(currentlyEquipped, parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());
		}

		Optional<ItemType> selectedWeapon = weaponSelectionComponent.getSelectedWeapon();

		if (selectedWeapon.isEmpty()) {
			completionType = SUCCESS;
		} else {
			InventoryComponent.InventoryEntry inventoryEntry = inventoryComponent.findByItemType(selectedWeapon.get(), gameContext.getGameClock());
			if (inventoryEntry == null) {
				completionType = FAILURE;
			} else {
				inventoryComponent.remove(inventoryEntry.entity.getId());
				equippedItemComponent.setEquippedItem(inventoryEntry.entity, parent.parentEntity, parent.messageDispatcher);
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
		// No state to read
	}

}
