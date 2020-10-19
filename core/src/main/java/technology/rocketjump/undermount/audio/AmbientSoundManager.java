package technology.rocketjump.undermount.audio;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.AssetDisposable;
import technology.rocketjump.undermount.audio.model.ActiveSoundEffect;
import technology.rocketjump.undermount.audio.model.GdxAudioException;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.environment.model.Season;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.rendering.ScreenWriter;

import static technology.rocketjump.undermount.audio.SoundEffectManager.GLOBAL_VOLUME_MULTIPLIER;

@Singleton
public class AmbientSoundManager implements Updatable, AssetDisposable {

	private boolean initialised;
	private final SoundAsset daytimeAmbience;
	private final SoundAsset nighttimeAmbience;
	private final SoundAsset riverAmbience;
	private ActiveSoundEffect daytimeActive;
	private ActiveSoundEffect nighttimeActive;
	private ActiveSoundEffect riverActive;
	private float outdoorRatio;
	private float riverRatio;
	private GameContext gameContext;

	public static final String DEFAULT_AMBIENT_AUDIO_VOLUME_AS_STRING = "0.5";
	private float globalAmbientVolumeModifier;

	@Inject
	public AmbientSoundManager(SoundAssetDictionary soundAssetDictionary, ScreenWriter screenWriter, UserPreferences userPreferences) {
		this.daytimeAmbience = soundAssetDictionary.getByName("Daytime");
		this.nighttimeAmbience = soundAssetDictionary.getByName("Nighttime");
		this.riverAmbience = soundAssetDictionary.getByName("River");

		try {
			daytimeActive = new ActiveSoundEffect(daytimeAmbience, 0L, null);
			daytimeActive.loop(0f);
			nighttimeActive = new ActiveSoundEffect(nighttimeAmbience, 0L, null);
			nighttimeActive.loop(0f);
			riverActive = new ActiveSoundEffect(riverAmbience, 0L, null);
			riverActive.loop(0f);
			initialised = true;
		} catch (GdxAudioException e) {
			Logger.error("Gdx.audio is null, did audio initialisation fail?");
			initialised = false;
		}

		String volumeString = userPreferences.getPreference(UserPreferences.PreferenceKey.AMBIENT_EFFECT_VOLUME, DEFAULT_AMBIENT_AUDIO_VOLUME_AS_STRING);
		this.globalAmbientVolumeModifier = GLOBAL_VOLUME_MULTIPLIER * Float.valueOf(volumeString);
	}

	public void updateViewport(int outdoorTiles, int riverTiles, int totalTiles) {
		if (totalTiles == 0) {
			outdoorRatio = 0;
			riverRatio = 0;
		} else {
			outdoorRatio = (float) outdoorTiles / (float) totalTiles;
			riverRatio = (float) riverTiles / (float) totalTiles;
		}
	}

	@Override
	public void update(float multipliedDeltaTime) {
		if (!initialised) {
			return;
		}
		// MODDING expose this
		// outdoor audio between 60% and 100% of ratio
		float desiredOutdoorAmbienceVolume = Math.max((outdoorRatio - 0.75f), 0f) * (1f / (1f - 0.75f));
		float desiredRiverAmbienceVolume = Math.min(riverRatio, 0.15f) * (1f / 0.15f);

		desiredOutdoorAmbienceVolume *= globalAmbientVolumeModifier;
		desiredRiverAmbienceVolume *= globalAmbientVolumeModifier;

		if (daytimeHours()) {
			decreaseVolume(nighttimeActive);

			if (daytimeActive.getVolume() > desiredOutdoorAmbienceVolume) {
				decreaseVolume(daytimeActive);
			} else if (daytimeActive.getVolume() < desiredOutdoorAmbienceVolume) {
				increaseVolume(daytimeActive);
			}

		} else if (nightTimeHours()) {
			decreaseVolume(daytimeActive);

			if (nighttimeActive.getVolume() > desiredOutdoorAmbienceVolume) {
				decreaseVolume(nighttimeActive);
			} else if (nighttimeActive.getVolume() < desiredOutdoorAmbienceVolume) {
				increaseVolume(nighttimeActive);
			}

		} else {
			decreaseVolume(daytimeActive);
			decreaseVolume(nighttimeActive);
		}

		if (riverActive.getVolume() > desiredRiverAmbienceVolume) {
			decreaseVolume(riverActive);
		} else if (riverActive.getVolume() < desiredRiverAmbienceVolume) {
			increaseVolume(riverActive);
		}

	}

	public void setPaused(Boolean pause) {
		if (!initialised) {
			return;
		}
		if (pause) {
			daytimeActive.pause();
			nighttimeActive.pause();
			riverActive.pause();
		} else {
			daytimeActive.resume();
			nighttimeActive.resume();
			riverActive.resume();
		}
	}

	public void setGlobalVolumeModifier(Float newVolume) {
		this.globalAmbientVolumeModifier = newVolume * GLOBAL_VOLUME_MULTIPLIER;
	}

	@Override
	public void dispose() {
		daytimeActive.stop();
		daytimeActive.dispose();
		nighttimeActive.stop();
		nighttimeActive.dispose();
		riverActive.stop();
		riverActive.dispose();
	}

	@Override
	public boolean runWhilePaused() {
		return true;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		outdoorRatio = 0;
		riverRatio = 0;
	}

	private void increaseVolume(ActiveSoundEffect soundEffect) {
		if (soundEffect.getVolume() < 1f) {
			soundEffect.setVolume(Math.min(soundEffect.getVolume() + 0.01f, 1f));
		}
	}

	private void decreaseVolume(ActiveSoundEffect soundEffect) {
		if (soundEffect.getVolume() > 0f) {
			soundEffect.setVolume(Math.max(soundEffect.getVolume() - 0.01f, 0f));
		}
	}

	private boolean nightTimeHours() {
		if (gameContext.getGameClock().getCurrentSeason().equals(Season.WINTER)) {
			return false;
		}
		int hourOfDay = gameContext.getGameClock().getHourOfDay();
		return hourOfDay < 4 || hourOfDay > 21;
	}

	private boolean daytimeHours() {
		if (gameContext.getGameClock().getCurrentSeason().equals(Season.WINTER)) {
			return false;
		}
		int hourOfDay = gameContext.getGameClock().getHourOfDay();
		return 5 <= hourOfDay && hourOfDay <= 19;
	}
}
