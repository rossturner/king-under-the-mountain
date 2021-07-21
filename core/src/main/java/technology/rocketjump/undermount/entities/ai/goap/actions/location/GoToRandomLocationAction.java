package technology.rocketjump.undermount.entities.ai.goap.actions.location;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.entities.ai.goap.actions.IdleAction.pickRandomLocation;

public class GoToRandomLocationAction extends GoToLocationAction {

	private static final float MAX_ELAPSED_TIME = 10f;

	private float elapsedTime;

	public GoToRandomLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (overrideLocation == null) {
			Vector2 target = pickRandomLocation(gameContext, parent.parentEntity);
			if (target != null) {
				overrideLocation = target;
			} else {
				completionType = FAILURE;
				return;
			}
		}

		elapsedTime += deltaTime;
		if (elapsedTime > MAX_ELAPSED_TIME) {
			parent.setInterrupted(true);
		}

		super.update(deltaTime, gameContext);
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		asJson.put("elapsed", elapsedTime);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.elapsedTime = asJson.getFloatValue("elapsed");
	}
}
