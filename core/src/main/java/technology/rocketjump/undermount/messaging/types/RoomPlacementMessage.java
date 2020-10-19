package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rooms.RoomTile;
import technology.rocketjump.undermount.rooms.RoomType;

import java.util.Map;

public class RoomPlacementMessage {

	private final Map<GridPoint2, RoomTile> roomTiles;
	private final RoomType roomType;

	public RoomPlacementMessage(Map<GridPoint2, RoomTile> roomTiles, RoomType roomType) {
		this.roomTiles = roomTiles;
		this.roomType = roomType;
	}

	public Map<GridPoint2, RoomTile> getRoomTiles() {
		return roomTiles;
	}

	public RoomType getRoomType() {
		return roomType;
	}
}
