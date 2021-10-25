package technology.rocketjump.undermount.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.model.physical.creature.Consciousness;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;

public class KnockedUnconscious extends StatusEffect {


	public KnockedUnconscious() {
		super(null, 1.0, null);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		if (attributes.getConsciousness().equals(Consciousness.AWAKE)) {
			attributes.setConsciousness(Consciousness.KNOCKED_UNCONSCIOUS);
			messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parentEntity);
		}
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		return false; // removed by hoursUntilNextStage expiry
	}

}
