package technology.rocketjump.undermount.assets.entities.creature;

import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.jobs.model.Profession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatureEntityAssetsByType {

	private Map<EntityAssetType, CreatureEntityAssetsByRace> typeMap = new HashMap<>();

	public CreatureEntityAssetsByType(EntityAssetTypeDictionary typeDictionary) {
		for (EntityAssetType assetType : typeDictionary.getAll()) {
			typeMap.put(assetType, new CreatureEntityAssetsByRace());
		}
	}

	public void add(CreatureEntityAsset asset) {
		// Assuming all entities have a type specified
		typeMap.get(asset.getType()).add(asset);
	}

	public CreatureEntityAsset get(EntityAssetType type, CreatureEntityAttributes attributes, Profession primaryProfession) {
		return typeMap.get(type).get(attributes, primaryProfession);
	}

	public List<CreatureEntityAsset> getAll(EntityAssetType type, CreatureEntityAttributes attributes, Profession primaryProfession) {
		return typeMap.get(type).getAll(attributes, primaryProfession);
	}

}
