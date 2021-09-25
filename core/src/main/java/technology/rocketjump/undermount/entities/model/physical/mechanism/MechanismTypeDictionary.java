package technology.rocketjump.undermount.entities.model.physical.mechanism;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.jobs.ProfessionDictionary;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class MechanismTypeDictionary {

	private final Map<String, MechanismType> byName = new HashMap<>();
	private final ItemTypeDictionary itemTypeDictionary;
	private final ProfessionDictionary professionDictionary;

	@Inject
	public MechanismTypeDictionary(ItemTypeDictionary itemTypeDictionary, ProfessionDictionary professionDictionary) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		this.itemTypeDictionary = itemTypeDictionary;
		this.professionDictionary = professionDictionary;
		File typesJsonFile = new File("assets/definitions/types/mechanismTypes.json");
		List<MechanismType> typeList = objectMapper.readValue(FileUtils.readFileToString(typesJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, MechanismType.class));

		for (MechanismType mechanismType : typeList) {
			initialiseTransientFields(mechanismType);
			byName.put(mechanismType.getName(), mechanismType);
		}
	}

	private void initialiseTransientFields(MechanismType mechanismType) {
		if (mechanismType.getRelatedItemTypeName() != null) {
			mechanismType.setRelatedItemType(itemTypeDictionary.getByName(mechanismType.getRelatedItemTypeName()));
			if (mechanismType.getRelatedItemType() == null) {
				Logger.error("Could not find item type with name " + mechanismType.getRelatedItemTypeName() + " for mechanism type " + mechanismType.getName());
			}
		}

		if (mechanismType.getRelatedProfessionName() != null) {
			mechanismType.setRelatedProfession(professionDictionary.getByName(mechanismType.getRelatedProfessionName()));
			if (mechanismType.getRelatedProfession() == null) {
				Logger.error("Could not find profession with name " + mechanismType.getRelatedProfessionName() + " for mechanism type " + mechanismType.getName());
			}
		}
	}

	public MechanismType getByName(String effectTypeName) {
		return byName.get(effectTypeName);
	}

	public Collection<MechanismType> getAll() {
		return byName.values();
	}
}
