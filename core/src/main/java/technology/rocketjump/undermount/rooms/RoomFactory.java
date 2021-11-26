package technology.rocketjump.undermount.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.tags.TagProcessor;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileNeighbours;
import technology.rocketjump.undermount.rooms.components.RoomComponent;
import technology.rocketjump.undermount.rooms.components.StockpileComponent;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nUpdatable;

import java.util.Iterator;
import java.util.Map;

@Singleton
public class RoomFactory implements GameContextAware, I18nUpdatable {

	private final TagProcessor tagProcessor;
	private final RoomStore roomStore;
	private final I18nTranslator i18nTranslator;
	private GameContext gameContext;

	@Inject
	public RoomFactory(TagProcessor tagProcessor, RoomStore roomStore, I18nTranslator i18nTranslator) {
		this.tagProcessor = tagProcessor;
		this.roomStore = roomStore;
		this.i18nTranslator = i18nTranslator;
	}


	public Room create(RoomType roomType) {
		Room room = new Room(roomType);
		I18nText translatedName = i18nTranslator.getTranslatedString(roomType.getI18nKey());
		tagProcessor.apply(roomType.getProcessedTags(), room);
		if (!roomType.getRoomName().equals("VIRTUAL_PLACING_ROOM")) {
			roomStore.add(room);
		}
		room.setRoomName(generateSequentialName(translatedName.toString(), null, 1));
		return room;
	}

	public Room createBasedOn(Room originalRoom) {
		Room newRoom = create(originalRoom.getRoomType());
		for (RoomComponent originalComponent : originalRoom.getAllComponents()) {
			RoomComponent cloned = originalComponent.clone(newRoom);
			newRoom.addComponent(cloned);
			if (cloned instanceof StockpileComponent) {
				Iterator<StockpileGroup> groupIterator = ((StockpileComponent) cloned).getEnabledGroups().iterator();
				if (groupIterator.hasNext()) {
					String translatedName =  i18nTranslator.getTranslatedString(newRoom.getRoomType().getI18nKey()).toString();
					String groupTranslated = i18nTranslator.getTranslatedString(groupIterator.next().getI18nKey()).toString();
					newRoom.setRoomName(generateSequentialName(translatedName, groupTranslated, 1));
				}
			}
		}
		return newRoom;
	}

	public Room create(RoomType roomType, Map<GridPoint2, RoomTile> roomTilesToPlace) {
		Room room = create(roomType);

		RoomTile firstRoomTile = roomTilesToPlace.values().iterator().next();
		roomTilesToPlace.remove(firstRoomTile.getTilePosition());
		addTilesToRoom(firstRoomTile, roomTilesToPlace, room);
		room.updateLayout(gameContext.getAreaMap());
		return room;
	}

	private void addTilesToRoom(RoomTile currentRoomTile, Map<GridPoint2, RoomTile> remainingRoomTiles, Room newRoom) {
		currentRoomTile.setRoom(newRoom);
		MapTile mapTile = gameContext.getAreaMap().getTile(currentRoomTile.getTilePosition());
		newRoom.addTile(currentRoomTile);
		mapTile.setRoomTile(currentRoomTile);

		TileNeighbours orthogonalNeighbours = gameContext.getAreaMap().getOrthogonalNeighbours(mapTile.getTileX(), mapTile.getTileY());
		for (MapTile neighbourTile : orthogonalNeighbours.values()) {
			if (remainingRoomTiles.containsKey(neighbourTile.getTilePosition())) {
				RoomTile neighbourRoomTile = remainingRoomTiles.remove(neighbourTile.getTilePosition());
				addTilesToRoom(neighbourRoomTile, remainingRoomTiles, newRoom);
			}
		}
	}

	@Override
	public void onLanguageUpdated() {
		for (Room room : roomStore.getAll()) {
			if (!room.isNameChangedByPlayer()) {
				I18nText translatedName = i18nTranslator.getTranslatedString(room.getRoomType().getI18nKey());
				String translatedStockpileGroup = null;
				StockpileComponent stockpileComponent = room.getComponent(StockpileComponent.class);
				if (stockpileComponent != null) {
					if (!stockpileComponent.getEnabledGroups().isEmpty()) {
						StockpileGroup group = stockpileComponent.getEnabledGroups().iterator().next();
						translatedStockpileGroup = i18nTranslator.getTranslatedString(group.getI18nKey()).toString();
					}
				}
				room.setRoomName(generateSequentialName(translatedName.toString(), translatedStockpileGroup, 1));
			}
		}
	}

	public void updateRoomNameForStockpileGroup(Room room, StockpileGroup stockpileGroup) {
		String translatedName =  i18nTranslator.getTranslatedString(room.getRoomType().getI18nKey()).toString();
		String groupTranslated = i18nTranslator.getTranslatedString(stockpileGroup.getI18nKey()).toString();
		room.setRoomName(generateSequentialName(translatedName, groupTranslated, 1));
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
		onLanguageUpdated();
	}

	@Override
	public void clearContextRelatedState() {

	}

	private String generateSequentialName(String translatedRoomType, String translatedStockpileGroup, int counter) {
		String sequentialName;
		if (translatedStockpileGroup != null) {
			sequentialName = translatedStockpileGroup + " " + translatedRoomType.toLowerCase() + " #" + counter;
		} else {
			sequentialName = translatedRoomType + " #" + counter;
		}

		if (roomStore.getByName(sequentialName) == null) {
			return sequentialName;
		} else {
			return generateSequentialName(translatedRoomType, translatedStockpileGroup, counter + 1);
		}
	}

}
