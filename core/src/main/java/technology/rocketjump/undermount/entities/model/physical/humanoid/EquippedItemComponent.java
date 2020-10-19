package technology.rocketjump.undermount.entities.model.physical.humanoid;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.EntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;

import static technology.rocketjump.undermount.entities.components.ItemAllocation.Purpose.EQUIPPED;

public class EquippedItemComponent implements EntityComponent {

	private Entity equippedItem;

	public Entity getEquippedItem() {
		return equippedItem;
	}

	public void setEquippedItem(Entity itemToEquip, Entity parentEntity, MessageDispatcher messageDispatcher) {
		this.equippedItem = itemToEquip;
		this.equippedItem.getLocationComponent().setContainerEntity(parentEntity);

		EntityAttributes attributes = itemToEquip.getPhysicalEntityComponent().getAttributes();
		if (attributes instanceof ItemEntityAttributes) {
			ItemEntityAttributes itemAttributes = (ItemEntityAttributes) attributes;
			itemAttributes.setItemPlacement(ItemPlacement.BEING_CARRIED);
			equippedItem.getLocationComponent().setOrientation(parentEntity.getLocationComponent().getOrientation());
			messageDispatcher.dispatchMessage(null, MessageType.ENTITY_ASSET_UPDATE_REQUIRED, itemToEquip);

			ItemAllocationComponent itemAllocationComponent = itemToEquip.getOrCreateComponent(ItemAllocationComponent.class);
			if (itemAllocationComponent.getNumAllocated() == 0) {
				itemAllocationComponent.createAllocation(itemAttributes.getQuantity(), parentEntity, EQUIPPED);
			} else {
				Logger.error("Equipping item which already has some allocations");
				if (GlobalSettings.DEV_MODE) {
					throw new RuntimeException("This must be fixed");
				}
			}
		}
	}

	public Entity clearEquippedItem() {
		if (this.equippedItem != null) {
			Entity equippedItem = this.equippedItem;
			equippedItem.getLocationComponent().setContainerEntity(null);
			this.equippedItem = null;
			if (equippedItem.getType().equals(EntityType.ITEM)) {
				ItemAllocationComponent itemAllocationComponent = equippedItem.getOrCreateComponent(ItemAllocationComponent.class);
				itemAllocationComponent.cancelAll(EQUIPPED);
			}
			return equippedItem;
		} else {
			return null;
		}
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		EquippedItemComponent clonedComponent = new EquippedItemComponent();
		if (equippedItem != null) {
			clonedComponent.setEquippedItem(equippedItem.clone(messageDispatcher, gameContext), equippedItem.getLocationComponent().getContainerEntity(), messageDispatcher);
		}
		return clonedComponent;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (equippedItem != null) {
			equippedItem.writeTo(savedGameStateHolder);
			asJson.put("equippedItem", equippedItem.getId());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		Long equippedItemId = asJson.getLong("equippedItem");
		if (equippedItemId != null) {
			this.equippedItem = savedGameStateHolder.entities.get(equippedItemId);
			if (this.equippedItem == null) {
				throw new InvalidSaveException("Could not find entity with ID " + equippedItemId);
			}
		}
	}
}
