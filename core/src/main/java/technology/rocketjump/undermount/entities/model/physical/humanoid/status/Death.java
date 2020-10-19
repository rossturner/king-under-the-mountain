package technology.rocketjump.undermount.entities.model.physical.humanoid.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.model.physical.humanoid.DeathReason;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.HumanoidDeathMessage;

public class Death extends StatusEffect {

	private DeathReason deathReason;

	public Death() {
		super(null, 0, null);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		messageDispatcher.dispatchMessage(MessageType.HUMANOID_DEATH, new HumanoidDeathMessage(parentEntity, deathReason));
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		return false;
	}

	public void setDeathReason(DeathReason deathReason) {
		this.deathReason = deathReason;
	}

	public DeathReason getDeathReason() {
		return deathReason;
	}
}
