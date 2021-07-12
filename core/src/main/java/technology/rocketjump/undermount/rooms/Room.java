package technology.rocketjump.undermount.rooms;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileNeighbours;
import technology.rocketjump.undermount.mapping.tile.layout.RoomTileLayout;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.rooms.components.RoomComponent;
import technology.rocketjump.undermount.rooms.components.RoomComponentMap;
import technology.rocketjump.undermount.rooms.components.behaviour.RoomBehaviourComponent;

import java.util.*;

public class Room implements Persistable {

	private long roomId;
	private String roomName;
	private boolean nameChangedByPlayer;
	private RoomType roomType;
	private Color borderColor = null;
	private final Map<GridPoint2, RoomTile> roomTiles = new HashMap<>();
	private final Vector2 avgWorldPosition = new Vector2();
	private final RoomComponentMap componentMap = new RoomComponentMap();
	private boolean isFullyEnclosed; // by walls, doors and covered with roof

	public Room() {

	}

	Room(RoomType roomType) {
		roomId = SequentialIdGenerator.nextId();
		this.roomType = roomType;
	}

	public long getRoomId() {
		return roomId;
	}

	public RoomType getRoomType() {
		return roomType;
	}

	public Map<GridPoint2, RoomTile> getRoomTiles() {
		return roomTiles;
	}

	public void addTile(RoomTile tile) {
		roomTiles.put(tile.getTilePosition(), tile);
		recalculatePosition();
	}

	public RoomTile removeTile(GridPoint2 position) {
		RoomTile removed = roomTiles.remove(position);
		recalculatePosition();
		for (RoomComponent roomComponent : componentMap.getAll()) {
			roomComponent.tileRemoved(position);
		}
		return removed;
	}

	public Set<GridPoint2> keySet() {
		return roomTiles.keySet();
	}

	public Set<Map.Entry<GridPoint2, RoomTile>> entrySet() {
		return roomTiles.entrySet();
	}

	public Vector2 getAvgWorldPosition() {
		return avgWorldPosition;
	}

	public <T extends RoomComponent> T getComponent(Class<T> classType) {
		return componentMap.get(classType);
	}

	public <T extends RoomComponent> T createComponent(Class<T> classType, MessageDispatcher messageDispatcher) {
		try {
			T component = classType.getDeclaredConstructor(Room.class, MessageDispatcher.class).newInstance(this, messageDispatcher);
			componentMap.add(component);
			return component;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public <T extends RoomComponent> T getOrCreateComponent(Class<T> classType, MessageDispatcher messageDispatcher) {
		T component = componentMap.get(classType);
		if (component == null) {
			try {
				component = classType.getDeclaredConstructor(Room.class, MessageDispatcher.class).newInstance(this, messageDispatcher);
				componentMap.add(component);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}
		return component;
	}

	public void removeComponent(Class<? extends RoomComponent> componentClass) {
		componentMap.remove(componentClass);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Room room = (Room) o;
		return roomId == room.roomId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(roomId);
	}

	public boolean isEmpty() {
		return roomTiles.isEmpty();
	}

	public void clearTiles() {
		for (RoomTile roomTile : roomTiles.values()) {
			roomTile.getTile().setRoomTile(null);
		}
		roomTiles.clear();
	}

	public void updateLayout(TiledMap tiledMap) {
		for (Map.Entry<GridPoint2, RoomTile> entry : roomTiles.entrySet()) {
			TileNeighbours neighbours = tiledMap.getNeighbours(entry.getKey().x, entry.getKey().y);
			RoomTileLayout newLayout = new RoomTileLayout(neighbours, this);
			entry.getValue().setLayout(newLayout);
		}

		checkIfEnclosed(tiledMap);
	}

	public void checkIfEnclosed(TiledMap tiledMap) {
		this.isFullyEnclosed = true;

		for (GridPoint2 tileLocation : roomTiles.keySet()) {
			MapTile tile = tiledMap.getTile(tileLocation);
			if (tile.getRoof().getState().equals(TileRoofState.OPEN)) {
				isFullyEnclosed = false;
				break;
			}

			for (CompassDirection direction : CompassDirection.CARDINAL_DIRECTIONS) {
				GridPoint2 neighbourLocation = tileLocation.cpy().add(direction.getXOffset(), direction.getYOffset());
				if (!roomTiles.containsKey(neighbourLocation)) {
					MapTile tileNeighbour = tiledMap.getTile(tileLocation.x + direction.getXOffset(), tileLocation.y + direction.getYOffset());
					if (tileNeighbour != null && !hasWallOrDoor(tileNeighbour)) {
						isFullyEnclosed = false;
						break;
					}
				}
			}
			if (!isFullyEnclosed) {
				break;
			}
		}
	}

	private boolean hasWallOrDoor(MapTile tile) {
		return tile.hasWall() || tile.hasDoorway();
	}


	public void mergeFrom(Room otherRoom) {
		for (RoomTile otherRoomTile : otherRoom.roomTiles.values()) {
			otherRoomTile.setRoom(this);
			this.addTile(otherRoomTile);
		}
		for (RoomComponent otherComponent : otherRoom.componentMap.getAll()) {
			RoomComponent thisMatchingComponent = this.componentMap.get(otherComponent.getClass());
			if (thisMatchingComponent != null) {
				thisMatchingComponent.mergeFrom(otherComponent);
			}
		}
	}

	private void recalculatePosition() {
		// This could be done a bit neater as a moving average rather than recalculating every time
		avgWorldPosition.set(0, 0);
		for (GridPoint2 tilePosition : roomTiles.keySet()) {
			avgWorldPosition.add(tilePosition.x + 0.5f, tilePosition.y + 0.5f);
		}
		avgWorldPosition.scl(1f / roomTiles.keySet().size());
	}

	public RoomBehaviourComponent getBehaviourComponent() {
		return componentMap.getBehaviourComponent();
	}

	public Collection<RoomComponent> getAllComponents() {
		return componentMap.getAll();
	}

	public void addComponent(RoomComponent roomComponent) {
		componentMap.add(roomComponent);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Room{");
		sb.append(roomId);
		sb.append(", type=").append(roomType);
		sb.append(", tiles=").append(roomTiles.size());
		sb.append(", enclosed=").append(isFullyEnclosed);
		sb.append('}');
		return sb.toString();
	}

	public String getRoomName() {
		return roomName;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	public boolean isNameChangedByPlayer() {
		return nameChangedByPlayer;
	}

	public void setNameChangedByPlayer(boolean nameChangedByPlayer) {
		this.nameChangedByPlayer = nameChangedByPlayer;
	}

	public Color getBorderColor() {
		if (borderColor == null) {
			return roomType.getColor();
		} else {
			return borderColor;
		}
	}

	public void setBorderColor(Color borderColor) {
		this.borderColor = borderColor;
	}

	public boolean isFullyEnclosed() {
		return isFullyEnclosed;
	}

	public void setFullyEnclosed(boolean fullyEnclosed) {
		isFullyEnclosed = fullyEnclosed;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.rooms.containsKey(this.roomId)) {
			return;
		}
		JSONObject asJson = new JSONObject(true);

		asJson.put("id", roomId);
		asJson.put("name", roomName);
		asJson.put("type", roomType.getRoomName());
		if (nameChangedByPlayer) {
			asJson.put("nameChangedByPlayer", true);
		}

		JSONArray tilesJson = new JSONArray();
		for (RoomTile roomTile : this.roomTiles.values()) {
			JSONObject tileAsJson = new JSONObject(true);
			roomTile.writeTo(tileAsJson, savedGameStateHolder);
			tilesJson.add(tileAsJson);
		}
		asJson.put("tiles", tilesJson);

		if (!componentMap.getAll().isEmpty()) {
			JSONArray componentsJson = new JSONArray();
			for (RoomComponent roomComponent : componentMap.getAll()) {
				JSONObject componentJson = new JSONObject(true);
				roomComponent.writeTo(componentJson, savedGameStateHolder);
				componentJson.put("_class", roomComponent.getClass().getSimpleName());
				componentsJson.add(componentJson);
			}
			asJson.put("components", componentsJson);
		}

		if (borderColor != null) {
			asJson.put("borderColor", HexColors.toHexString(borderColor));
		}

		if (isFullyEnclosed) {
			asJson.put("enclosed", true);
		}

		savedGameStateHolder.rooms.put(roomId, this);
		savedGameStateHolder.roomsJson.add(asJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.roomId = asJson.getLongValue("id");
		this.roomName = asJson.getString("name");
		this.roomType = relatedStores.roomTypeDictionary.getByName(asJson.getString("type"));
		if (roomType == null) {
			throw new InvalidSaveException("Could not find room type by name " + asJson.getString("type"));
		}
		this.nameChangedByPlayer = asJson.getBooleanValue("nameChangedByPlayer");

		JSONArray tilesArray = asJson.getJSONArray("tiles");
		for (int cursor = 0; cursor < tilesArray.size(); cursor++) {
			RoomTile roomTile = new RoomTile();
			roomTile.readFrom(tilesArray.getJSONObject(cursor), savedGameStateHolder, relatedStores);

			MapTile mapTile = savedGameStateHolder.tiles.get(roomTile.getTilePosition());
			if (mapTile == null) {
				throw new InvalidSaveException("Could not find map tile at location " + roomTile.getTilePosition());
			}
			roomTile = mapTile.getRoomTile(); // Map has the real instance of the RoomTile
			roomTile.setRoom(this);
			roomTiles.put(roomTile.getTilePosition(), roomTile);
		}

		JSONArray componentsArray = asJson.getJSONArray("components");
		if (componentsArray != null) {
			for (int cursor = 0; cursor < componentsArray.size(); cursor++) {
				JSONObject componentJson = componentsArray.getJSONObject(cursor);
				Class<? extends RoomComponent> componentClass = relatedStores.roomComponentDictionary.getByName(componentJson.getString("_class"));
				RoomComponent component = this.createComponent(componentClass, relatedStores.messageDispatcher);
				component.readFrom(componentJson, savedGameStateHolder, relatedStores);
			}
		}

		this.borderColor = HexColors.get(asJson.getString("borderColor"));
		this.isFullyEnclosed = asJson.getBooleanValue("enclosed");

		recalculatePosition();

		savedGameStateHolder.rooms.put(roomId, this);
	}
}
