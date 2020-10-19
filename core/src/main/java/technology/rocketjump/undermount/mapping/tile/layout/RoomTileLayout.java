package technology.rocketjump.undermount.mapping.tile.layout;

import technology.rocketjump.undermount.mapping.tile.TileNeighbours;
import technology.rocketjump.undermount.rooms.Room;

public class RoomTileLayout extends TileLayout {

	public RoomTileLayout(TileNeighbours neighbours, Room targetRoom) {
		super(neighbours, (tile, direction) -> tile.hasRoom() && tile.getRoomTile().getRoom().getRoomId() == targetRoom.getRoomId());
	}

	public RoomTileLayout(int id) {
		super(id);
	}
}
