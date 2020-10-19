package technology.rocketjump.undermount.assets.entities.item;

import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.undermount.assets.entities.model.EntityAssetType.NULL_ENTITY_ASSET_TYPE;

public class ItemEntityAssetsByAssetType {

	private final ItemTypeDictionary itemTypeDictionary;
	private Map<EntityAssetType, ItemEntityAssetsByItemType> typeMap = new HashMap<>();

	public ItemEntityAssetsByAssetType(EntityAssetTypeDictionary typeDictionary, ItemTypeDictionary itemTypeDictionary) {
		this.itemTypeDictionary = itemTypeDictionary;
		for (EntityAssetType assetType : typeDictionary.getByEntityType(EntityType.ITEM)) {
			typeMap.put(assetType, new ItemEntityAssetsByItemType(itemTypeDictionary));
		}
		typeMap.put(NULL_ENTITY_ASSET_TYPE, new ItemEntityAssetsByItemType(itemTypeDictionary));
	}

	public void add(ItemEntityAsset asset) {
		// Assuming all entities have a type specified
		if (!typeMap.containsKey(asset.getType())) {
			throw new RuntimeException("Unrecognised asset type " + asset.getType() + " for " + asset.getUniqueName());
		}

		if (asset.getItemTypeNames() != null && !asset.getItemTypeNames().isEmpty()) {
			for (String itemTypeName : asset.getItemTypeNames()) {
				typeMap.get(asset.getType()).add(itemTypeDictionary.getByName(itemTypeName), asset);
				typeMap.get(NULL_ENTITY_ASSET_TYPE).add(itemTypeDictionary.getByName(itemTypeName), asset);
			}
		} else {
			typeMap.get(asset.getType()).add(itemTypeDictionary.getByName(asset.getItemTypeName()), asset);
			typeMap.get(NULL_ENTITY_ASSET_TYPE).add(itemTypeDictionary.getByName(asset.getItemTypeName()), asset);
		}
	}

	public ItemEntityAsset get(EntityAssetType entityAssetType, ItemEntityAttributes attributes) {
		if (entityAssetType == null) {
			entityAssetType = NULL_ENTITY_ASSET_TYPE;
		}
		return typeMap.get(entityAssetType).get(attributes);
	}

	public List<ItemEntityAsset> getAll(EntityAssetType entityAssetType, ItemEntityAttributes attributes) {
		return typeMap.get(entityAssetType).getAll(attributes);
	}

	public ItemEntityAssetsByItemType getItemTypeMapByAssetType(EntityAssetType assetType) {
		return typeMap.get(assetType);
	}

}
