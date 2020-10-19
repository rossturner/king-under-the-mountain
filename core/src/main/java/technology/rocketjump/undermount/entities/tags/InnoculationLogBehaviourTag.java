package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.behaviour.furniture.InnoculationLogBehaviour;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.gamecontext.GameContext;

import java.util.ArrayList;
import java.util.List;

public class InnoculationLogBehaviourTag extends Tag {

	@Override
	public String getTagName() {
		return "INNOCULATION_LOG_BEHAVIOUR";
	}

	@Override
	public boolean isValid() {
		return true; // TODO implement this
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (entity.getBehaviourComponent() == null) {
			// Don't apply to furniture which already doesn't have a BehaviourComponent e.g. when placing from UI
			return;
		}

		if (!entity.getBehaviourComponent().getClass().equals(InnoculationLogBehaviour.class)) {
			// Only switch behaviour if already different
			InnoculationLogBehaviour behaviourComponent = new InnoculationLogBehaviour();

			List<ItemType> relatedItemTypes = new ArrayList<>();
			ItemType itemType = tagProcessingUtils.itemTypeDictionary.getByName(args.get(0));
			if (itemType == null) {
				Logger.error("Could not find item type " + args.get(0) + " specified in " + getTagName() + " tag");
			} else {
				relatedItemTypes.add(itemType);
			}
			behaviourComponent.setRelatedItemTypes(relatedItemTypes);

			FurnitureType transformToFurnitureType = tagProcessingUtils.furnitureTypeDictionary.getByName(args.get(1));
			if (transformToFurnitureType == null) {
				Logger.error("Could not find furniture type " + args.get(1) + " specified in " + getTagName() + " tag");
			} else {
				behaviourComponent.setTransformToFurnitureType(transformToFurnitureType);
			}

			behaviourComponent.setHaulingJobType(tagProcessingUtils.jobTypeDictionary.getByName("HAULING"));
			behaviourComponent.setInnoculationJobType(tagProcessingUtils.jobTypeDictionary.getByName("MUSHROOM_INNOCULATION"));
			behaviourComponent.init(entity, messageDispatcher, gameContext);

			entity.replaceBehaviourComponent(behaviourComponent);
		}
	}
}
