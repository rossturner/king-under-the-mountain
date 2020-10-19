package technology.rocketjump.undermount.assets.entities.furniture;

import com.google.inject.ProvidedBy;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureLayoutDictionary;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ProvidedBy(FurnitureEntityAssetDictionaryProvider.class)
@Singleton
public class FurnitureEntityAssetDictionary {

	private final Map<String, FurnitureEntityAsset> assetsByName = new HashMap<>();
	private FurnitureEntityAssetsByAssetType byAssetType;

	public FurnitureEntityAssetDictionary(List<FurnitureEntityAsset> completeAssetList, EntityAssetTypeDictionary assetTypeDictionary,
										  FurnitureTypeDictionary typeDictionary, FurnitureLayoutDictionary layoutDictionary) {
		byAssetType = new FurnitureEntityAssetsByAssetType(assetTypeDictionary, typeDictionary, layoutDictionary);

		for (FurnitureEntityAsset asset : completeAssetList) {
			assetsByName.put(asset.getUniqueName(), asset);
			byAssetType.add(asset);
		}
	}

	public FurnitureEntityAsset getByUniqueName(String uniqueAssetName) {
		FurnitureEntityAsset asset = assetsByName.get(uniqueAssetName);
		if (asset != null) {
			return asset;
		} else {
			Logger.error("Could not find asset by name " + uniqueAssetName);
			return null;
		}
	}

	public FurnitureEntityAsset getFurnitureEntityAsset(EntityAssetType assetType, FurnitureEntityAttributes attributes) {
		return byAssetType.get(assetType, attributes);
	}

	public List<FurnitureEntityAsset> getAllMatchingAssets(EntityAssetType assetType, FurnitureEntityAttributes attributes) {
		return byAssetType.getAll(assetType, attributes);
	}

	public Map<? extends String, ? extends EntityAsset> getAll() {
		return assetsByName;
	}
}
