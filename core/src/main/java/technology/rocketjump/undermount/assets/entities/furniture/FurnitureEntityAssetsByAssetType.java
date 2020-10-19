package technology.rocketjump.undermount.assets.entities.furniture;

import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureLayoutDictionary;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.undermount.assets.entities.model.EntityAssetType.NULL_ENTITY_ASSET_TYPE;

public class FurnitureEntityAssetsByAssetType {

	private final Map<EntityAssetType, FurnitureEntityAssetsByFurnitureType> byAssetType = new HashMap<>();

	public FurnitureEntityAssetsByAssetType(EntityAssetTypeDictionary typeDictionary, FurnitureTypeDictionary furnitureTypeDictionary,
											FurnitureLayoutDictionary layoutDictionary) {
		for (EntityAssetType assetType : typeDictionary.getByEntityType(EntityType.FURNITURE)) {
			byAssetType.put(assetType, new FurnitureEntityAssetsByFurnitureType(furnitureTypeDictionary, layoutDictionary));
		}
		byAssetType.put(EntityAssetType.NULL_ENTITY_ASSET_TYPE, new FurnitureEntityAssetsByFurnitureType(furnitureTypeDictionary, layoutDictionary));
	}

	public void add(FurnitureEntityAsset asset) {
		// Assuming all entities have a type specified
		if (!byAssetType.containsKey(asset.getType())) {
			throw new RuntimeException("Unrecognised asset type " + asset.getType() + " for " + asset.getUniqueName());
		}

		byAssetType.get(asset.getType()).add(asset);
		byAssetType.get(NULL_ENTITY_ASSET_TYPE).add(asset);
	}

	public FurnitureEntityAsset get(EntityAssetType entityAssetType, FurnitureEntityAttributes attributes) {
		if (entityAssetType == null) {
			entityAssetType = NULL_ENTITY_ASSET_TYPE;
		}
		return byAssetType.get(entityAssetType).get(attributes);
	}

	public List<FurnitureEntityAsset> getAll(EntityAssetType entityAssetType, FurnitureEntityAttributes attributes) {
		return byAssetType.get(entityAssetType).getAll(attributes);
	}

}
