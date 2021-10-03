package technology.rocketjump.undermount.assets.entities.creature;

import technology.rocketjump.undermount.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.Race;
import technology.rocketjump.undermount.jobs.model.Profession;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CreatureEntityAssetsByRace {

	private Map<Race, CreatureEntityAssetsByBodyType> raceMap = new EnumMap<>(Race.class);

	public CreatureEntityAssetsByRace() {
		for (Race race : Race.values()) {
			raceMap.put(race, new CreatureEntityAssetsByBodyType());
		}
	}

	public void add(CreatureEntityAsset asset) {
		Race race = asset.getRace();
		if (race == null) {
			// Any race, add to all lists
			for (CreatureEntityAssetsByBodyType assetsByBodyType : raceMap.values()) {
				assetsByBodyType.add(asset);
			}
		} else {
			// Specific race only
			raceMap.get(race).add(asset);
			raceMap.get(Race.ANY).add(asset);
		}
	}

	public CreatureEntityAsset get(CreatureEntityAttributes attributes, Profession primaryProfession) {
		Race race = attributes.getRace();
		if (race == null) {
			race = Race.ANY;
		}
		return raceMap.get(race).get(attributes, primaryProfession);
	}

	public List<CreatureEntityAsset> getAll(CreatureEntityAttributes attributes, Profession primaryProfession) {
		Race race = attributes.getRace();
		if (race == null) {
			race = Race.ANY;
		}
		return raceMap.get(race).getAll(attributes, primaryProfession);
	}
}
