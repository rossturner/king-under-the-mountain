package technology.rocketjump.undermount.rooms;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.rendering.utils.HexColors;

import java.io.IOException;
import java.util.*;

@Singleton
public class RoomTypeDictionary {

	private Map<String, RoomType> byName = new HashMap<>();
	Map<String, RoomType> byTranslatedName = new TreeMap<>();

	public final static RoomType VIRTUAL_PLACING_ROOM;
	static {
		VIRTUAL_PLACING_ROOM = new RoomType();
		VIRTUAL_PLACING_ROOM.setRoomName("VIRTUAL_PLACING_ROOM");
		VIRTUAL_PLACING_ROOM.setColor(HexColors.get("#ffff66AA"));
		VIRTUAL_PLACING_ROOM.setI18nKey("ROOMS.VIRTUAL_ROOM");
		VIRTUAL_PLACING_ROOM.setIconName("help");
	}

	@Inject
	public RoomTypeDictionary(FurnitureTypeDictionary furnitureTypeDictionary) throws IOException {
		this(Gdx.files.internal("assets/definitions/types/roomTypes.json"), furnitureTypeDictionary);
	}

	private RoomTypeDictionary(FileHandle jsonFile, FurnitureTypeDictionary furnitureTypeDictionary) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		List<RoomType> roomTypes = objectMapper.readValue(jsonFile.readString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, RoomType.class));

		for (RoomType roomType : roomTypes) {
			byName.put(roomType.getRoomName(), roomType);
			for (String furnitureName : roomType.getFurnitureNames()) {
				FurnitureType applicableFurnitureType = furnitureTypeDictionary.getByName(furnitureName);
				if (applicableFurnitureType == null) {
					Logger.error("Could not find furniture type by name " + furnitureName + " for room " + roomType.getRoomName());
				} else {
					applicableFurnitureType.getValidRoomTypes().add(roomType);
				}
			}
		}

	}

	public RoomType getByName(String name) {
		return byName.get(name);
	}

	public Collection<RoomType> getAll() {
		return byName.values();
	}

}
