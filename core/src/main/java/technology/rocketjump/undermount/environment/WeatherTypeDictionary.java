package technology.rocketjump.undermount.environment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.environment.model.WeatherType;
import technology.rocketjump.undermount.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;
import technology.rocketjump.undermount.rendering.utils.HexColors;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;

@Singleton
public class WeatherTypeDictionary {

	private final Map<String, WeatherType> byName = new HashMap<>();
	private final ParticleEffectTypeDictionary particleEffectTypeDictionary;
	private final SoundAssetDictionary soundAssetDictionary;

	@Inject
	public WeatherTypeDictionary(ParticleEffectTypeDictionary particleEffectTypeDictionary, SoundAssetDictionary soundAssetDictionary) throws IOException {
		this.particleEffectTypeDictionary = particleEffectTypeDictionary;
		this.soundAssetDictionary = soundAssetDictionary;
		ObjectMapper objectMapper = new ObjectMapper();
		FileHandle weatherTypesFile = Gdx.files.internal("assets/definitions/weatherTypes.json");
		List<WeatherType> weatherTypes = objectMapper.readValue(weatherTypesFile.readString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, WeatherType.class));

		for (WeatherType weatherType : weatherTypes) {
			initalise(weatherType);
			byName.put(weatherType.getName(), weatherType);
		}

	}

	public WeatherType getByName(String weatherTypeName) {
		return byName.get(weatherTypeName);
	}

	private void initalise(WeatherType weatherType) {
		if (weatherType.getMaxSunlight() != null) {
			weatherType.setMaxSunlightColor(HexColors.get(weatherType.getMaxSunlight()));
		}

		if (weatherType.getParticleEffectTypeName() != null) {
			ParticleEffectType particleEffectType = particleEffectTypeDictionary.getByName(weatherType.getParticleEffectTypeName());
			if (particleEffectType == null) {
				Logger.error("Can not find particle effect type " + weatherType.getParticleEffectTypeName() + " for weather type " + weatherType.getName());
			} else {
				weatherType.setParticleEffectType(particleEffectType);
			}
		}

		if (weatherType.getDayAmbienceSoundAssetName() != null) {
			weatherType.setDayAmbienceSoundAsset(soundAssetDictionary.getByName(weatherType.getDayAmbienceSoundAssetName()));
			if (weatherType.getDayAmbienceSoundAsset() == null) {
				Logger.error("Can not find sound asset with name "+weatherType.getDayAmbienceSoundAssetName()+" for weather " + weatherType.getName());
			}
		}

		if (weatherType.getNightAmbienceSoundAssetName() != null) {
			weatherType.setNightAmbienceSoundAsset(soundAssetDictionary.getByName(weatherType.getNightAmbienceSoundAssetName()));
			if (weatherType.getNightAmbienceSoundAsset() == null) {
				Logger.error("Can not find sound asset with name "+weatherType.getNightAmbienceSoundAssetName()+" for weather " + weatherType.getName());
			}
		}
	}

	public Collection<WeatherType> getAll() {
		return byName.values();
	}
}
