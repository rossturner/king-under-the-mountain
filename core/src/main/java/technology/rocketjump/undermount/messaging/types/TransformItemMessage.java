package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;

public class TransformItemMessage {

	public final Entity itemEntity;
	public final ItemType transformToItemType;

	public TransformItemMessage(Entity itemEntity, ItemType transformToItemType) {
		this.itemEntity = itemEntity;
		this.transformToItemType = transformToItemType;
	}

}
