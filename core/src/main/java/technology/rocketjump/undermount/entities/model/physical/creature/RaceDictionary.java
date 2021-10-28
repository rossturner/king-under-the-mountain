package technology.rocketjump.undermount.entities.model.physical.creature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.ai.goap.ScheduleDictionary;
import technology.rocketjump.undermount.entities.behaviour.creature.CreatureBehaviourDictionary;
import technology.rocketjump.undermount.entities.model.physical.creature.body.BodyStructureDictionary;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.plant.SpeciesColor;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.lang.String.format;
import static technology.rocketjump.undermount.entities.model.EntityType.CREATURE;
import static technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesDictionary.initialiseSpeciesColor;

@Singleton
public class RaceDictionary {

	private final GameMaterialDictionary gameMaterialDictionary;
	private final ItemTypeDictionary itemTypeDictionary;
	private final BodyStructureDictionary bodyStructureDictionary;
	private final CreatureBehaviourDictionary creatureBehaviourDictionary;
	private final ScheduleDictionary scheduleDictionary;
	private final Map<String, Race> byName = new HashMap<>();

	@Inject
	public RaceDictionary(GameMaterialDictionary gameMaterialDictionary, ItemTypeDictionary itemTypeDictionary, BodyStructureDictionary bodyStructureDictionary,
						  CreatureBehaviourDictionary creatureBehaviourDictionary, ScheduleDictionary scheduleDictionary) throws IOException {
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.bodyStructureDictionary = bodyStructureDictionary;
		this.creatureBehaviourDictionary = creatureBehaviourDictionary;
		this.scheduleDictionary = scheduleDictionary;

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
		race.setBodyStructure(bodyStructureDictionary.getByName(race.getBodyStructureName()));
		if (race.getBodyStructure() == null) {
			throw new RuntimeException(format("Could not find body structure with name %s for race %s", race.getBodyStructureName(), race.getName()));
		}

		if (race.getFeatures().getMeat() != null) {
			race.getFeatures().getMeat().setItemType(itemTypeDictionary.getByName(race.getFeatures().getMeat().getItemTypeName()));
			if (race.getFeatures().getMeat().getItemType() == null) {
				Logger.error("Could not find item type " + race.getFeatures().getMeat().getItemTypeName() +
						" for meat as part of race " + race.getName());
			}
			race.getFeatures().getMeat().setMaterial(gameMaterialDictionary.getByName(race.getFeatures().getMeat().getMaterialName()));
			if (race.getFeatures().getMeat().getMaterial() == null) {
				Logger.error("Could not find material " + race.getFeatures().getMeat().getMaterialName() +
						" for meat as part of race " + race.getName());
			}
		}

		if (race.getFeatures().getBones() != null) {
			race.getFeatures().getBones().setMaterial(gameMaterialDictionary.getByName(race.getFeatures().getBones().getMaterialName()));
			if (race.getFeatures().getBones().getMaterial() == null) {
				Logger.error("Could not find material " + race.getFeatures().getBones().getMaterialName() +
						" for bones as part of race " + race.getName());
			}
		}

		for (SpeciesColor speciesColor : race.getColors().values()) {
			initialiseSpeciesColor(CREATURE, speciesColor);
		}

		if (race.getFeatures().getSkin() != null && race.getFeatures().getSkin().getSkinMaterialName() != null) {
			race.getFeatures().getSkin().setSkinMaterial(gameMaterialDictionary.getByName(race.getFeatures().getSkin().getSkinMaterialName()));
			if (race.getFeatures().getSkin().getSkinMaterial() == null) {
				Logger.error("Could not find material " + race.getFeatures().getSkin().getSkinMaterialName() +
						" for skin as part of race " + race.getName());
			}
		}

		if (race.getBehaviour() == null) {
			throw new RuntimeException("Behaviour section does not exist for " + race.getName());
		}
		race.getBehaviour().setBehaviourClass(creatureBehaviourDictionary.getByName(race.getBehaviour().getBehaviourName()));
		if (race.getBehaviour().getBehaviourClass() == null) {
			Logger.error(String.format("Could not find behaviour with name %s for race %s", race.getBehaviour().getBehaviourName(), race.getName()));
		}
		race.getBehaviour().setSchedule(scheduleDictionary.getByName(race.getBehaviour().getScheduleName()));
		if (race.getBehaviour().getSchedule() == null) {
			Logger.error(String.format("Could not find scheudle %s for race %s", race.getBehaviour().getBehaviourName(), race.getName()));
		}

	}
}
