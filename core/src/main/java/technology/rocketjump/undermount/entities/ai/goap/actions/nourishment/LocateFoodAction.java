package technology.rocketjump.undermount.entities.ai.goap.actions.nourishment;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.cooking.model.FoodAllocation;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.ai.goap.actions.Action;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.FoodAllocationRequestMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class LocateFoodAction extends Action implements FoodAllocationRequestMessage.FoodAllocationCallback {

	boolean requestSent = false;

	public LocateFoodAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (!requestSent) {
			parent.messageDispatcher.dispatchMessage(MessageType.FOOD_ALLOCATION_REQUESTED, new FoodAllocationRequestMessage(parent.parentEntity, this));
			requestSent = true;
		}
	}

	@Override
	public void foodAssigned(FoodAllocation allocation) {
		if (allocation == null) {
			completionType = CompletionType.FAILURE;
		} else {
			parent.setFoodAllocation(allocation);
			completionType = CompletionType.SUCCESS;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// Do nothing, just request again next time, as this should currently be synchronous
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {

	}
}
