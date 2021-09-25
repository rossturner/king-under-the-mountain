package technology.rocketjump.undermount.assets.entities.mechanism;

import com.google.inject.ProvidedBy;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.assets.entities.mechanism.model.MechanismEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismTypeDictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ProvidedBy(MechanismEntityAssetDictionaryProvider.class)
@Singleton
public class MechanismEntityAssetDictionary {

	private final MechanismEntityAssetsByAssetType byAssetType;
	private final Map<String, MechanismEntityAsset> assetsByName = new HashMap<>();

	public MechanismEntityAssetDictionary(List<MechanismEntityAsset> completeAssetList, EntityAssetTypeDictionary entityAssetTypeDictionary, 
										  MechanismTypeDictionary mechanismTypeDictionary) {
		this.byAssetType = new MechanismEntityAssetsByAssetType(entityAssetTypeDictionary, mechanismTypeDictionary);
		for (MechanismEntityAsset asset : completeAssetList) {
			byAssetType.add(asset);
			assetsByName.put(asset.getUniqueName(), asset);
		}
	}

	public MechanismEntityAsset getByUniqueName(String uniqueAssetName) {
		MechanismEntityAsset asset = assetsByName.get(uniqueAssetName);
		if (asset != null) {
			return asset;
		} else {
			Logger.error("Could not find asset by name " + uniqueAssetName);
			return null;
		}
	}

	public MechanismEntityAsset getMechanismEntityAsset(EntityAssetType assetType, MechanismEntityAttributes attributes) {
		return byAssetType.get(assetType, attributes);
	}

	public List<MechanismEntityAsset> getAllMatchingAssets(EntityAssetType assetType, MechanismEntityAttributes attributes) {
		return byAssetType.getAll(assetType, attributes);
	}

	public Map<? extends String, ? extends EntityAsset> getAll() {
		return assetsByName;
	}

}
