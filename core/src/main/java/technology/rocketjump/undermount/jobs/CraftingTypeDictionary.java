package technology.rocketjump.undermount.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class CraftingTypeDictionary {

	private final ParticleEffectTypeDictionary particleEffectTypeDictionary;
	private Map<String, CraftingType> byName = new HashMap<>();
	private Map<GameMaterialType, CraftingType> byFurnitureConstruction = new HashMap<>();

	@Inject
	public CraftingTypeDictionary(ProfessionDictionary professionDictionary, ParticleEffectTypeDictionary particleEffectTypeDictionary) throws IOException {
		this(new File("assets/definitions/crafting/craftingTypes.json"), professionDictionary, particleEffectTypeDictionary);
	}

	public CraftingTypeDictionary(File craftingTypesJsonFile, ProfessionDictionary professionDictionary, ParticleEffectTypeDictionary particleEffectTypeDictionary) throws IOException {
		this.particleEffectTypeDictionary = particleEffectTypeDictionary;
		ObjectMapper objectMapper = new ObjectMapper();
		List<CraftingType> craftingTypes = objectMapper.readValue(FileUtils.readFileToString(craftingTypesJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, CraftingType.class));

		for (CraftingType craftingType : craftingTypes) {
			craftingType.setProfessionRequired(professionDictionary.getByName(craftingType.getProfessionRequiredName()));

			if (craftingType.getParticleEffectNames() != null) {
				for (String particleEffectName : craftingType.getParticleEffectNames()) {
					ParticleEffectType particleEffectType = particleEffectTypeDictionary.getByName(particleEffectName);
					if (particleEffectType == null) {
						Logger.error("Could not find particle effect with name " + particleEffectName + " for crafting type " + craftingType.getName());
					} else {
						craftingType.getParticleEffectTypes().add(particleEffectType);
					}
				}
			}

			byName.put(craftingType.getName(), craftingType);
			if (craftingType.getConstructsFurniture() != null) {
				byFurnitureConstruction.put(craftingType.getConstructsFurniture(), craftingType);
			}
		}

		// Something of a hack to remove seeded mushroom logs
		byFurnitureConstruction.put(GameMaterialType.SEED, byName.get("WOODCUTTING"));
	}

	public CraftingType getByName(String name) {
		return byName.get(name);
	}

	public Collection<CraftingType> getAll() {
		return byName.values();
	}

	public CraftingType getByFurnitureConstruction(GameMaterialType gameMaterialType) {
		return byFurnitureConstruction.get(gameMaterialType);
	}
}
