package technology.rocketjump.undermount.assets.entities.humanoid;

import technology.rocketjump.undermount.assets.entities.humanoid.model.HumanoidEntityAsset;
import technology.rocketjump.undermount.entities.model.physical.humanoid.Gender;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidEntityAttributes;
import technology.rocketjump.undermount.jobs.model.Profession;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class HumanoidEntityAssetsByGender {

	private Map<Gender, HumanoidEntityAssetsByProfession> genderMap = new EnumMap<>(Gender.class);

	public HumanoidEntityAssetsByGender() {
		for (Gender type : Gender.values()) {
			genderMap.put(type, new HumanoidEntityAssetsByProfession());
		}

	}

	public void add(HumanoidEntityAsset asset) {
		Gender gender = asset.getGender();
		if (gender == null || gender.equals(Gender.ANY)) {
			// Any gender, add to all lists
			for (HumanoidEntityAssetsByProfession assetsByProfession : genderMap.values()) {
				assetsByProfession.add(asset);
			}
		} else {
			// Specific gender only
			genderMap.get(gender).add(asset);
			genderMap.get(Gender.ANY).add(asset);
		}
	}

	public HumanoidEntityAsset get(HumanoidEntityAttributes attributes, Profession primaryProfession) {
		Gender entityGender = attributes.getGender();
		if (entityGender == null) {
			entityGender = Gender.ANY;
		}
		return genderMap.get(entityGender).get(attributes, primaryProfession);
	}

	public List<HumanoidEntityAsset> getAll(HumanoidEntityAttributes attributes, Profession primaryProfession) {
		Gender entityGender = attributes.getGender();
		if (entityGender == null) {
			entityGender = Gender.ANY;
		}
		return genderMap.get(entityGender).getAll(attributes, primaryProfession);
	}

}
