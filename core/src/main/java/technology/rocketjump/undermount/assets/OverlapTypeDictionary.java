package technology.rocketjump.undermount.assets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.undermount.assets.model.OverlapType;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class OverlapTypeDictionary {

	private final Map<String, OverlapType> byName = new HashMap<>();

	@Inject
	public OverlapTypeDictionary(ObjectMapper objectMapper) throws IOException {
		File overlapTypesFile = new File("assets/definitions/types/overlapTypes.json");
		List<OverlapType> overlapTypes = objectMapper.readValue(FileUtils.readFileToString(overlapTypesFile),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, OverlapType.class));

		for (OverlapType overlapType : overlapTypes) {
			byName.put(overlapType.getOverlapName(), overlapType);
		}
	}

	public OverlapType getByName(String name) {
		return byName.get(name);
	}

	public Collection<OverlapType> getAll() {
		return byName.values();
	}
}
