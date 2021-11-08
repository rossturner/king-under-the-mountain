package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.undermount.misc.VectorUtils.toVector;

public class FaceTowardsLocationAction extends Action {

	public FaceTowardsLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public boolean isApplicable(GameContext gameContext) {
		return selectTargetLocation() != null;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		Vector2 target = selectTargetLocation();
		Vector2 vectorToTarget = target.sub(parent.parentEntity.getLocationComponent().getWorldPosition());
		parent.parentEntity.getLocationComponent().setFacing(vectorToTarget);
		completionType = SUCCESS;
	}

	private Vector2 selectTargetLocation() {
		if (parent.getAssignedJob() != null) {
			if (parent.getAssignedJob().getType().isAccessedFromAdjacentTile()) {
				return toVector(parent.getAssignedJob().getJobLocation());
			} else if (parent.getAssignedJob().getSecondaryLocation() != null) {
				return toVector(parent.getAssignedJob().getSecondaryLocation());
			} else {
				return toVector(parent.getAssignedJob().getJobLocation());
			}
		} else {
			return parent.getTargetLocation();
		}
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
