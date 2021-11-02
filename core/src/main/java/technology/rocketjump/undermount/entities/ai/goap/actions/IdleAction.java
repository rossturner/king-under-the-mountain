package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.ai.goap.actions.location.GoToLocationAction;
import technology.rocketjump.undermount.entities.ai.goap.actions.location.GoToRandomLocationAction;
import technology.rocketjump.undermount.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.undermount.entities.behaviour.creature.CreatureGroup;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.undermount.misc.VectorUtils.toVector;

public class IdleAction extends Action {

	private static final double MAX_HOURS_TO_IDLE = 0.2;
	public static final float MAX_SEPARATION_FROM_CREATURE_GROUP = 7f;

	public IdleAction(AssignedGoal parent) {
		super(parent);
	}

	private boolean initialised;
	private double elapsedTime;
	private double maxTime;

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (!initialised) {
			GoToLocationAction goToLocationAction = null;
			FaceTowardsLocationAction faceTowardsLocationAction = null;

			Vector2 target = parent.parentEntity.getLocationComponent().getWorldPosition();
			MapTile currentTile = gameContext.getAreaMap().getTile(target);
			if (currentTile == null || !currentTile.isNavigable() || gameContext.getRandom().nextBoolean() || parent.parentEntity.isOnFire()) {
				// 50/50 go to a new location
				target = pickRandomLocation(gameContext, parent.parentEntity);
				if (target != null) {
					goToLocationAction = new GoToRandomLocationAction(parent);
				} else {
					target = parent.parentEntity.getLocationComponent().getWorldPosition();
				}
			}
			if (gameContext.getRandom().nextBoolean()) {
				// 50/50 face a new direction
				CompassDirection randomDirection = CompassDirection.values()[gameContext.getRandom().nextInt(CompassDirection.values().length)];
				Vector2 facingTarget = target.cpy().add(randomDirection.toVector());
				faceTowardsLocationAction = new FaceTowardsLocationAction(parent); // push to head of queue
				parent.setTargetLocation(facingTarget);
			}

			if (faceTowardsLocationAction != null) {
				parent.actionQueue.push(faceTowardsLocationAction);
			}
			if (goToLocationAction != null) {
				parent.actionQueue.push(goToLocationAction);
			}

			maxTime = gameContext.getGameClock().gameHoursToRealTimeSeconds(gameContext.getRandom().nextFloat() * MAX_HOURS_TO_IDLE);

			initialised = true;
		}

		elapsedTime += deltaTime;
		if (parent.parentEntity.isOnFire()) {
			maxTime = 0;
		}

		if (elapsedTime > maxTime) {
			completionType = SUCCESS;
		}
	}


	public static Vector2 pickRandomLocation(GameContext gameContext, Entity entity) {
		Vector2 targetLocation = null;
		int attempts = 0;
		while (targetLocation == null && attempts < 24) {
			Vector2 centralPosition = entity.getLocationComponent().getWorldPosition();
			if (entity.getBehaviourComponent() instanceof CreatureBehaviour) {
				CreatureGroup creatureGroup = ((CreatureBehaviour) entity.getBehaviourComponent()).getCreatureGroup();
				if (creatureGroup != null) {
					centralPosition = toVector(creatureGroup.getHomeLocation());
				}
			}

			MapTile randomCell = gameContext.getAreaMap().getTile(
					(int)Math.floor(centralPosition.x) + (gameContext.getRandom().nextInt(13) - 6),
					(int)Math.floor(centralPosition.y) + (gameContext.getRandom().nextInt(13) - 6));
			if (randomCell != null && randomCell.isNavigable()) {
				targetLocation = randomCell.getWorldPositionOfCenter();
			}
			if (targetLocation != null && entity.getBehaviourComponent() instanceof CreatureBehaviour) {
				CreatureGroup creatureGroup = ((CreatureBehaviour) entity.getBehaviourComponent()).getCreatureGroup();
				if (creatureGroup != null && targetLocation.dst(toVector(creatureGroup.getHomeLocation())) > MAX_SEPARATION_FROM_CREATURE_GROUP) {
					targetLocation = null;
				}
			}
			attempts++;
		}
		return targetLocation; // Null if can't find anywhere to go to
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (initialised) {
			asJson.put("initialised", true);
		}
		if (elapsedTime > 0) {
			asJson.put("elapsedTime", elapsedTime);
		}
		if (maxTime > 0) {
			asJson.put("maxTime", maxTime);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.initialised = asJson.getBooleanValue("initialised");
		this.elapsedTime = asJson.getDoubleValue("elapsedTime");
		this.maxTime = asJson.getDoubleValue("maxTime");
	}
}
