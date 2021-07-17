package technology.rocketjump.undermount.entities.model.physical.humanoid.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.model.physical.humanoid.DeathReason;
import technology.rocketjump.undermount.gamecontext.GameContext;

public class OnFireStatus extends StatusEffect {

	public OnFireStatus() {
		super(Death.class, 1.0, DeathReason.BURNING);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {

	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		return false;
	}

}
