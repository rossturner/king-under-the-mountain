package technology.rocketjump.undermount.audio;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.*;

@Singleton
public class AudioMessageHandler implements Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final SoundEffectManager soundEffectManager;
	private final AmbientSoundManager ambientSoundManager;
	private final SoundAssetDictionary soundAssetDictionary;

	@Inject
	public AudioMessageHandler(MessageDispatcher messageDispatcher, SoundEffectManager soundEffectManager,
							   AmbientSoundManager ambientSoundManager, SoundAssetDictionary soundAssetDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.soundEffectManager = soundEffectManager;
		this.ambientSoundManager = ambientSoundManager;
		this.soundAssetDictionary = soundAssetDictionary;

		messageDispatcher.addListener(this, MessageType.CAMERA_MOVED);
		messageDispatcher.addListener(this, MessageType.GAME_PAUSED);
		messageDispatcher.addListener(this, MessageType.REQUEST_SOUND);
		messageDispatcher.addListener(this, MessageType.GUI_CHANGE_SOUND_EFFECT_VOLUME);
		messageDispatcher.addListener(this, MessageType.GUI_CHANGE_AMBIENT_EFFECT_VOLUME);
		messageDispatcher.addListener(this, MessageType.AMBIENCE_UPDATE);
		messageDispatcher.addListener(this, MessageType.AMBIENCE_PAUSE);
		messageDispatcher.addListener(this, MessageType.GAME_PAUSED);
		messageDispatcher.addListener(this, MessageType.REQUEST_SOUND_ASSET);
		messageDispatcher.addListener(this, MessageType.REQUEST_STOP_SOUND_LOOP);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.CAMERA_MOVED: {
				return handle((CameraMovedMessage) msg.extraInfo);
			}
			case MessageType.GAME_PAUSED: {
				Boolean paused = (Boolean) msg.extraInfo;
				soundEffectManager.setPaused(paused);
				ambientSoundManager.setPaused(paused);
				return true;
			}
			case MessageType.REQUEST_SOUND: {
				return handle ((RequestSoundMessage)msg.extraInfo);
			}
			case MessageType.REQUEST_STOP_SOUND_LOOP : {
				return handle ((RequestSoundStopMessage)msg.extraInfo);
			}
			case MessageType.GUI_CHANGE_SOUND_EFFECT_VOLUME: {
				Float newVolume = (Float)msg.extraInfo;
				soundEffectManager.setVolume(newVolume);
				return true;
			}
			case MessageType.GUI_CHANGE_AMBIENT_EFFECT_VOLUME: {
				Float newVolume = (Float)msg.extraInfo;
				ambientSoundManager.setGlobalVolumeModifier(newVolume);
				return true;
			}
			case MessageType.AMBIENCE_UPDATE: {
				return handle((AmbienceMessage)msg.extraInfo);
			}
			case MessageType.AMBIENCE_PAUSE: {
				Boolean paused = (Boolean) msg.extraInfo;
				ambientSoundManager.setPaused(paused);
				return true;
			}
			case MessageType.REQUEST_SOUND_ASSET: {
				return handle((RequestSoundAssetMessage)msg.extraInfo);
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private boolean handle(AmbienceMessage ambienceMessage) {
		ambientSoundManager.updateViewport(ambienceMessage.outdoorTiles, ambienceMessage.riverTiles, ambienceMessage.totalTiles);
		return true;
	}

	private boolean handle(RequestSoundMessage requestSoundMessage) {
		if (requestSoundMessage.fixedPosition != null) {
			soundEffectManager.requestSound(requestSoundMessage.soundAsset, requestSoundMessage.requesterId, requestSoundMessage.fixedPosition, requestSoundMessage.callback);
		} else {
			soundEffectManager.requestSound(requestSoundMessage.soundAsset, null, null, requestSoundMessage.callback);
		}
		return true;
	}

	private boolean handle(RequestSoundStopMessage soundStopMessage) {
		soundEffectManager.stopSound(soundStopMessage.soundAsset, soundStopMessage.requesterId);
		return true;
	}

	private boolean handle(CameraMovedMessage cameraMovedMessage) {
		soundEffectManager.setViewportParams(cameraMovedMessage.viewportTileWidth, cameraMovedMessage.viewportTileHeight,
				cameraMovedMessage.cameraPosition, cameraMovedMessage.minTilesForZoom, cameraMovedMessage.maxTilesForZoom);
		return true;
	}

	private boolean handle(RequestSoundAssetMessage requestSoundAssetMessage) {
		SoundAsset asset = soundAssetDictionary.getByName(requestSoundAssetMessage.assetName);
		requestSoundAssetMessage.callback.assetFound(asset);
		return true;
	}
}
