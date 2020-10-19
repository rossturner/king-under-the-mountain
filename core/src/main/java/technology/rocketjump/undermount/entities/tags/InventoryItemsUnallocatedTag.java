package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;

public class InventoryItemsUnallocatedTag extends Tag {
	@Override
	public String getTagName() {
		return "INVENTORY_ITEMS_UNALLOCATED";
	}

	@Override
	public boolean isValid() {
		return true; // No args
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		InventoryComponent inventoryComponent = entity.getOrCreateComponent(InventoryComponent.class);
		inventoryComponent.setItemsUnallocated(true);
	}

}
