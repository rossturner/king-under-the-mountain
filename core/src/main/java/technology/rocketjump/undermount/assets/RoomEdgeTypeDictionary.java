package technology.rocketjump.undermount.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.model.RoomEdgeType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class RoomEdgeTypeDictionary {

	private ObjectMapper objectMapper = new ObjectMapper();

	private ConcurrentHashMap<Long, RoomEdgeType> byId = new ConcurrentHashMap<>();
	private Map<String, RoomEdgeType> byName = new ConcurrentHashMap<>();

	@Inject
	public RoomEdgeTypeDictionary() throws IOException {
		this(Gdx.files.internal("assets/definitions/room-edges.json"));
	}

	public RoomEdgeTypeDictionary(FileHandle roomEdgeDefinitionsJsonFile) throws IOException {
		List<RoomEdgeType> roomEdgeTypes = objectMapper.readValue(roomEdgeDefinitionsJsonFile.readString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, RoomEdgeType.class));

		for (RoomEdgeType roomEdgeType : roomEdgeTypes) {
			if (byId.containsKey(roomEdgeType.getRoomEdgeTypeId())) {
				throw new IOException("Duplicate ID for room edge: " + roomEdgeType);
			}
			byId.put(roomEdgeType.getRoomEdgeTypeId(), roomEdgeType);
			byName.put(roomEdgeType.getRoomEdgeTypeName(), roomEdgeType);
		}
	}

	public RoomEdgeType getById(long id) {
		// FIXME #87 Default placeholder assets - return default when not found by ID
		return byId.get(id);
	}

	public RoomEdgeType getByName(String name) {
		// FIXME #87 Default placeholder assets - return default when not found by name
		return byName.get(name);
	}

	public Iterable<RoomEdgeType> getAllDefinitions() {
		return byId.values();
	}
}
