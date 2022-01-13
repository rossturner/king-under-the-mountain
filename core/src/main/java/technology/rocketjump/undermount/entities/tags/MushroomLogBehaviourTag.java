package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.behaviour.furniture.MushroomLogBehaviour;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.gamecontext.GameContext;

import java.util.List;

public class MushroomLogBehaviourTag extends Tag {

	@Override
	public String getTagName() {
		return "MUSHROOM_LOG_BEHAVIOUR";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return true; // TODO implement this
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (entity.getBehaviourComponent() == null) {
			// Don't apply to furniture which already doesn't have a BehaviourComponent e.g. when placing from UI
			return;
		}

		if (!entity.getBehaviourComponent().getClass().equals(MushroomLogBehaviour.class)) {
			// Only switch behaviour if already different
			MushroomLogBehaviour behaviourComponent = new MushroomLogBehaviour();

//			List<ItemType> relatedItemTypes = new ArrayList<>();
//			ItemType itemType = tagProcessingUtils.itemTypeDictionary.getByName(args.get(0));
//			if (itemType == null) {
//				Logger.error("Could not find item type " + args.get(0) + " specified in " + getTagName() + " tag");
//			} else {
//				relatedItemTypes.add(itemType);
//			}
//			behaviourComponent.setRelatedItemTypes(relatedItemTypes);

			FurnitureType relatedFurnitureType = tagProcessingUtils.furnitureTypeDictionary.getByName(args.get(0));
			if (relatedFurnitureType != null) {
				behaviourComponent.setRelatedFurnitureTypes(List.of(relatedFurnitureType));
			}

			behaviourComponent.setHarvestJobType(tagProcessingUtils.jobTypeDictionary.getByName("HARVEST_FROM_FURNITURE"));

			behaviourComponent.init(entity, messageDispatcher, gameContext);

			entity.replaceBehaviourComponent(behaviourComponent);
		}
	}
}
