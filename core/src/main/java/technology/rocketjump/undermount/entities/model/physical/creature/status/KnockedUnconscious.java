package technology.rocketjump.undermount.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.undermount.entities.behaviour.creature.SettlerBehaviour;
import technology.rocketjump.undermount.entities.model.physical.creature.Consciousness;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;

import static technology.rocketjump.undermount.entities.ai.goap.actions.SleepOnFloorAction.showAsRotatedOnSide;

public class KnockedUnconscious extends StatusEffect {


	public KnockedUnconscious() {
		super(null, 1.0, null);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		if (attributes.getConsciousness().equals(Consciousness.AWAKE)) {
			attributes.setConsciousness(Consciousness.KNOCKED_UNCONSCIOUS);

			if (parentEntity.isJobAssignable()) {
				showAsRotatedOnSide(parentEntity, gameContext);
			}

			if (parentEntity.getBehaviourComponent() instanceof SettlerBehaviour) {
				SettlerBehaviour settlerBehaviour = (SettlerBehaviour) parentEntity.getBehaviourComponent();
				if (settlerBehaviour.getCurrentGoal() != null) {
					settlerBehaviour.getCurrentGoal().setInterrupted(true);
				}
			}
			if (parentEntity.getBehaviourComponent() instanceof CreatureBehaviour) {
				CreatureBehaviour creatureBehaviour = (CreatureBehaviour) parentEntity.getBehaviourComponent();
				if (creatureBehaviour.getCurrentGoal() != null) {
					creatureBehaviour.getCurrentGoal().setInterrupted(true);
				}
			}

			messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parentEntity);
		}
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		return false; // removed by hoursUntilNextStage expiry
	}

	@Override
	public String getI18Key() {
		return "STATUS.KNOCKED_UNCONSCIOUS";
	}

}
