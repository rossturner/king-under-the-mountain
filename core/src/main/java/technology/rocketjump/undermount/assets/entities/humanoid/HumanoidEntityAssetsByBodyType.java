package technology.rocketjump.undermount.assets.entities.humanoid;

import technology.rocketjump.undermount.assets.entities.humanoid.model.HumanoidBodyType;
import technology.rocketjump.undermount.assets.entities.humanoid.model.HumanoidEntityAsset;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidEntityAttributes;
import technology.rocketjump.undermount.jobs.model.Profession;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class HumanoidEntityAssetsByBodyType {

	private Map<HumanoidBodyType, HumanoidEntityAssetsByGender> bodyTypeMap = new EnumMap<>(HumanoidBodyType.class);

	public HumanoidEntityAssetsByBodyType() {
		for (HumanoidBodyType type : HumanoidBodyType.values()) {
			bodyTypeMap.put(type, new HumanoidEntityAssetsByGender());
		}
	}

	public void add(HumanoidEntityAsset asset) {
		HumanoidBodyType bodyType = asset.getBodyType();
		if (bodyType == null) {
			// Any body type, add to all lists
			for (HumanoidEntityAssetsByGender humanoidEntityAssetsByGender : bodyTypeMap.values()) {
				humanoidEntityAssetsByGender.add(asset);
			}
		} else {
			// Specific bodytype only
			bodyTypeMap.get(bodyType).add(asset);
			bodyTypeMap.get(HumanoidBodyType.ANY).add(asset);
		}
	}

	public HumanoidEntityAsset get(HumanoidEntityAttributes attributes, Profession primaryProfession) {
		HumanoidBodyType bodyType = attributes.getBodyType();
		if (bodyType == null) {
			bodyType = HumanoidBodyType.ANY;
		}
		return bodyTypeMap.get(bodyType).get(attributes, primaryProfession);
	}

	public List<HumanoidEntityAsset> getAll(HumanoidEntityAttributes attributes, Profession primaryProfession) {
		HumanoidBodyType bodyType = attributes.getBodyType();
		if (bodyType == null) {
			bodyType = HumanoidBodyType.ANY;
		}
		return bodyTypeMap.get(bodyType).getAll(attributes, primaryProfession);
	}
}
