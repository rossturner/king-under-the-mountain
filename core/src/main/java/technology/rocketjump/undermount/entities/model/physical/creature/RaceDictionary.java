package technology.rocketjump.undermount.entities.model.physical.creature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class RaceDictionary {

	private final GameMaterialDictionary gameMaterialDictionary;
	private final Map<String, Race> byName = new HashMap<>();

	@Inject
	public RaceDictionary(GameMaterialDictionary gameMaterialDictionary) throws IOException {
		this.gameMaterialDictionary = gameMaterialDictionary;

		ObjectMapper objectMapper = new ObjectMapper();
		File itemTypeJsonFile = new File("assets/definitions/types/races.json");
		List<Race> raceList = objectMapper.readValue(FileUtils.readFileToString(itemTypeJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, Race.class));

		for (Race race : raceList) {
			initialise(race);
			byName.put(race.getName(), race);
		}

	}

	public Race getByName(String name) {
		return byName.get(name);
	}

	public Collection<Race> getAll() {
		return byName.values();
	}

	private void initialise(Race race) {
		if (race.getFeatures().getBones() != null) {
			race.getFeatures().getBones().setMaterial(gameMaterialDictionary.getByName(race.getFeatures().getBones().getMaterialName()));
			if (race.getFeatures().getBones().getMaterial() == null) {
				Logger.error("Could not find material " + race.getFeatures().getBones().getMaterialName() +
						" for bones as part of race " + race.getName());
			}
		}

		if (race.getFeatures().getSkin() != null && race.getFeatures().getSkin().getSkinMaterialName() != null) {
			race.getFeatures().getSkin().setSkinMaterial(gameMaterialDictionary.getByName(race.getFeatures().getSkin().getSkinMaterialName()));
			if (race.getFeatures().getSkin().getSkinMaterial() == null) {
				Logger.error("Could not find material " + race.getFeatures().getSkin().getSkinMaterialName() +
						" for skin as part of race " + race.getName());
			}
		}

	}
}
