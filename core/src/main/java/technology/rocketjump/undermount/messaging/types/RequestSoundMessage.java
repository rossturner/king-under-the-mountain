package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.entities.model.Entity;

public class RequestSoundMessage {

	public final SoundAsset soundAsset;
	public final Long requesterId;
	public final Vector2 fixedPosition;

	public RequestSoundMessage(SoundAsset soundAsset, Long requesterId, Vector2 position) {
		this.soundAsset = soundAsset;
		this.requesterId = requesterId;
		this.fixedPosition = position;
	}

	/**
	 * This constructor is to be used for UI sounds and other sounds that should always be played
	 */
	public RequestSoundMessage(SoundAsset soundAsset) {
		this.soundAsset = soundAsset;
		this.requesterId = null;
		this.fixedPosition = null;
	}

	public RequestSoundMessage(SoundAsset soundAsset, Entity entity) {
		this(soundAsset, entity.getId(), entity.getLocationComponent().getWorldOrParentPosition());
	}
}
