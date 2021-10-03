package technology.rocketjump.undermount.assets.entities.creature;

import technology.rocketjump.undermount.assets.entities.creature.model.CreatureBodyType;
import technology.rocketjump.undermount.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.Gender;
import technology.rocketjump.undermount.entities.model.physical.creature.Race;
import technology.rocketjump.undermount.jobs.model.Profession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.undermount.jobs.ProfessionDictionary.NULL_PROFESSION;

public class CreatureEntityAssetsByProfession {

	private Map<String, List<CreatureEntityAsset>> professionNameMap = new HashMap<>();

	public static final CreatureEntityAsset NULL_ENTITY_ASSET;

	static {
		NULL_ENTITY_ASSET = new CreatureEntityAsset();
		NULL_ENTITY_ASSET.setBodyType(CreatureBodyType.ANY);
		NULL_ENTITY_ASSET.setGender(Gender.ANY);
		NULL_ENTITY_ASSET.setRace(Race.ANY);
		NULL_ENTITY_ASSET.setType(null);
		NULL_ENTITY_ASSET.setUniqueName("None");
	}

	public CreatureEntityAssetsByProfession() {
		professionNameMap.put(NULL_PROFESSION.getName(), new ArrayList<>());
	}

	public void add(CreatureEntityAsset asset) {
		String professionName = asset.getProfession();
		if (professionName == null) {
			professionName = NULL_PROFESSION.getName();
		}
		List<CreatureEntityAsset> assets = professionNameMap.get(professionName);
		if (assets == null) {
			assets = new ArrayList<>();
			professionNameMap.put(professionName, assets);
		}
		assets.add(asset);
	}

	public CreatureEntityAsset get(CreatureEntityAttributes attributes, Profession primaryProfession) {
		List<CreatureEntityAsset> assetsForProfession = professionNameMap.get(primaryProfession.getName());

		if (assetsForProfession == null || assetsForProfession.isEmpty()) {
			assetsForProfession = professionNameMap.get(NULL_PROFESSION.getName());
		}

		if (assetsForProfession == null || assetsForProfession.isEmpty()) {
			return NULL_ENTITY_ASSET;
		} else {
			return assetsForProfession.get((Math.abs((int)attributes.getSeed())) % assetsForProfession.size());
		}
	}

	public List<CreatureEntityAsset> getAll(CreatureEntityAttributes attributes, Profession primaryProfession) {
		if (primaryProfession == null) {
			primaryProfession = NULL_PROFESSION;
		}
		List<CreatureEntityAsset> assetsForProfession = professionNameMap.get(primaryProfession.getName());

		if (assetsForProfession == null || assetsForProfession.isEmpty()) {
			assetsForProfession = professionNameMap.get(NULL_PROFESSION.getName());
		}

		return assetsForProfession;
	}
}
