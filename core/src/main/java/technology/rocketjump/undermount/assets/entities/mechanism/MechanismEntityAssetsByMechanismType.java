package technology.rocketjump.undermount.assets.entities.mechanism;

import technology.rocketjump.undermount.assets.entities.mechanism.model.MechanismEntityAsset;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismTypeDictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MechanismEntityAssetsByMechanismType {

	private Map<MechanismType, MechanismEntityAssetsByLayout> typeMap = new HashMap<>();

	public MechanismEntityAssetsByMechanismType(MechanismTypeDictionary mechanismTypeDictionary) {
		for (MechanismType mechanismType : mechanismTypeDictionary.getAll()) {
			typeMap.put(mechanismType, new MechanismEntityAssetsByLayout());
		}
	}

	public void add(MechanismType mechanismType, MechanismEntityAsset asset) {
		// Assuming all entities have a type specified
		typeMap.get(mechanismType).add(asset);
	}

	public MechanismEntityAsset get(MechanismEntityAttributes attributes) {
		return typeMap.get(attributes.getMechanismType()).get(attributes);
	}

	public List<MechanismEntityAsset> getAll(MechanismEntityAttributes attributes) {
		return typeMap.get(attributes.getMechanismType()).getAll(attributes);
	}

}
