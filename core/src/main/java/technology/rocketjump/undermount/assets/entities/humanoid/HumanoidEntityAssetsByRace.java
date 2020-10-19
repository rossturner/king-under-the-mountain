package technology.rocketjump.undermount.assets.entities.humanoid;

import technology.rocketjump.undermount.assets.entities.humanoid.model.HumanoidEntityAsset;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.humanoid.Race;
import technology.rocketjump.undermount.jobs.model.Profession;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class HumanoidEntityAssetsByRace {

	private Map<Race, HumanoidEntityAssetsByBodyType> raceMap = new EnumMap<>(Race.class);

	public HumanoidEntityAssetsByRace() {
		for (Race race : Race.values()) {
			raceMap.put(race, new HumanoidEntityAssetsByBodyType());
		}
	}

	public void add(HumanoidEntityAsset asset) {
		Race race = asset.getRace();
		if (race == null) {
			// Any race, add to all lists
			for (HumanoidEntityAssetsByBodyType assetsByBodyType : raceMap.values()) {
				assetsByBodyType.add(asset);
			}
		} else {
			// Specific race only
			raceMap.get(race).add(asset);
			raceMap.get(Race.ANY).add(asset);
		}
	}

	public HumanoidEntityAsset get(HumanoidEntityAttributes attributes, Profession primaryProfession) {
		Race race = attributes.getRace();
		if (race == null) {
			race = Race.ANY;
		}
		return raceMap.get(race).get(attributes, primaryProfession);
	}

	public List<HumanoidEntityAsset> getAll(HumanoidEntityAttributes attributes, Profession primaryProfession) {
		Race race = attributes.getRace();
		if (race == null) {
			race = Race.ANY;
		}
		return raceMap.get(race).getAll(attributes, primaryProfession);
	}
}
