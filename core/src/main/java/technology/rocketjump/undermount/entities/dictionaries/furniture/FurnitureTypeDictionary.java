package technology.rocketjump.undermount.entities.dictionaries.furniture;

import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemType;

import java.io.IOException;
import java.util.*;

@Singleton
public class FurnitureTypeDictionary {

	private final Map<String, FurnitureType> byName = new HashMap<>();
	private final List<FurnitureType> placeAnywhereFurniture = new LinkedList<>();

	public static FurnitureType NULL_TYPE = new FurnitureType();
	static {
		NULL_TYPE.setName("Null furniture type");
	}


	@Inject
	public FurnitureTypeDictionary(FurnitureCategoryDictionary categoryDictionary, FurnitureLayoutDictionary layoutDictionary,
								   ItemTypeDictionary itemTypeDictionary) throws IOException {
		this(new FileHandle("assets/definitions/types/furnitureTypes.json"), categoryDictionary, layoutDictionary, itemTypeDictionary);
	}

	public FurnitureTypeDictionary(FileHandle jsonFile, FurnitureCategoryDictionary categoryDictionary,
								   FurnitureLayoutDictionary layoutDictionary, ItemTypeDictionary itemTypeDictionary) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		List<FurnitureType> furnitureTypes = objectMapper.readValue(jsonFile.readString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, FurnitureType.class));

		for (FurnitureType furnitureType : furnitureTypes) {
			initialiseFurnitureType(furnitureType, categoryDictionary, layoutDictionary, itemTypeDictionary);

			byName.put(furnitureType.getName(), furnitureType);
			if (furnitureType.isPlaceAnywhere()) {
				placeAnywhereFurniture.add(furnitureType);
			}
		}
		byName.put(NULL_TYPE.getName(), NULL_TYPE);
	}

	public List<FurnitureType> getPlaceAnywhereFurniture() {
		return placeAnywhereFurniture;
	}

	private void initialiseFurnitureType(FurnitureType furnitureType, FurnitureCategoryDictionary categoryDictionary, FurnitureLayoutDictionary layoutDictionary, ItemTypeDictionary itemTypeDictionary) {
		furnitureType.setFurnitureCategory(categoryDictionary.getByName(furnitureType.getCategoryName()));
		if (furnitureType.getFurnitureCategory() == null) {
			throw new RuntimeException("Could not find furniture category: " + furnitureType.getCategoryName() + " for " + furnitureType.getName());
		}

		furnitureType.setDefaultLayout(layoutDictionary.getByName(furnitureType.getDefaultLayoutName()));
		if (furnitureType.getDefaultLayout() == null) {
			throw new RuntimeException("Could not find furniture layout: " + furnitureType.getDefaultLayoutName() + " for " + furnitureType.getName());
		}

		if (furnitureType.getRequirements() != null) {
			for (List<QuantifiedItemType> quantifiedItemTypes : furnitureType.getRequirements().values()) {
				for (QuantifiedItemType quantifiedItemType : quantifiedItemTypes) {
					ItemType itemType = itemTypeDictionary.getByName(quantifiedItemType.getItemTypeName());
					if (itemType == null) {
						throw new RuntimeException("Could not find item type: " + quantifiedItemType.getItemTypeName() + " for " + quantifiedItemType + " in " + furnitureType.getName());
					} else {
						quantifiedItemType.setItemType(itemType);
					}
				}
			}
		}

	}

	public FurnitureType getByName(String name) {
		return byName.get(name);
	}

	public Collection<FurnitureType> getAll() {
		return byName.values();
	}
}
