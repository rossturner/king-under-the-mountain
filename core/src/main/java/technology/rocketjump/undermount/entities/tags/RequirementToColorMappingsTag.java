package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.gamecontext.GameContext;

import java.util.HashMap;
import java.util.Map;

public class RequirementToColorMappingsTag extends Tag {

	@Override
	public String getTagName() {
		return "REQUIREMENT_TO_COLOR_MAPPINGS";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return args.size() % 2 == 0;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		// Do nothing, this is special case tag applied by ConstructionMessageHandler
	}

	public void apply(Entity createdFurnitureEntity, Map<Long, Entity> itemsRemovedFromConstruction, ItemTypeDictionary itemTypeDictionary) {
		Map<ItemType, ColoringLayer> configuration = new HashMap<>();

		for (int configCursor = 0; configCursor < args.size(); configCursor += 2) {
			ItemType itemType = itemTypeDictionary.getByName(args.get(configCursor));
			ColoringLayer coloringLayer =  ColoringLayer.valueOf(args.get(configCursor + 1));

			if (itemType == null) {
				Logger.error("Unrecognised item type in " + this.getClass().getSimpleName() + " with name " + args.get(configCursor));
			} else  if (!EnumUtils.isValidEnum(ColoringLayer.class, args.get(configCursor + 1))) {
					Logger.error("Unrecognised " + ColoringLayer.class.getSimpleName() + " in " + this.getClass().getSimpleName() + " with name " + args.get(configCursor + 1));
			} else {
				configuration.put(itemType, coloringLayer);
			}
		}

		FurnitureEntityAttributes targetAttributes = (FurnitureEntityAttributes) createdFurnitureEntity.getPhysicalEntityComponent().getAttributes();

		for (Map.Entry<ItemType, ColoringLayer> configEntry : configuration.entrySet()) {
			Entity matchingEntity = DecorationFromInputTag.getMatching(configEntry.getKey(), itemsRemovedFromConstruction);
			if (matchingEntity == null) {
				Logger.warn("Could not find matching entity with item type " + configEntry.getKey().getItemTypeName() + " in " + this.getClass().getSimpleName());
			} else {
				ItemEntityAttributes attributes = (ItemEntityAttributes) matchingEntity.getPhysicalEntityComponent().getAttributes();
				targetAttributes.setColor(configEntry.getValue(), attributes.getPrimaryMaterial().getColor());
			}
		}
	}
}
