package technology.rocketjump.undermount.entities.components.humanoid;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.furniture.model.DoorState;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.LocationComponent;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.NullMapTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.undermount.entities.model.physical.creature.Consciousness.AWAKE;

public class SteeringComponent implements ChildPersistable {

	private static final float ROTATION_MULTIPLIER = 1.5f; // for quicker turning speed
	private static final float KNOCKBACK_DISTANCE_PER_SECOND = 8f;
	private LocationComponent locationComponent;
	private MessageDispatcher messageDispatcher;
	private Entity parentEntity;
	private TiledMap areaMap;

	private Vector2 destination;
	private Vector2 nextWaypoint;

	private float timeSinceLastPauseCheck = 0;
	private static float TIME_BETWEEN_PAUSE_CHECKS = 1.2f;
	private float pauseTime = 0;
	private static float DEFAULT_PAUSE_TIME = 0.9f;
	private boolean isSlowed;
	private boolean movementImpaired;
	private Vector2 knockback;

	public SteeringComponent() {

	}

	public void init(Entity parentEntity, TiledMap map, LocationComponent locationComponent, MessageDispatcher messageDispatcher) {
		this.locationComponent = locationComponent;
		this.parentEntity = parentEntity;
		this.areaMap = map;
		this.messageDispatcher = messageDispatcher;
		// Somewhat randomise when this first occurs
		this.timeSinceLastPauseCheck = -((float)(parentEntity.getId() % 2400L))/1000f;
	}

	public void destinationReached() {
		this.nextWaypoint = null;
		this.destination = null;
	}

	public void update(float deltaTime) {

		Vector2 steeringOutputForce = new Vector2();
		// Get current position and vector to target destination
		Vector2 currentPosition = locationComponent.getWorldPosition();
		Vector2 currentVelocity = locationComponent.getLinearVelocity();

		if (currentPosition == null) {
			Logger.error("Attempting to update null position in " + this.getClass().getSimpleName());
			return;
		}

		boolean updateFacing = true;

		if (nextWaypoint == null) {
			updateFacing = false;
			if (currentVelocity.len2() > 0.5f) {
				currentVelocity.mulAdd(currentVelocity.cpy().scl(-3f), deltaTime);
			} else {
				currentVelocity.setZero();
			}
		} else {
			MapTile nextTile = areaMap.getTile(nextWaypoint);
			boolean waitingForDoorToOpen = false;
			if (nextTile.hasDoorway()) {
				DoorState doorState = nextTile.getDoorway().getDoorState();
				if (!doorState.equals(DoorState.OPEN)) {
					messageDispatcher.dispatchMessage(MessageType.REQUEST_DOOR_OPEN, nextTile.getDoorway().getDoorEntity());
					waitingForDoorToOpen = true;
				}
			}

			Vector2 nextWaypointRelative = nextWaypoint.cpy().sub(currentPosition);
			if (!waitingForDoorToOpen) {
				if (nextWaypoint == destination) {
					// approach rather than full steam ahead
					currentVelocity.mulAdd(currentVelocity.cpy().scl(-2f), deltaTime);
					steeringOutputForce.add(nextWaypointRelative.nor().scl(2f));
				} else {
					steeringOutputForce.add(nextWaypointRelative.nor().scl(3f));
				}

			}
			rotateFacingAndApplyVelocity(deltaTime, currentVelocity, nextWaypointRelative);


		}

		float maxSpeed = locationComponent.getMaxLinearSpeed();
		isSlowed = false;
		Vector2 entityAvoidanceForce = new Vector2();
		Vector2 wallAvoidanceForce = new Vector2();
		// If we're colliding with another entity, slow down
		MapTile currentTile = areaMap.getTile(currentPosition);
		for (MapTile tileNearPosition : areaMap.getNearestTiles(currentPosition)) {
			// If it's a wall, only repel if we're not in it
			if ((tileNearPosition.hasWall() && !tileNearPosition.equals(currentTile)) || tileNearPosition instanceof NullMapTile) {
				Vector2 wallToEntity = currentPosition.cpy().sub(tileNearPosition.getWorldPositionOfCenter());
				if (wallToEntity.len2() < 0.5f) {
					wallAvoidanceForce.add(wallToEntity.nor());
				}
			}

			for (Entity otherEntity : tileNearPosition.getEntities()) {
				if (otherEntity.getId() != parentEntity.getId() && otherEntity.getType().equals(EntityType.CREATURE)) {
					if (!AWAKE.equals(((CreatureEntityAttributes)otherEntity.getPhysicalEntityComponent().getAttributes()).getConsciousness())) {
						continue;
					}
					Vector2 separation = currentPosition.cpy().sub(otherEntity.getLocationComponent().getWorldPosition());
					float totalRadii = this.locationComponent.getRadius() + otherEntity.getLocationComponent().getRadius();
					float separationDistance = separation.len();
					if (separationDistance < totalRadii) {
						// Overlapping
						isSlowed = true;
					}
					if (separationDistance < totalRadii + locationComponent.getRadius()) {
						entityAvoidanceForce.add(separation.nor());
					}
				}
			}
		}

		if (currentTile != null) {
			if (currentTile.getFloor().isRiverTile() && !currentTile.getFloor().hasBridge()) {
				maxSpeed *= 2;
				steeringOutputForce.add(currentTile.getFloor().getRiverTile().getFlowDirection().cpy().scl(20f));
			}

			maxSpeed *= currentTile.getFloor().getFloorType().getSpeedModifier();

			if (!currentTile.hasWall()) {
				steeringOutputForce.add(wallAvoidanceForce.limit(2f));
			}
		}

		steeringOutputForce.add(entityAvoidanceForce.limit(1f));

		if (isSlowed || movementImpaired) {
			maxSpeed *= 0.5f;
		}

		if (parentEntity.isOnFire()) {
			maxSpeed *= 2f;
		}


		if (pauseTime > 0) {
			pauseTime -= deltaTime;
			maxSpeed *= 0.4f;
			timeSinceLastPauseCheck = 0f;
		} else {
			timeSinceLastPauseCheck += deltaTime;
			if (timeSinceLastPauseCheck > TIME_BETWEEN_PAUSE_CHECKS) {
				timeSinceLastPauseCheck = 0f;
				checkToPauseForOtherEntities();
			}
		}
		Vector2 newVelocity = currentVelocity.cpy().mulAdd(steeringOutputForce, deltaTime).limit(maxSpeed);
		Vector2 newPosition = currentPosition.cpy().mulAdd(newVelocity, deltaTime);

		locationComponent.setLinearVelocity(newVelocity);
		locationComponent.setWorldPosition(newPosition, updateFacing);

		// TODO Adjust position for nudges by other entities

		if (knockback != null) {
			float knockbackDistanceThisFrame = deltaTime * KNOCKBACK_DISTANCE_PER_SECOND;
			if (knockbackDistanceThisFrame > knockback.len()) {
				locationComponent.setWorldPosition(newPosition.cpy().add(knockback), false);
				knockback = null;
			} else {
				Vector2 knockbackThisFrame = knockback.cpy().nor().scl(knockbackDistanceThisFrame);
				locationComponent.setWorldPosition(newPosition.cpy().add(knockbackThisFrame), false);
				knockback.sub(knockbackThisFrame);
			}
		}


		if (currentTile != null && !currentTile.hasWall()) {
			repelFromImpassableCollisions(deltaTime, currentTile);
		}
	}



	private void rotateFacingAndApplyVelocity(float deltaTime, Vector2 currentVelocity, Vector2 target) {
		float angleToWaypoint = target.angle();
		float angleToVelocity = currentVelocity.angle();
		float difference = Math.abs(angleToVelocity - angleToWaypoint);
		// Don't try to apply very small rotations
		if (difference > 3f) {
			boolean positiveRotation = angleToVelocity - angleToWaypoint < 0;
			if (difference > 180f) {
				difference = 360f - difference;
				positiveRotation = !positiveRotation;
			}
			if (difference > 120f) {
				difference = 120f;
//				 Relatively large angle, so let's slow down velocity
//				currentVelocity.mulAdd(currentVelocity.cpy().scl(-3f), deltaTime);
			}

			if (!positiveRotation) {
				difference = -difference;
			}
			currentVelocity.rotate(difference * deltaTime * ROTATION_MULTIPLIER);
		}
	}

	public void setDestination(Vector2 destination) {
		this.destination = destination;
	}

	public void setNextWaypoint(Vector2 nextWaypoint) {
		this.nextWaypoint = nextWaypoint;
	}

	private void repelFromImpassableCollisions(float deltaTime, MapTile currentTile) {
		Vector2 currentPosition = locationComponent.getWorldPosition().cpy();
		Vector2 adjustmentForce = new Vector2();
		for (MapTile tileNearNewPosition : areaMap.getNearestTiles(currentPosition)) {
			if (!tileNearNewPosition.isNavigable() && !tileNearNewPosition.equals(currentTile)) {
				// if overlapping wall
				Vector2 wallToPosition = currentPosition.cpy().sub(tileNearNewPosition.getWorldPositionOfCenter());

				if (Math.abs(wallToPosition.x) < 0.5f + locationComponent.getRadius() &&
						Math.abs(wallToPosition.y) < 0.5f + locationComponent.getRadius()) {
					// We are overlapping the wall
					adjustmentForce.add(wallToPosition.nor());
				}

			}
		}
		// Each force is a 1 tile/second speed, could do with being proportional to nearness of wall
		currentPosition.mulAdd(adjustmentForce, deltaTime);
		locationComponent.setWorldPosition(currentPosition, false);
	}

	/**
	 * This checks to see if other moving entities are in front, and if so and moving in same direction, slow down a bit
	 */
	public void checkToPauseForOtherEntities() {
		if (pauseTime <= 0) {
			boolean forceBreak = false;
			Vector2 currentPosition = locationComponent.getWorldPosition();
			MapTile currentTile = areaMap.getTile(currentPosition);
			for (MapTile tileNearPosition : areaMap.getNearestTiles(currentPosition)) {
				if (pauseTime > 0) {
					break;
				}

				for (Entity otherEntity : tileNearPosition.getEntities()) {
					if (otherEntity.getId() != parentEntity.getId() && otherEntity.getType().equals(EntityType.CREATURE)) {
						if (!AWAKE.equals(((CreatureEntityAttributes)otherEntity.getPhysicalEntityComponent().getAttributes()).getConsciousness())) {
							continue;
						}

						if (otherEntity.getBehaviourComponent().getSteeringComponent().pauseTime > 0) {
							break;
						}

						Vector2 thisToOther = currentPosition.cpy().sub(otherEntity.getLocationComponent().getWorldPosition());
						float totalRadii = this.locationComponent.getRadius() + otherEntity.getLocationComponent().getRadius();
						float separationDistance = thisToOther.len();
						if (separationDistance < totalRadii * 2) {
							// Overlapping

							boolean similarFacing = this.locationComponent.getLinearVelocity().cpy().dot(otherEntity.getLocationComponent().getLinearVelocity()) > 0;
							boolean otherEntityInFront = thisToOther.cpy().dot(otherEntity.getLocationComponent().getLinearVelocity()) < 0;
							if (similarFacing && otherEntityInFront) {
								pauseTime = DEFAULT_PAUSE_TIME;
								break;
							}
						}
					}
				}
			}
		}
	}

	public float getPauseTime() {
		return pauseTime;
	}

	public boolean isSlowed() {
		return isSlowed;
	}

	public void setMovementImpaired(boolean movementImpaired) {
		this.movementImpaired = movementImpaired;
	}

	public boolean getMovementImpaired() {
		return movementImpaired;
	}

	public void setKnockback(Vector2 knockback) {
		this.knockback = knockback;
	}

	public Vector2 getKnockback() {
		return knockback;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (destination != null) {
			asJson.put("destination", JSONUtils.toJSON(destination));
		}
		if (nextWaypoint != null) {
			asJson.put("nextWaypoint", JSONUtils.toJSON(nextWaypoint));
		}
		if (movementImpaired) {
			asJson.put("movementImpaired", true);
		}
		if (knockback != null) {
			asJson.put("knockback", JSONUtils.toJSON(knockback));
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.destination = JSONUtils.vector2(asJson.getJSONObject("destination"));
		this.nextWaypoint = JSONUtils.vector2(asJson.getJSONObject("destination"));
		this.movementImpaired = asJson.getBooleanValue("movementImpaired");
		this.knockback = JSONUtils.vector2(asJson.getJSONObject("knockback"));
	}

}
