package technology.rocketjump.undermount.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class CraftingTypeDictionary {

	private Map<String, CraftingType> byName = new HashMap<>();
	private Map<GameMaterialType, CraftingType> byFurnitureConstruction = new HashMap<>();

	@Inject
	public CraftingTypeDictionary(ProfessionDictionary professionDictionary) throws IOException {
		this(new File("assets/definitions/crafting/craftingTypes.json"), professionDictionary);
	}

	public CraftingTypeDictionary(File craftingTypesJsonFile, ProfessionDictionary professionDictionary) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		List<CraftingType> craftingTypes = objectMapper.readValue(FileUtils.readFileToString(craftingTypesJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, CraftingType.class));

		for (CraftingType craftingType : craftingTypes) {
			craftingType.setProfessionRequired(professionDictionary.getByName(craftingType.getProfessionRequiredName()));
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
