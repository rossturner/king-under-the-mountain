package technology.rocketjump.undermount.assets.entities.humanoid;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.ProvidedBy;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.assets.entities.humanoid.model.HumanoidEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidEntityAttributes;
import technology.rocketjump.undermount.jobs.model.Profession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static technology.rocketjump.undermount.assets.entities.humanoid.HumanoidEntityAssetsByProfession.NULL_ENTITY_ASSET;

/**
 * This class stores all the entity assets for use in the game,
 * either for rendering or attaching to entities at creation
 */
@ProvidedBy(HumanoidEntityAssetDictionaryProvider.class)
public class HumanoidEntityAssetDictionary {

	private final HumanoidEntityAssetsByType typeMap;
	private final Map<String, HumanoidEntityAsset> assetsByName = new HashMap<>();

	public HumanoidEntityAssetDictionary(List<HumanoidEntityAsset> completeAssetList,
										 EntityAssetTypeDictionary entityAssetTypeDictionary) {
		this.typeMap = new HumanoidEntityAssetsByType(entityAssetTypeDictionary);
		for (HumanoidEntityAsset asset : completeAssetList) {
			typeMap.add(asset);
			assetsByName.put(asset.getUniqueName(), asset);
		}
		assetsByName.put(NULL_ENTITY_ASSET.getUniqueName(), NULL_ENTITY_ASSET);
	}

	public HumanoidEntityAsset getByUniqueName(String uniqueAssetName) {
		HumanoidEntityAsset asset = assetsByName.get(uniqueAssetName);
		if (asset != null) {
			return asset;
		} else {
			Logger.error("Could not find asset by name " + uniqueAssetName);
			return NULL_ENTITY_ASSET;
		}
	}

	public Map<String, HumanoidEntityAsset> getAll() {
		return assetsByName;
	}

	public HumanoidEntityAsset getMatching(EntityAssetType assetType, HumanoidEntityAttributes attributes, Profession primaryProfession) {
		List<HumanoidEntityAsset> allMatchingAssets = getAllMatchingAssets(assetType, attributes, primaryProfession);

		List<HumanoidEntityAsset> matched = allMatchingAssets.stream().filter(humanoidEntityAsset ->
				(humanoidEntityAsset.getConsciousness() == null || humanoidEntityAsset.getConsciousness().equals(attributes.getConsciousness())) &&
						(humanoidEntityAsset.getSanity() == null || humanoidEntityAsset.getSanity().equals(attributes.getSanity()))
		).collect(Collectors.toList());

		if (matched.size() > 0) {
			return matched.get(new RandomXS128(attributes.getSeed()).nextInt(matched.size()));
		} else {
			return null;
		}
	}

	public List<HumanoidEntityAsset> getAllMatchingAssets(EntityAssetType assetType, HumanoidEntityAttributes attributes, Profession primaryProfession) {
		return typeMap.getAll(assetType, attributes, primaryProfession);
	}

}
