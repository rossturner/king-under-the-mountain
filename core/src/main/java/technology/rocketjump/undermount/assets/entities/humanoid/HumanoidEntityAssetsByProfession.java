package technology.rocketjump.undermount.assets.entities.humanoid;

import technology.rocketjump.undermount.assets.entities.humanoid.model.HumanoidBodyType;
import technology.rocketjump.undermount.assets.entities.humanoid.model.HumanoidEntityAsset;
import technology.rocketjump.undermount.entities.model.physical.humanoid.Gender;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.humanoid.Race;
import technology.rocketjump.undermount.jobs.model.Profession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.undermount.jobs.ProfessionDictionary.NULL_PROFESSION;

public class HumanoidEntityAssetsByProfession {

	private Map<String, List<HumanoidEntityAsset>> professionNameMap = new HashMap<>();

	public static final HumanoidEntityAsset NULL_ENTITY_ASSET;

	static {
		NULL_ENTITY_ASSET = new HumanoidEntityAsset();
		NULL_ENTITY_ASSET.setBodyType(HumanoidBodyType.ANY);
		NULL_ENTITY_ASSET.setGender(Gender.ANY);
		NULL_ENTITY_ASSET.setRace(Race.ANY);
		NULL_ENTITY_ASSET.setType(null);
		NULL_ENTITY_ASSET.setUniqueName("None");
	}

	public HumanoidEntityAssetsByProfession() {
		professionNameMap.put(NULL_PROFESSION.getName(), new ArrayList<>());
	}

	public void add(HumanoidEntityAsset asset) {
		String professionName = asset.getProfession();
		if (professionName == null) {
			professionName = NULL_PROFESSION.getName();
		}
		List<HumanoidEntityAsset> assets = professionNameMap.get(professionName);
		if (assets == null) {
			assets = new ArrayList<>();
			professionNameMap.put(professionName, assets);
		}
		assets.add(asset);
	}

	public HumanoidEntityAsset get(HumanoidEntityAttributes attributes, Profession primaryProfession) {
		List<HumanoidEntityAsset> assetsForProfession = professionNameMap.get(primaryProfession.getName());

		if (assetsForProfession == null || assetsForProfession.isEmpty()) {
			assetsForProfession = professionNameMap.get(NULL_PROFESSION.getName());
		}

		if (assetsForProfession == null || assetsForProfession.isEmpty()) {
			return NULL_ENTITY_ASSET;
		} else {
			return assetsForProfession.get((Math.abs((int)attributes.getSeed())) % assetsForProfession.size());
		}
	}

	public List<HumanoidEntityAsset> getAll(HumanoidEntityAttributes attributes, Profession primaryProfession) {
		List<HumanoidEntityAsset> assetsForProfession = professionNameMap.get(primaryProfession.getName());

		if (assetsForProfession == null || assetsForProfession.isEmpty()) {
			assetsForProfession = professionNameMap.get(NULL_PROFESSION.getName());
		}

		return assetsForProfession;
	}
}
