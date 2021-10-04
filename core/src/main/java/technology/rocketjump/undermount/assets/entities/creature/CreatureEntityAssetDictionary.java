package technology.rocketjump.undermount.assets.entities.creature;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.ProvidedBy;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.undermount.jobs.model.Profession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static technology.rocketjump.undermount.assets.entities.creature.CreatureEntityAssetsByProfession.NULL_ENTITY_ASSET;

/**
 * This class stores all the entity assets for use in the game,
 * either for rendering or attaching to entities at creation
 */
@ProvidedBy(CreatureEntityAssetDictionaryProvider.class)
public class CreatureEntityAssetDictionary {

	private final CreatureEntityAssetsByType typeMap;
	private final Map<String, CreatureEntityAsset> assetsByName = new HashMap<>();

	public CreatureEntityAssetDictionary(List<CreatureEntityAsset> completeAssetList,
										 EntityAssetTypeDictionary entityAssetTypeDictionary, RaceDictionary raceDictionary) {
		this.typeMap = new CreatureEntityAssetsByType(entityAssetTypeDictionary, raceDictionary);
		for (CreatureEntityAsset asset : completeAssetList) {
			typeMap.add(asset);
			assetsByName.put(asset.getUniqueName(), asset);
		}
		assetsByName.put(NULL_ENTITY_ASSET.getUniqueName(), NULL_ENTITY_ASSET);
	}

	public CreatureEntityAsset getByUniqueName(String uniqueAssetName) {
		CreatureEntityAsset asset = assetsByName.get(uniqueAssetName);
		if (asset != null) {
			return asset;
		} else {
			Logger.error("Could not find asset by name " + uniqueAssetName);
			return NULL_ENTITY_ASSET;
		}
	}

	public Map<String, CreatureEntityAsset> getAll() {
		return assetsByName;
	}

	public CreatureEntityAsset getMatching(EntityAssetType assetType, CreatureEntityAttributes attributes, Profession primaryProfession) {
		List<CreatureEntityAsset> allMatchingAssets = getAllMatchingAssets(assetType, attributes, primaryProfession);

		List<CreatureEntityAsset> matched = allMatchingAssets.stream().filter(humanoidEntityAsset ->
				(humanoidEntityAsset.getConsciousness() == null || humanoidEntityAsset.getConsciousness().equals(attributes.getConsciousness())) &&
						(humanoidEntityAsset.getSanity() == null || humanoidEntityAsset.getSanity().equals(attributes.getSanity()))
		).collect(Collectors.toList());

		if (matched.size() > 0) {
			return matched.get(new RandomXS128(attributes.getSeed()).nextInt(matched.size()));
		} else {
			return null;
		}
	}

	public List<CreatureEntityAsset> getAllMatchingAssets(EntityAssetType assetType, CreatureEntityAttributes attributes, Profession primaryProfession) {
		return typeMap.getAll(assetType, attributes, primaryProfession);
	}

}
