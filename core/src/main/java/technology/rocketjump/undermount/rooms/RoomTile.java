package technology.rocketjump.undermount.rooms;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.layout.RoomTileLayout;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.Objects;

public class RoomTile implements ChildPersistable {

	private RoomTileLayout layout;
	private Room room;
	private GridPoint2 tilePosition;
	private MapTile tile;

	public RoomTileLayout getLayout() {
		return layout;
	}

	public void setLayout(RoomTileLayout layout) {
		this.layout = layout;
	}

	public Room getRoom() {
		return room;
	}

	public void setRoom(Room room) {
		this.room = room;
	}

	public GridPoint2 getTilePosition() {
		return tilePosition;
	}

	public void setTilePosition(GridPoint2 tilePosition) {
		this.tilePosition = tilePosition;
	}

	public boolean isAtRoomEdge() {
		return layout != null && layout.getId() != 255; // 255 is the layout where every surrounding tile is of the same type
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RoomTile roomTile = (RoomTile) o;
		return Objects.equals(tilePosition, roomTile.tilePosition);
	}

	@Override
	public int hashCode() {
		return Objects.hash(tilePosition);
	}

	public MapTile getTile() {
		return tile;
	}

	public void setTile(MapTile tile) {
		this.tile = tile;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("layout", layout.getId());
		asJson.put("position", JSONUtils.toJSON(tilePosition));
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.layout = new RoomTileLayout(asJson.getIntValue("layout"));
		// Room set by parent room
		this.tilePosition = JSONUtils.gridPoint2(asJson.getJSONObject("position"));
		// MapTile is set by loading of MapTile
	}
}
