package technology.rocketjump.undermount.assets.entities.item;

import com.google.inject.ProvidedBy;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ProvidedBy(ItemEntityAssetDictionaryProvider.class)
@Singleton
public class ItemEntityAssetDictionary {

	private final ItemEntityAssetsByQuantity quantityMap;
	private final Map<String, ItemEntityAsset> assetsByName = new HashMap<>();

	public ItemEntityAssetDictionary(List<ItemEntityAsset> completeAssetList, EntityAssetTypeDictionary entityAssetTypeDictionary, ItemTypeDictionary itemTypeDictionary) {
		this.quantityMap = new ItemEntityAssetsByQuantity(entityAssetTypeDictionary, itemTypeDictionary);
		for (ItemEntityAsset asset : completeAssetList) {
			quantityMap.add(asset);
			assetsByName.put(asset.getUniqueName(), asset);
		}
	}

	public ItemEntityAsset getByUniqueName(String uniqueAssetName) {
		ItemEntityAsset asset = assetsByName.get(uniqueAssetName);
		if (asset != null) {
			return asset;
		} else {
			Logger.error("Could not find asset by name " + uniqueAssetName);
			return null;
		}
	}

	public ItemEntityAsset getItemEntityAsset(EntityAssetType assetType, ItemEntityAttributes attributes) {
		return quantityMap.get(assetType, attributes);
	}

	public List<ItemEntityAsset> getAllMatchingAssets(EntityAssetType assetType, ItemEntityAttributes attributes) {
		return quantityMap.getAll(assetType, attributes);
	}

	public Map<? extends String, ? extends EntityAsset> getAll() {
		return assetsByName;
	}

	public ItemEntityAssetsByQuantity getQuantityMap() {
		return quantityMap;
	}
}
