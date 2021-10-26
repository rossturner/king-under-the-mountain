package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.creature.Consciousness;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.DeathReason;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.CreatureDeathMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class DeliverKillingBlowAction extends Action {

	public DeliverKillingBlowAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		Entity targetEntity = gameContext.getEntities().get(parent.getAssignedJob().getTargetId());

		if (targetEntity == null) {
			completionType = CompletionType.FAILURE;
		} else if (targetEntity.getType().equals(EntityType.CREATURE)) {
			float distanceToTarget = targetEntity.getLocationComponent().getWorldOrParentPosition().dst(parent.parentEntity.getLocationComponent().getWorldOrParentPosition());
			if (distanceToTarget > 2 * targetEntity.getLocationComponent().getRadius()) {
				completionType = CompletionType.FAILURE; // too far away
			} else {
				CreatureEntityAttributes attributes = (CreatureEntityAttributes) targetEntity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getConsciousness().equals(Consciousness.KNOCKED_UNCONSCIOUS)) {
					parent.messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH,
							new CreatureDeathMessage(targetEntity, DeathReason.CRITICAL_ORGAN_DAMAGE));
					completionType = CompletionType.SUCCESS;
				} else {
					completionType = CompletionType.FAILURE;
				}
			}
		} else {
			completionType = CompletionType.FAILURE;
			Logger.error(String.format("Don't know how to deliver killing blow to %S type entity", targetEntity.getType()));
		}
	}

	@Override
	public boolean isApplicable(GameContext gameContext) {
		Entity targetEntity = gameContext.getEntities().get(parent.getAssignedJob().getTargetId());
		return targetEntity != null && targetEntity.getType().equals(EntityType.CREATURE) &&
				((CreatureEntityAttributes)targetEntity.getPhysicalEntityComponent().getAttributes()).getConsciousness().equals(Consciousness.KNOCKED_UNCONSCIOUS);
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}

}
