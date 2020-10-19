package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.ai.goap.GoalSelectionCondition;
import technology.rocketjump.undermount.entities.ai.goap.GoalSelector;
import technology.rocketjump.undermount.entities.ai.goap.QueuedGoal;
import technology.rocketjump.undermount.entities.behaviour.humanoids.SettlerBehaviour;
import technology.rocketjump.undermount.entities.components.humanoid.MemoryComponent;
import technology.rocketjump.undermount.entities.components.humanoid.NeedsComponent;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class RequeueThisGoalAction extends Action {
	public RequeueThisGoalAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public boolean isInterruptible() {
		return false;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		SettlerBehaviour parentBehaviour = (SettlerBehaviour) parent.parentEntity.getBehaviourComponent();
		NeedsComponent needsComponent = parent.parentEntity.getComponent(NeedsComponent.class);
		MemoryComponent memoryComponent = parent.parentEntity.getComponent(MemoryComponent.class);


		for (GoalSelector selector : parent.goal.getSelectors()) {
			boolean allConditionsApply = true;
			for (GoalSelectionCondition condition : selector.conditions) {
				if (!condition.apply(gameContext.getGameClock(), needsComponent, memoryComponent)) {
					allConditionsApply = false;
					break;
				}
			}
			if (allConditionsApply) {
				parentBehaviour.getGoalQueue().add(new QueuedGoal(parent.goal, selector.scheduleCategory, selector.priority, gameContext.getGameClock()));
				completionType = CompletionType.SUCCESS;
				return;
			}
		}

		completionType = CompletionType.FAILURE;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state to read
	}
}
