package technology.rocketjump.undermount.entities.behaviour.furniture;

import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RequestHaulingMessage;

public class DestroyWhenInventoryEmptyBehaviour extends FurnitureBehaviour implements Prioritisable {

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);

		InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
		if (inventoryComponent.isEmpty()) {
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, parentEntity);
		} else {
			for (InventoryComponent.InventoryEntry entry : inventoryComponent.getInventoryEntries()) {
				if (entry.entity.getType().equals(EntityType.ITEM)) {
					ItemAllocationComponent itemAllocationComponent = entry.entity.getOrCreateComponent(ItemAllocationComponent.class);
					if (itemAllocationComponent.getNumUnallocated() > 0) {
						messageDispatcher.dispatchMessage(MessageType.REQUEST_ENTITY_HAULING, new RequestHaulingMessage(entry.entity, parentEntity, false, priority, null));
					}
				} else {
					Logger.warn("To be implemented: Handle non-item type inventory items in " + this.getClass().getSimpleName());
				}
			}

		}
	}
}
