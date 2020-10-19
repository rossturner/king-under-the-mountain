package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.components.LiquidAllocation;
import technology.rocketjump.undermount.entities.components.LiquidContainerComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.undermount.entities.components.ItemAllocation.AllocationState.CANCELLED;

public class CancelLiquidAllocationAction extends Action {

	public CancelLiquidAllocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public boolean isInterruptible() {
		return false;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (parent.getLiquidAllocation() != null) {
			completionType = cancelLiquidAllocation(parent.getLiquidAllocation(), gameContext);
			parent.setLiquidAllocation(null);
		} else {
			completionType = FAILURE;
		}
	}

	public static CompletionType cancelLiquidAllocation(LiquidAllocation liquidAllocation, GameContext gameContext) {
		switch (liquidAllocation.getType()) {
			case FROM_RIVER: {
				return SUCCESS;
			}
			case FROM_LIQUID_CONTAINER: {
				Entity targetEntity = gameContext.getEntities().get(liquidAllocation.getTargetContainerId());
				if (targetEntity == null) {
					Logger.warn("Target entity for " + CancelLiquidAllocationAction.class.getSimpleName() + " is null, probably removed furniture");
					return FAILURE;
				}

				LiquidContainerComponent liquidContainerComponent1 = targetEntity.getComponent(LiquidContainerComponent.class);
				if (liquidContainerComponent1 == null) {
					Logger.error("Target entity does not have LiquidContainerComponent");
					return FAILURE;
				}

				liquidContainerComponent1.cancelAllocation(liquidAllocation);
				if (liquidAllocation.getState().equals(CANCELLED)) {
					return SUCCESS;
				} else {
					Logger.error("LiquidAllocation was not cancelled successfully");
					return FAILURE;
				}
			}
			default:
				Logger.error("Not yet implemented cancelLiquidAllocation() for type " + liquidAllocation.getType());
		}
		return FAILURE;
	}


	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {

	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {

	}
}
