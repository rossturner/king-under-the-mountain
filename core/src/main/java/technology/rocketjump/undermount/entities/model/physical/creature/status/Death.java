package technology.rocketjump.undermount.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.creature.DeathReason;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.CreatureDeathMessage;

public class Death extends StatusEffect {

	private DeathReason deathReason;

	public Death() {
		super(null, 0.0, null);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		if (parentEntity.getType().equals(EntityType.CREATURE)) {
			messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH, new CreatureDeathMessage(parentEntity, deathReason));
		}
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		return false;
	}

	@Override
	public String getI18Key() {
		return "STATUS.DEATH";
	}

	public void setDeathReason(DeathReason deathReason) {
		this.deathReason = deathReason;
	}

	public DeathReason getDeathReason() {
		return deathReason;
	}
}
