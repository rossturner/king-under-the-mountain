package technology.rocketjump.undermount.entities.model.physical.humanoid.body.organs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class OrganDefinitionDictionary {

	private Map<String, OrganDefinition> byName = new HashMap<>();

	@Inject
	public OrganDefinitionDictionary() throws IOException {
		File jsonFile = new File("assets/definitions/organs.json");
		ObjectMapper objectMapper = new ObjectMapper();
		List<OrganDefinition> organDefinitions = objectMapper.readValue(FileUtils.readFileToString(jsonFile),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, OrganDefinition.class));

		for (OrganDefinition organDefinition : organDefinitions) {
			byName.put(organDefinition.getName(), organDefinition);
		}
	}

	public OrganDefinition getByName(String organDefinitionName) {
		return byName.get(organDefinitionName);
	}

}
