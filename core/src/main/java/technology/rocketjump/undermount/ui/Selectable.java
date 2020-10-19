package technology.rocketjump.undermount.ui;

import technology.rocketjump.undermount.doors.Doorway;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.rooms.Bridge;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.rooms.constructions.Construction;

import java.util.Objects;

/**
 * This holds and represents something which can be clicked on and selected by the UI in the game world
 */
public class Selectable implements Comparable<Selectable> {

	public final SelectableType type;
	private Entity entity;

	private Construction construction;
	private Room room;
	private Bridge bridge;
	private MapTile tile;
	private Doorway doorway;
	private int distanceFromCursor;

	public Selectable(Entity entity, float distanceFromCursor) {
		this.type = SelectableType.ENTITY;
		this.entity = entity;
		this.distanceFromCursor = (int)(distanceFromCursor * 1000f);
	}

	public Selectable(Construction construction) {
		this.type = SelectableType.CONSTRUCTION;
		this.construction = construction;
	}

	public Selectable(Room room) {
		this.type = SelectableType.ROOM;
		this.room = room;
	}

	public Selectable(Bridge bridge) {
		this.type = SelectableType.BRIDGE;
		this.bridge = bridge;
	}

	public Selectable(Doorway doorway) {
		this.type = SelectableType.DOORWAY;
		this.doorway = doorway;
	}

	public Selectable(MapTile tile) {
		this.type = SelectableType.TILE;
		this.tile = tile;
	}

	@Override
	public int compareTo(Selectable other) {
		return other.getSortValue() - this.getSortValue();
	}

	public boolean equals(Selectable other) {
		if (this.type.equals(other.type)) {
			// Types are the same so compare on IDs
			return this.getId() == other.getId();
		} else {
			// Types are different so definitely not the same
			return false;
		}
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other == null || getClass() != other.getClass()) return false;
		return equals((Selectable)other);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId());
	}

	public long getId() {
		switch (this.type) {
			case ENTITY:
				return this.entity.getId();
			case ROOM:
				return this.room.getRoomId();
			case BRIDGE:
				return this.bridge.getBridgeId();
			case TILE:
				return (this.tile.getTileX() * 10000) + this.tile.getTileY();
			case DOORWAY:
				return this.doorway.getDoorEntity().getId();
			case CONSTRUCTION:
				return this.construction.getId();
			default:
				return 0;
		}
	}

	public Entity getEntity() {
		return entity;
	}

	public Construction getConstruction() {
		return construction;
	}

	public Room getRoom() {
		return room;
	}

	public Bridge getBridge() {
		return bridge;
	}

	public MapTile getTile() {
		return tile;
	}

	public Doorway getDoorway() {
		return doorway;
	}

	private int getSortValue() {
		int sort = type.sortOrder;
		if (this.type == SelectableType.ENTITY) {
			sort -= distanceFromCursor;
		}
		return sort;
	}

	public enum SelectableType {

		ENTITY(5000),
		CONSTRUCTION(4000),
		DOORWAY(3000),
		BRIDGE(2001),
		ROOM(2000),
		TILE(1000);

		private final int sortOrder;

		SelectableType(int sortOrder) {
			this.sortOrder = sortOrder;
		}
	}

}
