package technology.rocketjump.undermount.entities.ai.goap.actions.nourishment;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.ai.goap.actions.Action;
import technology.rocketjump.undermount.entities.components.LiquidAllocation;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RequestLiquidAllocationMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.Optional;

import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.misc.VectorUtils.toVector;

public class LocateDrinkAction extends Action implements RequestLiquidAllocationMessage.LiquidAllocationCallback {

	public static final float LIQUID_AMOUNT_FOR_DRINK_CONSUMPTION = 0.15f;
	private GameContext gameContext;

	public LocateDrinkAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		this.gameContext = gameContext;

		if (completionType == null) {
			parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_LIQUID_ALLOCATION, new RequestLiquidAllocationMessage(
					parent.parentEntity, LIQUID_AMOUNT_FOR_DRINK_CONSUMPTION,
					true, true, this));

			// Expecting above code to have found an alcoholic drink first
			if (completionType != null && completionType.equals(FAILURE)) {
				parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_LIQUID_ALLOCATION, new RequestLiquidAllocationMessage(
						parent.parentEntity, LIQUID_AMOUNT_FOR_DRINK_CONSUMPTION,
						false, true, this));
			}
		}

	}

	@Override
	public void allocationFound(Optional<LiquidAllocation> optionalLiquidAllocation) {
		if (optionalLiquidAllocation.isEmpty()) {
			completionType = FAILURE;
		} else {
			LiquidAllocation liquidAllocation = optionalLiquidAllocation.get();
			parent.setLiquidAllocation(liquidAllocation);
			parent.setTargetLocation(toVector(liquidAllocation.getTargetZoneTile().getTargetTile()));
			completionType = CompletionType.SUCCESS;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
