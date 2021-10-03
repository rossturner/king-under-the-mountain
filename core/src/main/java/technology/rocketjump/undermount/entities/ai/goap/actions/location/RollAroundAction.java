package technology.rocketjump.undermount.entities.ai.goap.actions.location;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.ai.goap.actions.Action;
import technology.rocketjump.undermount.entities.components.humanoid.StatusComponent;
import technology.rocketjump.undermount.entities.model.physical.LocationComponent;
import technology.rocketjump.undermount.entities.model.physical.creature.status.OnFireStatus;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static technology.rocketjump.undermount.entities.ai.goap.actions.location.GoToLocationAction.WAYPOINT_TOLERANCE;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.misc.VectorUtils.toVector;

public class RollAroundAction extends Action {

	private static final float SECONDS_TO_SPIN_ONE_REVOLUTION = 0.6f;
	private static final float MAX_SECONDS_TO_ROLL_FOR = 4.5f;
	private static final float CHANCE_TO_PUT_OUT_FIRE = 0.1f;

	private GridPoint2 targetTilePosition;
	private float timeElapsed;

	public RollAroundAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (targetTilePosition == null) {
			GridPoint2 parentTileLocation = toGridPoint(parent.parentEntity.getLocationComponent().getWorldOrParentPosition());
			List<CompassDirection> directions = new ArrayList<>(List.of(CompassDirection.values()));
			Collections.shuffle(directions, gameContext.getRandom());
			for (CompassDirection direction : directions) {
				MapTile targetTile = gameContext.getAreaMap().getTile(
						parentTileLocation.x + direction.getXOffset(),
						parentTileLocation.y + direction.getYOffset()
				);
				if (targetTile != null && targetTile.isNavigable()) {
					targetTilePosition = targetTile.getTilePosition();
					break;
				}
			}
			if (targetTilePosition == null) {
				targetTilePosition = parentTileLocation;
			}

			parent.parentEntity.getBehaviourComponent().getSteeringComponent().setNextWaypoint(toVector(targetTilePosition));
			parent.parentEntity.getBehaviourComponent().getSteeringComponent().setDestination(toVector(targetTilePosition));


			if (gameContext.getRandom().nextBoolean()) {
				parent.parentEntity.getLocationComponent().setRotation(80f + (gameContext.getRandom().nextFloat() * 20f));
			} else {
				parent.parentEntity.getLocationComponent().setRotation(260f + (gameContext.getRandom().nextFloat() * 20f));
			}
		}

		timeElapsed += deltaTime;
		float spinAmount = timeElapsed % 1f;


		Vector2 newFacing = new Vector2(0, 1);
		float rotation = spinAmount * 360;
		if (parent.parentEntity.getLocationComponent().getLinearVelocity().y > 0) {
			// This is so the roll happens in a realistic direction
			rotation = 360 - rotation;
		}
		newFacing.rotate(rotation);

		parent.parentEntity.getLocationComponent().setFacing(newFacing);

		if (reachedTarget() || timeElapsed > MAX_SECONDS_TO_ROLL_FOR) {
			parent.parentEntity.getLocationComponent().setRotation(0);
			parent.parentEntity.getBehaviourComponent().getSteeringComponent().destinationReached();

			if (gameContext.getRandom().nextFloat() < CHANCE_TO_PUT_OUT_FIRE) {
				StatusComponent statusComponent = parent.parentEntity.getComponent(StatusComponent.class);
				if (statusComponent != null) {
					statusComponent.remove(OnFireStatus.class);
				}
			}

			completionType = CompletionType.SUCCESS;
		}
	}

	private boolean reachedTarget() {
		LocationComponent locationComponent = parent.parentEntity.getLocationComponent();
		Vector2 targetPosition = toVector(targetTilePosition);
		return (Math.abs(locationComponent.getWorldPosition().x - targetPosition.x) < WAYPOINT_TOLERANCE &&
				Math.abs(locationComponent.getWorldPosition().y - targetPosition.y) < WAYPOINT_TOLERANCE);
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("target", JSONUtils.toJSON(targetTilePosition));
		asJson.put("elapsed", timeElapsed);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.targetTilePosition = JSONUtils.gridPoint2(asJson.getJSONObject("target"));
		this.timeElapsed = asJson.getFloatValue("elapsed");
	}
}
