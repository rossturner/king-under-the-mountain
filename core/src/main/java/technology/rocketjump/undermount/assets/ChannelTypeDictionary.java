package technology.rocketjump.undermount.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.model.ChannelType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ChannelTypeDictionary {

	private ObjectMapper objectMapper = new ObjectMapper();

	private Map<String, ChannelType> byName = new ConcurrentHashMap<>();

	@Inject
	public ChannelTypeDictionary() throws IOException {
		this(Gdx.files.internal("assets/definitions/types/channelTypes.json"));
	}

	public ChannelTypeDictionary(FileHandle wallDefinitionsJsonFile) throws IOException {
		List<ChannelType> channelTypes = objectMapper.readValue(wallDefinitionsJsonFile.readString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, ChannelType.class));

		for (ChannelType type : channelTypes) {
			byName.put(type.getChannelTypeName(), type);
		}
	}

	public ChannelType getByName(String name) {
		// FIXME #87 Default placeholder assets - return default when not found by name
		return byName.get(name);
	}

	public Iterable<ChannelType> getAll() {
		return byName.values();
	}
}
