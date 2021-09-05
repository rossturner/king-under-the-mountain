package technology.rocketjump.undermount.entities.model.physical;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.entities.components.AttachedLightSourceComponent;
import technology.rocketjump.undermount.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.EntityPositionChangedMessage;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;

import static technology.rocketjump.undermount.entities.model.EntityType.ONGOING_EFFECT;

public class LocationComponent implements ParentDependentEntityComponent {

	private Vector2 worldPosition; // can be null to denote off-map or being carried
	private Vector2 facing = new Vector2();
	private EntityAssetOrientation orientation; // To only be updated by facing
	private float radius = 0.3f; // Rough size of entity around worldPosition point, maybe change to width and height vector

	private Vector2 linearVelocity = new Vector2();
	private float maxLinearSpeed = 1.8f;
	private float maxLinearAcceleration = 1.2f;

	private float rotation;

	/*
		true signifies this entity should not be tracked for the purposes of items/resources available
		e.g. it is a decoration-only item entity
	 */
	private boolean untracked = false;

	private MessageDispatcher messageDispatcher;
	private Entity parentEntity;

	private Entity containerEntity;
	private transient Long containerEntityId; // Only used during loading
	private transient boolean initialised; // Only used during loading

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.messageDispatcher = messageDispatcher;
		this.parentEntity = parentEntity;

		if (containerEntityId != null) {
			containerEntity = gameContext.getEntities().get(containerEntityId);
			if (containerEntity == null) {
//				if (GlobalSettings.DEV_MODE) {
//					throw new RuntimeException("Could not find container entity by ID for " + this.getClass().getSimpleName() + " of " + parentEntity.toString());
//				} else {
					Logger.error("Could not find container entity by ID for " + this.getClass().getSimpleName() + " of " + parentEntity.toString());
//				}
			}
		}
		initialised = true;
	}

	@Override
	public LocationComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext  /* always null for this type */) {
		LocationComponent cloned = new LocationComponent();
		if (this.worldPosition != null) {
			cloned.worldPosition = this.worldPosition.cpy();
		}
		cloned.facing = this.facing.cpy();
		cloned.orientation = this.orientation;
		cloned.radius = this.radius;
		cloned.linearVelocity = this.linearVelocity.cpy();
		cloned.maxLinearSpeed = this.maxLinearSpeed;
		cloned.maxLinearAcceleration = this.maxLinearAcceleration;
		cloned.rotation = this.rotation;
		cloned.untracked = this.untracked;
		cloned.containerEntity = this.containerEntity;
		return cloned;
	}

	public Vector2 getWorldPosition() {
		return worldPosition;
	}

	public Vector2 getWorldOrParentPosition() {
		if (containerEntity != null) {
			return containerEntity.getLocationComponent().getWorldOrParentPosition();
		} else {
			return worldPosition;
		}
	}

	public void setWorldPosition(Vector2 newPosition, boolean updateFacing) {
		setWorldPosition(newPosition, updateFacing, true);
	}

	public void setWorldPosition(Vector2 newPosition, boolean updateFacing, boolean updateMapTile) {
		if (isInitialised() && updateMapTile) {
			if (newPosition == null || worldPosition == null || crossedTileBoundary(newPosition)) {
				messageDispatcher.dispatchMessage(MessageType.ENTITY_POSITION_CHANGED,
						new EntityPositionChangedMessage(parentEntity, worldPosition, newPosition));
			}
		}
		if (this.worldPosition != null && updateFacing) {
			Vector2 previousPosition = this.worldPosition.cpy();
			Vector2 difference = newPosition.cpy().sub(previousPosition);
			setFacing(difference);
		}
		this.worldPosition = newPosition;
		if (parentEntity != null) {
			AttachedLightSourceComponent attachedLightSourceComponent = parentEntity.getComponent(AttachedLightSourceComponent.class);
			if (attachedLightSourceComponent != null) {
				if (newPosition == null && !parentEntity.getType().equals(ONGOING_EFFECT)) {
					attachedLightSourceComponent.setEnabled(false);
				} else {
					attachedLightSourceComponent.updatePosition();
				}
			}
		}
	}

	/**
	 * This should only be used after cloning an entity so it is not removed from the pre-cloned location
	 */
	public void clearWorldPosition() {
		this.worldPosition = null;
	}

	private boolean crossedTileBoundary(Vector2 newPosition) {
		return Math.floor(worldPosition.x) != Math.floor(newPosition.x) ||
				Math.floor(worldPosition.y) != Math.floor(newPosition.y);
	}

	public Vector2 getFacing() {
		return facing;
	}

	public void setFacing(Vector2 facing) {
		this.facing = facing.nor();
		setOrientation(EntityAssetOrientation.fromFacing(facing));
	}

	public void setOrientation(EntityAssetOrientation newOrientation) {
		this.orientation = newOrientation;
		if (parentEntity != null) {
			for (AttachedEntity attachedItem : parentEntity.getAttachedEntities()) {
				attachedItem.entity.getLocationComponent().setOrientation(newOrientation);
			}
			AttachedLightSourceComponent attachedLightSourceComponent = parentEntity.getComponent(AttachedLightSourceComponent.class);
			if (attachedLightSourceComponent != null) {
				attachedLightSourceComponent.updatePosition();
			}
		}
	}

	public EntityAssetOrientation getOrientation() {
		return orientation;
	}

	public float getRadius() {
		return radius;
	}

	public void setRadius(float radius) {
		this.radius = radius;
	}

	public Vector2 getLinearVelocity() {
		return linearVelocity;
	}

	public void setLinearVelocity(Vector2 linearVelocity) {
		this.linearVelocity = linearVelocity;
	}

	public float getMaxLinearSpeed() {
		return maxLinearSpeed;
	}

	public void setMaxLinearSpeed(float maxLinearSpeed) {
		this.maxLinearSpeed = maxLinearSpeed;
	}

	public float getRotation() {
		return rotation;
	}

	public void setRotation(float rotation) {
		this.rotation = rotation;
	}

	public Entity getContainerEntity() {
		return containerEntity;
	}

	public void setContainerEntity(Entity containerEntity) {
		this.containerEntity = containerEntity;
	}

	public boolean isUntracked() {
		return untracked;
	}

	public void setUntracked(boolean untracked) {
		this.untracked = untracked;
	}

	public boolean isInitialised() {
		return parentEntity != null && messageDispatcher != null;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (worldPosition != null) {
			asJson.put("worldPosition", JSONUtils.toJSON(worldPosition));
		}
		if (facing.len2() > 0) {
			asJson.put("facing", JSONUtils.toJSON(facing));
		}
		if (!EntityAssetOrientation.DOWN.equals(orientation)) {
			asJson.put("orientation", orientation.name());
		}
		if (radius != 0.3f) {
			asJson.put("radius", radius);
		}
		if (linearVelocity.len2() > 0) {
			asJson.put("linearVelocity", JSONUtils.toJSON(linearVelocity));
		}
		if (maxLinearSpeed != 1.8f) {
			asJson.put("maxLinearSpeed", maxLinearSpeed);
		}
		if (maxLinearAcceleration != 1.2f) {
			asJson.put("maxLinearAcceleration", maxLinearAcceleration);
		}
		if (rotation != 0f) {
			asJson.put("rotation", rotation);
		}
		if (untracked) {
			asJson.put("untracked", true);
		}

		if (containerEntity != null) {
			asJson.put("containerEntity", containerEntity.getId());
		}

		if (GlobalSettings.DEV_MODE && worldPosition == null && containerEntity == null) {
			Logger.error("Saving entity with no position");
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONObject worldPositionJson = asJson.getJSONObject("worldPosition");
		if (worldPositionJson != null) {
			worldPosition = new Vector2(worldPositionJson.getFloatValue("x"), worldPositionJson.getFloatValue("y"));
		}

		JSONObject facingJson = asJson.getJSONObject("facing");
		if (facingJson != null) {
			facing.set(facingJson.getFloatValue("x"), facingJson.getFloatValue("y"));
		}

		this.orientation = EnumParser.getEnumValue(asJson, "orientation", EntityAssetOrientation.class, EntityAssetOrientation.DOWN);

		Float radius = asJson.getFloat("radius");
		if (radius != null) {
			this.radius = radius;
		}

		JSONObject linearVelocityJson = asJson.getJSONObject("linearVelocity");
		this.linearVelocity = new Vector2();
		if (linearVelocityJson != null) {
			this.linearVelocity.set(linearVelocityJson.getFloatValue("x"), linearVelocityJson.getFloatValue("y"));
		}

		Float maxLinearSpeed = asJson.getFloat("maxLinearSpeed");
		if (maxLinearSpeed != null) {
			this.maxLinearSpeed = maxLinearSpeed;
		}

		Float maxLinearAcceleration = asJson.getFloat("maxLinearAcceleration");
		if (maxLinearAcceleration != null) {
			this.maxLinearAcceleration = maxLinearAcceleration;
		}

		Float rotation = asJson.getFloat("rotation");
		if (rotation != null) {
			this.rotation = rotation;
		}

		this.untracked = asJson.getBooleanValue("untracked");

		this.containerEntityId = asJson.getLong("containerEntity");

		if (GlobalSettings.DEV_MODE && worldPosition == null && containerEntityId == null) {
			Logger.error("Loading entity with no position");
		}
	}
}
