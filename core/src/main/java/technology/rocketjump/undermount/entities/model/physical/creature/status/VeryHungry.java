package technology.rocketjump.undermount.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.ai.goap.EntityNeed;
import technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent;
import technology.rocketjump.undermount.entities.components.humanoid.NeedsComponent;
import technology.rocketjump.undermount.entities.model.physical.creature.DeathReason;
import technology.rocketjump.undermount.gamecontext.GameContext;

public class VeryHungry extends StatusEffect {

	/**
	 * Note that this will get re-added to a humanoid when DyingOfHunger kicks in, but the happiness modifier will only apply for DyingOfHunger
	 */
	public VeryHungry() {
		super(DyingOfHunger.class, 16.0, DeathReason.STARVATION);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		parentEntity.getComponent(HappinessComponent.class).add(HappinessComponent.HappinessModifier.VERY_HUNGRY);
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		NeedsComponent needsComponent = parentEntity.getComponent(NeedsComponent.class);
		if (needsComponent == null) {
			return true;
		} else {
			return needsComponent.getValue(EntityNeed.FOOD) > 1;
		}
	}

	@Override
	public String getI18Key() {
		return "STATUS.VERY_HUNGRY";
	}

}
