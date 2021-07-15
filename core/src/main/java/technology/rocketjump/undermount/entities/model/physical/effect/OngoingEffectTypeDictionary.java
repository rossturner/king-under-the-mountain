package technology.rocketjump.undermount.entities.model.physical.effect;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.undermount.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class OngoingEffectTypeDictionary {

	private final Map<String, OngoingEffectType> byName = new HashMap<>();
	private final ParticleEffectTypeDictionary particleEffectTypeDictionary;

	@Inject
	public OngoingEffectTypeDictionary(ParticleEffectTypeDictionary particleEffectTypeDictionary) throws IOException {
		this.particleEffectTypeDictionary = particleEffectTypeDictionary;

		ObjectMapper objectMapper = new ObjectMapper();
		File typesJsonFile = new File("assets/definitions/types/ongoingEffectTypes.json");
		List<OngoingEffectType> typeList = objectMapper.readValue(FileUtils.readFileToString(typesJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, OngoingEffectType.class));

		for (OngoingEffectType ongoingEffectType : typeList) {
			initialiseTransientFields(ongoingEffectType);
			byName.put(ongoingEffectType.getName(), ongoingEffectType);
		}
	}

	private void initialiseTransientFields(OngoingEffectType ongoingEffectType) {
		ParticleEffectType particleEffectType = particleEffectTypeDictionary.getByName(ongoingEffectType.getParticleEffectTypeName());
		if (particleEffectType == null) {
			throw new RuntimeException("Can not find " + ongoingEffectType.getParticleEffectTypeName() + " particle effect for ongoing effect " + ongoingEffectType.getName());
		}
		ongoingEffectType.setParticleEffectType(particleEffectType);
	}

	public OngoingEffectType getByName(String effectTypeName) {
		return byName.get(effectTypeName);
	}

	public Collection<OngoingEffectType> getAll() {
		return byName.values();
	}
}
