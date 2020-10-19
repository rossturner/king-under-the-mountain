package technology.rocketjump.undermount.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.undermount.jobs.CraftingTypeDictionary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class WallTypeDictionary {

	private ObjectMapper objectMapper = new ObjectMapper();

	private Map<Long, WallType> wallTypeIdMap = new ConcurrentHashMap<>();
	private Map<String, WallType> nameMap = new ConcurrentHashMap<>();

	@Inject
	public WallTypeDictionary(ItemTypeDictionary itemTypeDictionary, CraftingTypeDictionary craftingTypeDictionary) throws IOException {
		this(Gdx.files.internal("assets/definitions/types/wallTypes.json"), itemTypeDictionary, craftingTypeDictionary);
	}

	public WallTypeDictionary(FileHandle wallDefinitionsJsonFile, ItemTypeDictionary itemTypeDictionary, CraftingTypeDictionary craftingTypeDictionary) throws IOException {
		List<WallType> wallTypes = objectMapper.readValue(wallDefinitionsJsonFile.readString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, WallType.class));

		for (WallType wallType : wallTypes) {
			if (wallTypeIdMap.containsKey(wallType.getWallTypeId())) {
				throw new IOException("Duplicate ID for wall: " + wallType);
			}
			if (wallType.getCraftingTypeName() != null) {
				wallType.setCraftingType(craftingTypeDictionary.getByName(wallType.getCraftingTypeName()));
			}
			if (wallType.getRequirements() != null) {
				for (List<QuantifiedItemType> quantifiedItemTypeList : wallType.getRequirements().values()) {
					for (QuantifiedItemType quantifiedItemType : quantifiedItemTypeList) {
						quantifiedItemType.setItemType(itemTypeDictionary.getByName(quantifiedItemType.getItemTypeName()));
					}
				}
			}
			wallTypeIdMap.put(wallType.getWallTypeId(), wallType);
			nameMap.put(wallType.getWallTypeName(), wallType);
		}

		for (WallType wallType : wallTypes) {
			if (wallType.getOverlayWallTypeName() != null) {
				WallType overlayType = getByWallTypeName(wallType.getOverlayWallTypeName());
				if (overlayType == null) {
					throw new RuntimeException("Could not find wall type overlay by name: " + wallType.getOverlayWallTypeName());
				}
				wallType.setOverlayWallType(overlayType);
			}
		}


	}

	public WallType getByWallTypeId(long id) {
		// FIXME #87 Default placeholder assets - return default when not found by ID
		return wallTypeIdMap.get(id);
	}

	public WallType getByWallTypeName(String name) {
		// FIXME #87 Default placeholder assets - return default when not found by name
		return nameMap.get(name);
	}

	public Iterable<WallType> getAllDefinitions() {
		return wallTypeIdMap.values();
	}
}
