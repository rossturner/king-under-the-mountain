package technology.rocketjump.undermount.assets;

import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.ProvidedBy;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.guice.FloorDictionaryProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ProvidedBy(FloorDictionaryProvider.class)
@Singleton
public class FloorTypeDictionary {

	private Map<Long, FloorType> floorTypeIdMap = new ConcurrentHashMap<>();
	private Map<String, FloorType> floorTypeNameMap = new ConcurrentHashMap<>();
	private ObjectMapper objectMapper = new ObjectMapper();

	public FloorTypeDictionary(FileHandle floorDefinitionsJsonFile, OverlapTypeDictionary overlapTypeDictionary) throws IOException {
		List<FloorType> floorTypes = objectMapper.readValue(floorDefinitionsJsonFile.readString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, FloorType.class));

		for (FloorType floorType : floorTypes) {
			if (floorTypeIdMap.containsKey(floorType.getFloorTypeId())) {
				throw new IOException("Duplicate floor for ID: " + floorType.toString());
			}
			floorTypeIdMap.put(floorType.getFloorTypeId(), floorType);
			floorTypeNameMap.put(floorType.getFloorTypeName().toLowerCase(), floorType);

			if (overlapTypeDictionary.getByName(floorType.getOverlapType().getOverlapName()) == null) {
				Logger.error("Unrecognised overlap name in floor type " + floorType.getFloorTypeName());
			}
		}
	}

	public FloorType getByFloorTypeId(long materialId) {
		// FIXME #87 Default placeholder assets - return default when not found by ID
		return floorTypeIdMap.get(materialId);
	}

	public FloorType getByFloorTypeName(String name) {
		// FIXME #87 Default placeholder assets - return default when not found by name
		return floorTypeNameMap.get(name.toLowerCase());
	}

	public Collection<FloorType> getAllDefinitions() {
		return floorTypeNameMap.values();
	}
}
