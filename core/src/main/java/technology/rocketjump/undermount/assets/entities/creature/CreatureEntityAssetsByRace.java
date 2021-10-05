package technology.rocketjump.undermount.assets.entities.creature;

import technology.rocketjump.undermount.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.Race;
import technology.rocketjump.undermount.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.undermount.jobs.model.Profession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatureEntityAssetsByRace {

//	private static final Race NO_RACE = new Race();

	private Map<Race, CreatureEntityAssetsByBodyType> raceMap = new HashMap<>();

	public CreatureEntityAssetsByRace(RaceDictionary raceDictionary) {
//		NO_RACE.setName("None");
		// not adding all race placeholders
//		for (Race race : raceDictionary.getAll()) {
//			raceMap.put(race, new CreatureEntityAssetsByBodyType());
//		}
	}

	public void add(CreatureEntityAsset asset) {
		Race race = asset.getRace();
//		if (race == null) {
			// Any race, add to all lists
//			for (CreatureEntityAssetsByBodyType assetsByBodyType : raceMap.values()) {
//				assetsByBodyType.add(asset);
//			}
//		} else {
		// currently assuming all creature assets are race-specific
			// Specific race only
			raceMap.computeIfAbsent(race, a -> new CreatureEntityAssetsByBodyType(race.getBodyShapes())).add(asset);
//			raceMap.get(NO_RACE).add(asset);
//		}
	}

	public CreatureEntityAsset get(CreatureEntityAttributes attributes, Profession primaryProfession) {
		Race race = attributes.getRace();
//		if (race == null) {
//			race = NO_RACE;
//		}
		return raceMap.get(race).get(attributes, primaryProfession);
	}

	public List<CreatureEntityAsset> getAll(CreatureEntityAttributes attributes, Profession primaryProfession) {
		Race race = attributes.getRace();
//		if (race == null) {
//			race = Race.ANY;
//		}
		return raceMap.get(race).getAll(attributes, primaryProfession);
	}
}
