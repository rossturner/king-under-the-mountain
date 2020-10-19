package technology.rocketjump.undermount.assets.entities.item;

import com.badlogic.gdx.utils.IntMap;
import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;

import java.util.List;

public class ItemEntityAssetsByQuantity {

	private IntMap<ItemEntityAssetsByAssetType> quantityMap = new IntMap<>();

	private static final int MIN_QUANTITY = 0;

	public ItemEntityAssetsByQuantity(EntityAssetTypeDictionary entityAssetTypeDictionary, ItemTypeDictionary itemTypeDictionary) {
		final int MAX_QUANTITY = itemTypeDictionary.getConstantsRepo().getWorldConstants().getMaxItemStackSize();
		for (int q = MIN_QUANTITY; q <= MAX_QUANTITY; q++) {
			quantityMap.put(q, new ItemEntityAssetsByAssetType(entityAssetTypeDictionary, itemTypeDictionary));
		}
	}

	public void add(ItemEntityAsset asset) {
		for (int cursor = asset.getMinQuantity(); cursor <= asset.getMaxQuantity(); cursor++) {
			quantityMap.get(cursor).add(asset);
		}
	}

	public ItemEntityAsset get(EntityAssetType assetType, ItemEntityAttributes attributes) {
		return quantityMap.get(attributes.getQuantity()).get(assetType, attributes);
	}

	public List<ItemEntityAsset> getAll(EntityAssetType assetType, ItemEntityAttributes attributes) {
		return quantityMap.get(attributes.getQuantity()).getAll(assetType, attributes);
	}

	public ItemEntityAssetsByAssetType getAssetTypeMapByQuantity(int quantity) {
		return quantityMap.get(quantity);
	}

}
