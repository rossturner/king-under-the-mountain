package technology.rocketjump.undermount.entities.behaviour.furniture;

import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.ai.goap.actions.EntityCreatedCallback;
import technology.rocketjump.undermount.entities.components.LiquidAllocation;
import technology.rocketjump.undermount.entities.components.LiquidContainerComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.ItemCreationRequestMessage;
import technology.rocketjump.undermount.messaging.types.ItemPrimaryMaterialChangedMessage;

public class EdibleLiquidSourceBehaviour extends FurnitureBehaviour implements EntityCreatedCallback {

	private transient Entity createdItem;

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);
		LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);
		if (liquidContainerComponent != null) {
			if (liquidContainerComponent.getLiquidQuantity() <= 0) {
				liquidContainerComponent.setTargetLiquidMaterial(null);
				messageDispatcher.dispatchMessage(MessageType.REQUEST_FURNITURE_REMOVAL, parentEntity);
				parentEntity.replaceBehaviourComponent(null);
				messageDispatcher.dispatchMessage(MessageType.FURNITURE_PLACEMENT, parentEntity);
			}
		}
	}

	public Entity createItem(LiquidAllocation allocationToCreateFrom, GameContext gameContext) {
		if (relatedItemTypes.isEmpty() || relatedItemTypes.get(0) == null) {
			Logger.error("No item type specified to create in " + this.getClass().getSimpleName());
			return null;
		}


		LiquidContainerComponent parentLiquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);
		LiquidAllocation success = parentLiquidContainerComponent.cancelAllocationAndDecrementQuantity(allocationToCreateFrom);
		if (success == null) {
			return null;
		}

		messageDispatcher.dispatchMessage(MessageType.ITEM_CREATION_REQUEST, new ItemCreationRequestMessage(relatedItemTypes.get(0), this));
		if (createdItem == null) {
			return null;
		}
		GameMaterial oldPrimaryMaterial = ((ItemEntityAttributes) createdItem.getPhysicalEntityComponent().getAttributes()).getPrimaryMaterial();

		LiquidContainerComponent itemLiquidContainerComponent = new LiquidContainerComponent();
		itemLiquidContainerComponent.init(createdItem, messageDispatcher, gameContext);
		createdItem.addComponent(itemLiquidContainerComponent);


		itemLiquidContainerComponent.setTargetLiquidMaterial(parentLiquidContainerComponent.getTargetLiquidMaterial());
		itemLiquidContainerComponent.setLiquidQuantity(1f);

		if (!oldPrimaryMaterial.equals(((ItemEntityAttributes)createdItem.getPhysicalEntityComponent().getAttributes()).getPrimaryMaterial())) {
			// Tracker needs updating due to change in material
			messageDispatcher.dispatchMessage(MessageType.ITEM_PRIMARY_MATERIAL_CHANGED, new ItemPrimaryMaterialChangedMessage(createdItem, oldPrimaryMaterial));
		}
		return createdItem;
	}

	@Override
	public void entityCreated(Entity entity) {
		this.createdItem = entity;
	}
}
