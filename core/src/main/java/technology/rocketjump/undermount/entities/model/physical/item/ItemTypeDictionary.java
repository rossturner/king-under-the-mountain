package technology.rocketjump.undermount.entities.model.physical.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.constants.ConstantsRepo;
import technology.rocketjump.undermount.jobs.CraftingTypeDictionary;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.rooms.StockpileGroup;
import technology.rocketjump.undermount.rooms.StockpileGroupDictionary;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.util.Collections.emptyList;

@Singleton
public class ItemTypeDictionary {

	private final ConstantsRepo constantsRepo;
	private Map<String, ItemType> byName = new HashMap<>();
	private List<ItemType> allTypesList = new ArrayList<>();
	private Map<CraftingType, List<ItemType>> byCraftingType = new HashMap<>();
	private final Map<StockpileGroup, List<ItemType>> byStockpileGroup = new HashMap<>();

	@Inject
	public ItemTypeDictionary(CraftingTypeDictionary craftingTypeDictionary,
							  StockpileGroupDictionary stockpileGroupDictionary,
							  SoundAssetDictionary soundAssetDictionary,
							  ConstantsRepo constantsRepo) throws IOException {
		this.constantsRepo = constantsRepo;
		ObjectMapper objectMapper = new ObjectMapper();
		File itemTypeJsonFile = new File("assets/definitions/types/itemTypes.json");
		List<ItemType> itemTypeList = objectMapper.readValue(FileUtils.readFileToString(itemTypeJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, ItemType.class));

		itemTypeList.sort(Comparator.comparing(ItemType::getItemTypeName));

		for (ItemType itemType : itemTypeList) {
			if (itemType.getRelatedCraftingTypeNames() == null) {
				itemType.setRelatedCraftingTypeNames(new ArrayList<>());
			}
			if (itemType.getRelatedCraftingTypes() == null) {
				itemType.setRelatedCraftingTypes(new ArrayList<>());
			}
			for (String relatedCraftingName : itemType.getRelatedCraftingTypeNames()) {
				CraftingType craftingType = craftingTypeDictionary.getByName(relatedCraftingName);
				if (craftingType == null) {
					Logger.error("Could not find related crafting type by name: " + relatedCraftingName + " for " + itemType.getItemTypeName());
				} else {
					itemType.getRelatedCraftingTypes().add(craftingType);

					List<ItemType> itemTypesByCraftingType = byCraftingType.computeIfAbsent(craftingType, k -> new ArrayList<>());
					itemTypesByCraftingType.add(itemType);
				}
			}

			if (itemType.getStockpileGroupName() != null) {
				itemType.setStockpileGroup(stockpileGroupDictionary.getByName(itemType.getStockpileGroupName()));
				if (itemType.getStockpileGroup() == null) {
					Logger.error("Could not find stockpile group '"+itemType.getStockpileGroupName()+"' for itemType " + itemType.getItemTypeName());
				} else {
					byStockpileGroup.computeIfAbsent(itemType.getStockpileGroup(), a -> new ArrayList<>()).add(itemType);
				}
			}

			if (itemType.getPlacementSoundAssetName() != null) {
				itemType.setPlacementSoundAsset(soundAssetDictionary.getByName(itemType.getPlacementSoundAssetName()));
				if (itemType.getPlacementSoundAsset() == null) {
					Logger.error("Could not find sound asset with name " + itemType.getPlacementSoundAssetName() + " for item type " + itemType.getItemTypeName());
				}
			}

			if (itemType.getConsumeSoundAssetName() != null) {
				itemType.setConsumeSoundAsset(soundAssetDictionary.getByName(itemType.getConsumeSoundAssetName()));
				if (itemType.getConsumeSoundAsset() == null) {
					Logger.error("Could not find sound asset with name " + itemType.getConsumeSoundAssetName() + " for item type " + itemType.getItemTypeName());
				}
			}

			byName.put(itemType.getItemTypeName(), itemType);
			allTypesList.add(itemType);
		}


		for (CraftingType craftingType : craftingTypeDictionary.getAll()) {
			if (craftingType.getDefaultItemTypeName() != null) {
				craftingType.setDefaultItemType(getByName(craftingType.getDefaultItemTypeName()));
			}
		}

	}

	public ItemType getByName(String itemTypeName) {
		return byName.get(itemTypeName);
	}

	public List<ItemType> getAll() {
		return allTypesList;
	}

	public List<ItemType> getByCraftingType(CraftingType craftingType) {
		return byCraftingType.getOrDefault(craftingType, emptyList());
	}

	public List<ItemType> getByStockpileGroup(StockpileGroup stockpileGroup) {
		return byStockpileGroup.getOrDefault(stockpileGroup, emptyList());
	}

	public ConstantsRepo getConstantsRepo() {
		return constantsRepo;
	}
}
