package technology.rocketjump.undermount.materials;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;

@Singleton
public class GameMaterialDictionary {

	private Map<Long, GameMaterial> idMap = new ConcurrentHashMap<>();
	private Map<Long, GameMaterial> thirstQuenchingMaterials = new ConcurrentHashMap<>();
	private Map<Long, GameMaterial> dynamicMaterialsById = new ConcurrentHashMap<>();
	private Map<String, GameMaterial> nameMap = new ConcurrentHashMap<>();
	private Map<GameMaterialType, List<GameMaterial>> typeMap = new ConcurrentHashMap<>();

	@Inject
	public GameMaterialDictionary() throws IOException {
		this(new File("assets/definitions/materials.json"));
	}

	public GameMaterialDictionary(File definitionsJsonFile) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		List<GameMaterial> gameMaterials = objectMapper.readValue(FileUtils.readFileToString(definitionsJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, GameMaterial.class));

		for (GameMaterialType gameMaterialType : GameMaterialType.values()) {
			typeMap.put(gameMaterialType, new LinkedList<>());
		}

		for (GameMaterial gameMaterial : gameMaterials) {
			add(gameMaterial);
		}
	}

	public void add(GameMaterial gameMaterial) {
		idMap.put(gameMaterial.getMaterialId(), gameMaterial);
		nameMap.put(gameMaterial.getMaterialName(), gameMaterial);
		typeMap.get(gameMaterial.getMaterialType()).add(gameMaterial);
		if (gameMaterial.isDynamicallyCreated()) {
			dynamicMaterialsById.put(gameMaterial.getMaterialId(), gameMaterial);
		}
		if (gameMaterial.isQuenchesThirst()) {
			thirstQuenchingMaterials.put(gameMaterial.getMaterialId(), gameMaterial);
		}
	}

	public GameMaterial getByName(String materialName) {
		GameMaterial material = nameMap.get(materialName);
		if (material == null) {
			if (!NULL_MATERIAL.getMaterialName().equals(materialName)) {
				Logger.error("Could not find material with name " + materialName);
			}
			material = NULL_MATERIAL;
		}
		return material;
	}

	public boolean contains(GameMaterial gameMaterial) {
		return nameMap.containsKey(gameMaterial.getMaterialName());
	}

	public GameMaterial getById(long materialId) {
		GameMaterial material = idMap.get(materialId);
		if (material == null) {
			Logger.error("Could not find material by ID " + materialId);
			material = NULL_MATERIAL;
		}
		return material;
	}

	public Iterable<GameMaterial> getAll() {
		return idMap.values();
	}

	public List<GameMaterial> getByType(GameMaterialType type) {
		return typeMap.get(type);
	}

	public Collection<GameMaterial> getThirstQuenchingMaterials() {
		return thirstQuenchingMaterials.values();
	}

	public void clearDynamicMaterials() {
		for (GameMaterial dynamicMaterial : new LinkedList<>(dynamicMaterialsById.values())) {
			idMap.remove(dynamicMaterial.getMaterialId());
			thirstQuenchingMaterials.remove(dynamicMaterial.getMaterialId());
			dynamicMaterialsById.remove(dynamicMaterial.getMaterialId());
			nameMap.remove(dynamicMaterial.getMaterialName());
			typeMap.get(dynamicMaterial.getMaterialType()).remove(dynamicMaterial);
		}
	}
}