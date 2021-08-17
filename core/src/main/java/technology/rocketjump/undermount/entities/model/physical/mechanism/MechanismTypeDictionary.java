package technology.rocketjump.undermount.entities.model.physical.mechanism;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class MechanismTypeDictionary {

	private final Map<String, MechanismType> byName = new HashMap<>();

	@Inject
	public MechanismTypeDictionary() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		File typesJsonFile = new File("assets/definitions/types/mechanismTypes.json");
		List<MechanismType> typeList = objectMapper.readValue(FileUtils.readFileToString(typesJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, MechanismType.class));

		for (MechanismType mechanismType : typeList) {
			initialiseTransientFields(mechanismType);
			byName.put(mechanismType.getName(), mechanismType);
		}
	}

	private void initialiseTransientFields(MechanismType mechanismType) {
	}

	public MechanismType getByName(String effectTypeName) {
		return byName.get(effectTypeName);
	}

	public Collection<MechanismType> getAll() {
		return byName.values();
	}
}
