package technology.rocketjump.undermount.assets.entities.item;

import technology.rocketjump.undermount.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemEntityAssetsByItemType {

	private Map<ItemType, ItemEntityAssetsBySize> typeMap = new HashMap<>();

	public ItemEntityAssetsByItemType(ItemTypeDictionary itemTypeDictionary) {
		for (ItemType itemType : itemTypeDictionary.getAll()) {
			typeMap.put(itemType, new ItemEntityAssetsBySize());
		}
	}

	public void add(ItemType itemType, ItemEntityAsset asset) {
		// Assuming all entities have a type specified
		typeMap.get(itemType).add(asset);
	}

	public ItemEntityAsset get(ItemEntityAttributes attributes) {
		return typeMap.get(attributes.getItemType()).get(attributes);
	}

	public List<ItemEntityAsset> getAll(ItemEntityAttributes attributes) {
		return typeMap.get(attributes.getItemType()).getAll(attributes);
	}

	public ItemEntityAssetsBySize getSizeMapByItemType(ItemType itemType) {
		return typeMap.get(itemType);
	}
}
