package technology.rocketjump.undermount.entities.model.physical.creature.body;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.physical.creature.body.organs.OrganDefinitionDictionary;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class BodyStructureDictionary {

	private final OrganDefinitionDictionary organDefinitionDictionary;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final Map<String, BodyStructure> byName = new HashMap<>();

	@Inject
	public BodyStructureDictionary(OrganDefinitionDictionary organDefinitionDictionary, GameMaterialDictionary gameMaterialDictionary) throws IOException {
		this.organDefinitionDictionary = organDefinitionDictionary;
		this.gameMaterialDictionary = gameMaterialDictionary;
		File jsonFile = new File("assets/definitions/bodyStructures.json");
		ObjectMapper objectMapper = new ObjectMapper();
		List<BodyStructure> bodyStructures = objectMapper.readValue(FileUtils.readFileToString(jsonFile),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, BodyStructure.class));

		for (BodyStructure bodyStructure : bodyStructures) {
			initialise(bodyStructure);
			byName.put(bodyStructure.getName(), bodyStructure);
		}
	}

	private void initialise(BodyStructure bodyStructure) {
		bodyStructure.getPartDefinitionByName(bodyStructure.getRootPartName()).ifPresent(bodyStructure::setRootPart);
		if (bodyStructure.getRootPart() == null) {
			throw new RuntimeException("Could not find root part with name " + bodyStructure.getRootPartName() + " for " + bodyStructure.getName());
		}

		if (bodyStructure.getFeatures().getBones() != null) {
			bodyStructure.getFeatures().getBones().setMaterial(gameMaterialDictionary.getByName(bodyStructure.getFeatures().getBones().getMaterialName()));
			if (bodyStructure.getFeatures().getBones().getMaterial() == null) {
				Logger.error("Could not find material " + bodyStructure.getFeatures().getBones().getMaterialName() +
						" for bones as part of body structure " + bodyStructure.getName());
			}
		}

		for (BodyPartDefinition partDefinition : bodyStructure.getPartDefinitions()) {
			for (BodyPartOrgan organ : partDefinition.getOrgans()) {
				organ.setOrganDefinition(organDefinitionDictionary.getByName(organ.getType()));
				if (organ.getOrganDefinition() == null) {
					Logger.error("Can not find organ definition with name " + organ.getType() + " for " + bodyStructure.getName());
				}
			}
		}

	}

	public BodyStructure getByName(String organDefinitionName) {
		return byName.get(organDefinitionName);
	}

}
