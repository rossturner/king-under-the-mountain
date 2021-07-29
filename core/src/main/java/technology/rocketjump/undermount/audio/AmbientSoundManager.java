package technology.rocketjump.undermount.audio;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.AssetDisposable;
import technology.rocketjump.undermount.audio.model.ActiveSoundEffect;
import technology.rocketjump.undermount.audio.model.GdxAudioException;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.persistence.UserPreferences;

import static technology.rocketjump.undermount.audio.SoundEffectManager.GLOBAL_VOLUME_MULTIPLIER;

@Singleton
public class AmbientSoundManager implements Updatable, AssetDisposable {

	private boolean initialised;
	private SoundAsset riverAmbience;
	private ActiveSoundEffect weatherActiveSound;
	private ActiveSoundEffect riverActiveSound;
	private float outdoorRatio;
	private float riverRatio;
	private GameContext gameContext;

	public static final String DEFAULT_AMBIENT_AUDIO_VOLUME_AS_STRING = "0.5";
	private float globalAmbientVolumeModifier;

	@Inject
	public AmbientSoundManager(SoundAssetDictionary soundAssetDictionary, UserPreferences userPreferences) {
		this.riverAmbience = soundAssetDictionary.getByName("River");

		try {
			riverActiveSound = new ActiveSoundEffect(riverAmbience, 0L, null);
			riverActiveSound.loop(0f);
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

		try {
			// MODDING expose this
			// outdoor audio between 60% and 100% of ratio
			float desiredOutdoorAmbienceVolume = Math.max((outdoorRatio - 0.2f), 0f) * (1f / (1f - 0.75f));
			float desiredRiverAmbienceVolume = Math.min(riverRatio, 0.2f) * (1f / 0.15f);

			desiredOutdoorAmbienceVolume *= globalAmbientVolumeModifier;
			desiredRiverAmbienceVolume *= globalAmbientVolumeModifier;

			SoundAsset weatherAmbienceAsset = getCurrentWeatherAmbienceAsset(daytimeHours());
			if (weatherActiveSound == null && weatherAmbienceAsset != null) {
				weatherActiveSound = new ActiveSoundEffect(weatherAmbienceAsset, 0L, null);
				weatherActiveSound.loop(0f);
			}

			if (weatherActiveSound != null) {
				if (matchesCurrentWeather(weatherAmbienceAsset)) {
					if (weatherActiveSound.getVolume() > desiredOutdoorAmbienceVolume) {
						decreaseVolume(weatherActiveSound);
					} else if (weatherActiveSound.getVolume() < desiredOutdoorAmbienceVolume) {
						increaseVolume(weatherActiveSound);
					}
				} else {
					decreaseVolume(weatherActiveSound);
					if (weatherActiveSound.getVolume() <= 0f) {
						weatherActiveSound.stop();
						weatherActiveSound.dispose();
						weatherActiveSound = null;
					}
				}
			}

			if (riverActiveSound.getVolume() > desiredRiverAmbienceVolume) {
				decreaseVolume(riverActiveSound);
			} else if (riverActiveSound.getVolume() < desiredRiverAmbienceVolume) {
				increaseVolume(riverActiveSound);
			}
		} catch (GdxAudioException e) {
			Logger.error(e.getMessage(), e);
		}

	}

	private boolean matchesCurrentWeather(SoundAsset weatherAmbienceAsset) {
		return weatherActiveSound.getAsset() == weatherAmbienceAsset;
	}

	private SoundAsset getCurrentWeatherAmbienceAsset(boolean isDaytime) {
		if (isDaytime) {
			return gameContext.getMapEnvironment().getCurrentWeather().getDayAmbienceSoundAsset();
		} else {
			return gameContext.getMapEnvironment().getCurrentWeather().getNightAmbienceSoundAsset();
		}
	}

	public void setPaused(Boolean pause) {
		if (!initialised) {
			return;
		}
		if (pause) {
			if (weatherActiveSound != null) {
				weatherActiveSound.pause();
			}
			riverActiveSound.pause();
		} else {
			if (weatherActiveSound != null) {
				weatherActiveSound.resume();
			}
			riverActiveSound.resume();
		}
	}

	public void setGlobalVolumeModifier(Float newVolume) {
		this.globalAmbientVolumeModifier = newVolume * GLOBAL_VOLUME_MULTIPLIER;
	}

	@Override
	public void dispose() {
		if (weatherActiveSound != null) {
			weatherActiveSound.stop();
			weatherActiveSound.dispose();
		}
		riverActiveSound.stop();
		riverActiveSound.dispose();
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

	private boolean daytimeHours() {
		int hourOfDay = gameContext.getGameClock().getHourOfDay();
		return 5 <= hourOfDay && hourOfDay <= 19;
	}
}
