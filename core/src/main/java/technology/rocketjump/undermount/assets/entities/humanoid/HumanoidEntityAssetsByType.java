package technology.rocketjump.undermount.assets.entities.humanoid;

import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.assets.entities.humanoid.model.HumanoidEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidEntityAttributes;
import technology.rocketjump.undermount.jobs.model.Profession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HumanoidEntityAssetsByType {

	private Map<EntityAssetType, HumanoidEntityAssetsByRace> typeMap = new HashMap<>();

	public HumanoidEntityAssetsByType(EntityAssetTypeDictionary typeDictionary) {
		for (EntityAssetType assetType : typeDictionary.getAll()) {
			typeMap.put(assetType, new HumanoidEntityAssetsByRace());
		}
	}

	public void add(HumanoidEntityAsset asset) {
		// Assuming all entities have a type specified
		typeMap.get(asset.getType()).add(asset);
	}

	public HumanoidEntityAsset get(EntityAssetType type, HumanoidEntityAttributes attributes, Profession primaryProfession) {
		return typeMap.get(type).get(attributes, primaryProfession);
	}

	public List<HumanoidEntityAsset> getAll(EntityAssetType type, HumanoidEntityAttributes attributes, Profession primaryProfession) {
		return typeMap.get(type).getAll(attributes, primaryProfession);
	}

}
