package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.components.furniture.DecorationInventoryComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.gamecontext.GameContext;

import java.util.Map;

public class DecorationFromInputTag extends Tag {

	@Override
	public String getTagName() {
		return "DECORATION_FROM_INPUT";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return args.size() > 0;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		// Do nothing, this is a special case tag (perhaps shouldn't be a tag?) where items from construction are carried over
	}

	public void apply(Entity entity, Map<Long, Entity> itemsRemovedFromConstruction, ItemTypeDictionary itemTypeDictionary,
					  MessageDispatcher messageDispatcher, GameContext gameContext) {
		DecorationInventoryComponent decorationInventoryComponent = entity.getComponent(DecorationInventoryComponent.class);
		if (decorationInventoryComponent == null) {
			decorationInventoryComponent = new DecorationInventoryComponent();
			decorationInventoryComponent.init(entity, messageDispatcher, gameContext);
			entity.addComponent(decorationInventoryComponent);
		}

		for (String arg : args) {
			ItemType itemType = itemTypeDictionary.getByName(arg);
			if (itemType != null) {
				Entity match = getMatching(itemType, itemsRemovedFromConstruction);
				if (match != null) {
					Entity cloned = match.clone(messageDispatcher, gameContext);
					cloned.getLocationComponent().setUntracked(true);
					decorationInventoryComponent.add(cloned);
				} else {
					Logger.error("Could not find matching " + itemType.getItemTypeName() + " while processing " + this.getClass().getSimpleName());
				}
			} else {
				Logger.error("Could not find item type by name " + arg + " in " + this.getClass().getSimpleName());
			}
		}
	}

	public static Entity getMatching(ItemType itemType, Map<Long, Entity> itemsRemovedFromConstruction) {
		for (Entity entity : itemsRemovedFromConstruction.values()) {
			if (entity.getType().equals(EntityType.ITEM)) {
				ItemEntityAttributes itemEntityAttributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (itemEntityAttributes.getItemType().equals(itemType)) {
					return entity;
				}
			}
		}
		return null;
	}

}
