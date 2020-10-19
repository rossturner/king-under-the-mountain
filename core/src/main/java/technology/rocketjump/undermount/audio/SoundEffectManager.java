package technology.rocketjump.undermount.audio;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.undermount.assets.AssetDisposable;
import technology.rocketjump.undermount.audio.model.ActiveSoundEffect;
import technology.rocketjump.undermount.audio.model.GdxAudioException;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.persistence.UserPreferences;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.SOUND_EFFECT_VOLUME;

@Singleton
public class SoundEffectManager implements AssetDisposable {

	public static final String DEFAULT_SOUND_VOLUME_AS_STRING = "0.6";
	public static final float GLOBAL_VOLUME_MULTIPLIER = 0.5f;
	private final float VIEWPORT_OVERFLOW_DISTANCE = 2f;
	private final int MAX_PARALLEL_SOUNDS = 5;

	private final Map<Long, ActiveSoundEffect> activeSoundsByAssetId = new HashMap<>();
	private final float minTilesForZoom;
	private final UserPreferences userPreferences;

	private float viewportWidth; // Volume / distance to sounds based on viewportTileWidth
	private float viewportHeight;
	private Vector3 cameraPosition;
	private boolean gamePaused;
	private float baseVolumeLevel;
	private float viewportZoom;

	@Inject
	public SoundEffectManager(UserPreferences userPreferences) throws IOException {
		this.userPreferences = userPreferences;

		JSONObject uiSettings = JSON.parseObject(FileUtils.readFileToString(new File("assets/ui/uiSettings.json")));
		minTilesForZoom = uiSettings.getFloatValue("minTilesZoom");

		this.baseVolumeLevel = GLOBAL_VOLUME_MULTIPLIER * Float.valueOf(userPreferences.getPreference(SOUND_EFFECT_VOLUME, DEFAULT_SOUND_VOLUME_AS_STRING));
	}

	public void setViewportParams(float viewportWidth, float viewportHeight, Vector3 cameraPosition, float minTilesForZoom, float maxTilesForZoom) {
		this.viewportWidth = viewportWidth;
		this.viewportHeight = viewportHeight;
		this.cameraPosition = cameraPosition;

		this.viewportZoom = 1 - ((viewportWidth - minTilesForZoom) / (maxTilesForZoom - minTilesForZoom));
	}

	@Override
	public void dispose() {
		for (ActiveSoundEffect activeSoundEffect : activeSoundsByAssetId.values()) {
			activeSoundEffect.stop();
			activeSoundEffect.dispose();
		}
		activeSoundsByAssetId.clear();
	}

	public void setPaused(boolean gamePaused) {
		this.gamePaused = gamePaused;
		for (ActiveSoundEffect activeSoundEffect : activeSoundsByAssetId.values()) {
			// Only pause sounds that are in the game world
			if (activeSoundEffect.getWorldPosition() != null) {
				if (gamePaused) {
					activeSoundEffect.pause();
				} else {
					activeSoundEffect.resume();
				}
			}
		}
	}

	public void requestSound(SoundAsset asset, Long entityId, Vector2 worldPosition) {
		if (asset == null) {
			return;
		}
		if (baseVolumeLevel <= 0) {
			return;
		}
		// Cancel any sounds from the same entity
		if (entityId != null) {
			Iterator<Map.Entry<Long, ActiveSoundEffect>> iterator = activeSoundsByAssetId.entrySet().iterator();
			while (iterator.hasNext()) {
				if (iterator.next().getValue().getParentEntityId() == entityId) {
					iterator.remove();
					break;
				}
			}

			// Only disallow multiple sounds when they come from entities
			if (activeSoundsByAssetId.size() >= MAX_PARALLEL_SOUNDS || activeSoundsByAssetId.containsKey(asset.getSoundAssetId())) {
				return;
			}
		}


		try {
			// Check if inside viewport
			ActiveSoundEffect activeSoundEffect = null;
			if (worldPosition != null) {
				long unboxedId = -1L;
				if (entityId != null) {
					unboxedId = entityId;
				}
				if (Math.abs(cameraPosition.x - worldPosition.x) < (viewportWidth / 2) + VIEWPORT_OVERFLOW_DISTANCE &&
						Math.abs(cameraPosition.y - worldPosition.y) < (viewportHeight / 2) + VIEWPORT_OVERFLOW_DISTANCE) {
					activeSoundEffect = new ActiveSoundEffect(asset, unboxedId, worldPosition);
				}
			} else {
				activeSoundEffect = new ActiveSoundEffect(asset, 0L, null);
			}
			if (activeSoundEffect != null) {
				activeSoundsByAssetId.put(asset.getSoundAssetId(), activeSoundEffect);
				activeSoundEffect.play();
				attentuate(activeSoundEffect);

				if (activeSoundEffect.getWorldPosition() != null && gamePaused) {
					activeSoundEffect.pause();
				}
			}
		} catch (GdxAudioException e) {
			// Gdx.audio is not set so just don't play sound
		}
	}

	public void stopSound(SoundAsset soundAsset, long requesterId) {
		ActiveSoundEffect activeSoundEffect = activeSoundsByAssetId.get(soundAsset.getSoundAssetId());
		if (activeSoundEffect != null && activeSoundEffect.getParentEntityId() == requesterId) {
			activeSoundEffect.stop();
		}
	}


	public void update() {
		float elapsed = Gdx.graphics.getDeltaTime();

		Iterator<Map.Entry<Long, ActiveSoundEffect>> activeSoundIterator = activeSoundsByAssetId.entrySet().iterator();
		while (activeSoundIterator.hasNext()) {
			ActiveSoundEffect activeSoundEffect = activeSoundIterator.next().getValue();
			activeSoundEffect.update(elapsed);
			if (activeSoundEffect.completed()) {
				activeSoundEffect.dispose();
				activeSoundIterator.remove();
			} else {
				attentuate(activeSoundEffect);
			}
		}
	}

	private void attentuate(ActiveSoundEffect activeSoundEffect) {
		if (activeSoundEffect.getWorldPosition() == null) {
			activeSoundEffect.setVolume(baseVolumeLevel);
		} else {

			// Curently just based on how zoomed in we are
			float volume = 0.2f + (viewportZoom * 0.7f);
			activeSoundEffect.setVolume(volume);


//			Vector2 cameraToSoundEffect = activeSoundEffect.getWorldPosition().cpy().sub(cameraPosition.x, cameraPosition.y);
//			float soundRadius = Math.min(viewportWidth / 2, viewportHeight / 2) - 3f;
//			float distanceToSound = cameraToSoundEffect.len();
//			float volumeMultiplier;
//			if (distanceToSound < soundRadius) {
//				volumeMultiplier = 1f;
//			} else {
//				volumeMultiplier = (soundRadius - (distanceToSound - soundRadius)) / soundRadius;
//				volumeMultiplier = Math.max(volumeMultiplier, 0);
//			}
//
//			// Also attenuate based on camera zoom
//			// Fall off when zoom is showing more than double minimum tiles
//			if (viewportWidth > minTilesForZoom * 2) {
//				float zoomMultiplier = ((3f * minTilesForZoom) - (viewportWidth - (minTilesForZoom * 2f))) / (3f * minTilesForZoom);
//				zoomMultiplier = Math.max(zoomMultiplier, 0);
//				volumeMultiplier *= zoomMultiplier;
//			}
//			activeSoundEffect.setVolume(baseVolumeLevel * volumeMultiplier);
		}
	}

	public void setVolume(Float newVolume) {
		baseVolumeLevel = newVolume * GLOBAL_VOLUME_MULTIPLIER;
		userPreferences.setPreference(SOUND_EFFECT_VOLUME, String.valueOf(newVolume));
	}
}
