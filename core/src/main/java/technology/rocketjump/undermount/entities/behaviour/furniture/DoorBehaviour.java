package technology.rocketjump.undermount.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.assets.entities.furniture.model.DoorState;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.components.humanoid.SteeringComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RequestSoundMessage;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.undermount.assets.entities.furniture.model.DoorState.*;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;

public class DoorBehaviour implements BehaviourComponent {

	public static final float ANIMATION_TIME = 0.4f;

	private MessageDispatcher messageDispatcher;
	private Entity parentEntity;

	private float currentStateElapsedTime = 0;
	private boolean doorOpenRequested;
	private DoorState state = DoorState.CLOSED;
	private SoundAsset openSound;
	private SoundAsset closeSound;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.messageDispatcher = messageDispatcher;
		this.parentEntity = parentEntity;
	}

	@Override
	public DoorBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		DoorBehaviour cloned = new DoorBehaviour();
		cloned.init(parentEntity, messageDispatcher, gameContext);
		cloned.currentStateElapsedTime = this.currentStateElapsedTime;
		cloned.doorOpenRequested = this.doorOpenRequested;
		cloned.state = this.state;
		return cloned;
	}

	public DoorState getState() {
		return state;
	}

	public void setState(DoorState state) {
		this.state = state;
	}

	public void doorOpenRequested() {
		if (state.equals(CLOSED) || state.equals(CLOSING)) {
			doorOpenRequested = true;
		}
	}

	public void setSoundAssets(SoundAsset openSound, SoundAsset closeSound) {
		this.openSound = openSound;
		this.closeSound = closeSound;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		currentStateElapsedTime += deltaTime;

		PhysicalEntityComponent physicalEntityComponent = parentEntity.getPhysicalEntityComponent();
		float currentAnimationProgress = physicalEntityComponent.getAnimationProgress();

		MapTile doorTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
		boolean otherEntityInTile = doorTile.getEntities().size() > 1;

		if (doorOpenRequested) {
			switchState(OPENING);
			doorOpenRequested = false;
		} else {
			switch (state) {
				case CLOSED:
					break;
				case OPENING:
					currentAnimationProgress += (deltaTime / ANIMATION_TIME);
					if (currentAnimationProgress >= 1f) {
						currentAnimationProgress = 1f;
						switchState(OPEN);
					}
					physicalEntityComponent.setAnimationProgress(currentAnimationProgress);
					break;
				case OPEN:
					// Check for entities in doorway, if so, start closing
					if (otherEntityInTile) {
						currentStateElapsedTime = 0f;
					}

					if (currentStateElapsedTime > 1) {
						switchState(CLOSING);
					}
					break;
				case CLOSING:
					currentAnimationProgress -= (deltaTime / ANIMATION_TIME);

					if (otherEntityInTile) {
						switchState(OPENING);
					} else if (currentAnimationProgress <= 0f) {
						currentAnimationProgress = 0f;
						switchState(CLOSED);
					}
					physicalEntityComponent.setAnimationProgress(currentAnimationProgress);
					break;
			}
		}

	}

	private void switchState(DoorState newState) {
		DoorState previousState = this.state;
		if (previousState.equals(newState)) {
			return;
		} else {
			currentStateElapsedTime = 0;
		}
		this.state = newState;

		if (previousState.equals(CLOSED) && newState.equals(OPENING)) {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(openSound, parentEntity.getId(), parentEntity.getLocationComponent().getWorldOrParentPosition()));
		} else if (previousState.equals(OPEN) && newState.equals(CLOSING)) {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(closeSound, parentEntity.getId(), parentEntity.getLocationComponent().getWorldOrParentPosition()));
		}

		if (previousState.equals(CLOSED) || newState.equals(CLOSED)) {
			messageDispatcher.dispatchMessage(MessageType.DOOR_OPENED_OR_CLOSED, toGridPoint(parentEntity.getLocationComponent().getWorldPosition()));
		}
	}


	@Override
	public void infrequentUpdate(GameContext gameContext) {
		// Do nothing, does not update infrequently
	}

	@Override
	public SteeringComponent getSteeringComponent() {
		return null;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return true;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return false;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (currentStateElapsedTime != 0f) {
			asJson.put("elapsedTime", currentStateElapsedTime);
		}
		if (doorOpenRequested) {
			asJson.put("openRequested", true);
		}
		if (!state.equals(CLOSED)) {
			asJson.put("state", state.name());
		}

		if (openSound != null) {
			asJson.put("openSound", openSound.getName());
		}
		if (closeSound != null) {
			asJson.put("closeSound", closeSound.getName());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.currentStateElapsedTime = asJson.getFloatValue("elapsedTime");
		this.doorOpenRequested = asJson.getBooleanValue("openRequested");
		this.state = EnumParser.getEnumValue(asJson, "state", DoorState.class, DoorState.CLOSED);

		String openSoundName = asJson.getString("openSound");
		if (openSoundName != null) {
			this.openSound = relatedStores.soundAssetDictionary.getByName(openSoundName);
			if (this.openSound == null) {
				throw new InvalidSaveException("Could not find sound by name " + openSoundName);
			}
		}
		String closeSoundName = asJson.getString("closeSound");
		if (closeSoundName != null) {
			this.closeSound = relatedStores.soundAssetDictionary.getByName(closeSoundName);
			if (this.closeSound == null) {
				throw new InvalidSaveException("Could not find sound by name " + closeSoundName);
			}
		}
	}
}
