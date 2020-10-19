package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.ai.goap.actions.EntityCreatedCallback;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;

public class ItemCreationRequestMessage {

	private final ItemType requiredItemType;
	private final ItemEntityAttributes attributes;
	private final EntityCreatedCallback callback;
	private final boolean addToGameContext;

	public ItemCreationRequestMessage(ItemType requiredItemType, EntityCreatedCallback callback) {
		this.requiredItemType = requiredItemType;
		this.attributes = null;
		this.callback = callback;
		this.addToGameContext = true;
	}

	public ItemCreationRequestMessage(ItemType requiredItemType, boolean addToGameContext, EntityCreatedCallback callback) {
		this.requiredItemType = requiredItemType;
		this.attributes = null;
		this.callback = callback;
		this.addToGameContext = addToGameContext;
	}

	public ItemCreationRequestMessage(ItemEntityAttributes attributes, EntityCreatedCallback callback) {
		this.requiredItemType = null;
		this.attributes = attributes;
		this.callback = callback;
		this.addToGameContext = true;
	}

	public ItemEntityAttributes getAttributes() {
		return attributes;
	}

	public ItemType getRequiredItemType() {
		return requiredItemType;
	}

	public EntityCreatedCallback getCallback() {
		return callback;
	}

	public boolean isAddToGameContext() {
		return addToGameContext;
	}
}
