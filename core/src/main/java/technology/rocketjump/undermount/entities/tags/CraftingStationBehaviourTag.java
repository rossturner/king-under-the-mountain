package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.behaviour.furniture.CraftingStationBehaviour;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.jobs.model.JobType;

import java.util.ArrayList;
import java.util.List;

public class CraftingStationBehaviourTag extends Tag {

	public static final String CRAFTING_STATION_BEHAVIOUR_TAGNAME = "CRAFTING_STATION_BEHAVIOUR";

	@Override
	public String getTagName() {
		return CRAFTING_STATION_BEHAVIOUR_TAGNAME;
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		/**
		 * Tag params are:
		 * 0: CraftingType
		 * 1: Crafting job type
		 * 2: Hauling job type
		 * 3+: related item types
		 */
		return args.size() >= 3;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (entity.getBehaviourComponent() == null) {
			// Don't apply to furniture which already doesn't have a BehaviourComponent e.g. when placing from UI
			return;
		} else if (!entity.getBehaviourComponent().getClass().equals(CraftingStationBehaviour.class)) {
			// Only switch behaviour if already different
			List<ItemType> relatedItemTypes = new ArrayList<>();

			CraftingType craftingType = tagProcessingUtils.craftingTypeDictionary.getByName(args.get(0));
			if (craftingType == null) {
				Logger.error("Could not find crafting type with name " + args.get(0) + " for " + this.getTagName());
			}
			JobType craftItemJobType = tagProcessingUtils.jobTypeDictionary.getByName(args.get(1));
			if (craftItemJobType == null) {
				Logger.error("Could not find job type with name " + args.get(1) + " for " + this.getTagName());
			}
			JobType haulingJobType = tagProcessingUtils.jobTypeDictionary.getByName(args.get(2));
			if (haulingJobType == null) {
				Logger.error("Could not find job type with name " + args.get(2) + " for " + this.getTagName());
			}

			if (args.size() > 3) {
				for (int cursor = 3; cursor < args.size(); cursor++) {
					ItemType itemType = tagProcessingUtils.itemTypeDictionary.getByName(args.get(cursor));
					if (itemType == null) {
						Logger.error("Could not find related item type " + args.get(cursor) + " for " + this.getTagName());
					} else {
						relatedItemTypes.add(itemType);
					}
				}
			}

			CraftingStationBehaviour craftingStationBehaviour = new CraftingStationBehaviour(craftingType,
					craftItemJobType, haulingJobType,
					tagProcessingUtils.materialDictionary);
			craftingStationBehaviour.init(entity, messageDispatcher, gameContext);
			craftingStationBehaviour.setRelatedItemTypes(relatedItemTypes);
			entity.replaceBehaviourComponent(craftingStationBehaviour);
		}
	}

}
