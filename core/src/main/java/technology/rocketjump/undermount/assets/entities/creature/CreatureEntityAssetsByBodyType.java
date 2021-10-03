package technology.rocketjump.undermount.assets.entities.creature;

import technology.rocketjump.undermount.assets.entities.creature.model.CreatureBodyType;
import technology.rocketjump.undermount.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.jobs.model.Profession;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CreatureEntityAssetsByBodyType {

	private Map<CreatureBodyType, CreatureEntityAssetsByGender> bodyTypeMap = new EnumMap<>(CreatureBodyType.class);

	public CreatureEntityAssetsByBodyType() {
		for (CreatureBodyType type : CreatureBodyType.values()) {
			bodyTypeMap.put(type, new CreatureEntityAssetsByGender());
		}
	}

	public void add(CreatureEntityAsset asset) {
		CreatureBodyType bodyType = asset.getBodyType();
		if (bodyType == null) {
			// Any body type, add to all lists
			for (CreatureEntityAssetsByGender creatureEntityAssetsByGender : bodyTypeMap.values()) {
				creatureEntityAssetsByGender.add(asset);
			}
		} else {
			// Specific bodytype only
			bodyTypeMap.get(bodyType).add(asset);
			bodyTypeMap.get(CreatureBodyType.ANY).add(asset);
		}
	}

	public CreatureEntityAsset get(CreatureEntityAttributes attributes, Profession primaryProfession) {
		CreatureBodyType bodyType = attributes.getBodyType();
		if (bodyType == null) {
			bodyType = CreatureBodyType.ANY;
		}
		return bodyTypeMap.get(bodyType).get(attributes, primaryProfession);
	}

	public List<CreatureEntityAsset> getAll(CreatureEntityAttributes attributes, Profession primaryProfession) {
		CreatureBodyType bodyType = attributes.getBodyType();
		if (bodyType == null) {
			bodyType = CreatureBodyType.ANY;
		}
		return bodyTypeMap.get(bodyType).getAll(attributes, primaryProfession);
	}
}
