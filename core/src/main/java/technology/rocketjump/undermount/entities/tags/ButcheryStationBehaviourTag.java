package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.behaviour.furniture.ButcheryStationBehaviour;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;

public class ButcheryStationBehaviourTag extends Tag {

	@Override
	public String getTagName() {
		return "BUTCHERY_STATION_BEHAVIOUR";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return true;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (entity.getBehaviourComponent() == null) {
			// Don't apply to furniture which already doesn't have a BehaviourComponent e.g. when placing from UI
			return;
		}

		if (!entity.getBehaviourComponent().getClass().equals(ButcheryStationBehaviour.class)) {
			// Only switch behaviour if already different
			ButcheryStationBehaviour newBehaviour = new ButcheryStationBehaviour();
			newBehaviour.setHaulingJobType(tagProcessingUtils.jobTypeDictionary.getByName("HAULING"));
			newBehaviour.setButcheryJobType(tagProcessingUtils.jobTypeDictionary.getByName("BUTCHER_CREATURE"));
			newBehaviour.setRequiredProfession(tagProcessingUtils.professionDictionary.getByName("CHEF"));
			newBehaviour.init(entity, messageDispatcher, gameContext);
			entity.replaceBehaviourComponent(newBehaviour);
		}
	}

}
