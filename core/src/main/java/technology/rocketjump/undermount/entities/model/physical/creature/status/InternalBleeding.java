package technology.rocketjump.undermount.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.model.physical.creature.DeathReason;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.CreatureDeathMessage;

public class InternalBleeding extends StatusEffect {

	private static final float CHANCE_OF_DEATH_ON_TICK = 1f / 35f;

	public InternalBleeding() {
		super(null, 24.0, null);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		if (gameContext.getRandom().nextFloat() < CHANCE_OF_DEATH_ON_TICK) {
			messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH,
					new CreatureDeathMessage(parentEntity, DeathReason.INTERNAL_BLEEDING));
		}
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		return false;
	}

	@Override
	public String getI18Key() {
		return "STATUS.INTERNAL_BLEEDING";
	}

}
