package technology.rocketjump.undermount.mapping.tile.floor;

import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.assets.model.OverlapType;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileNeighbours;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

import java.util.Map;
import java.util.Objects;

import static technology.rocketjump.undermount.mapping.tile.CompassDirection.*;

/**
 * This class is very similar to yet subtly different from WallLayout
 *
 * For a WallLayout, a wall in a corner neighbour without either sibling does not affect the layout
 * whereas for an OverlapLayout, an overlapping corner neighbour without either sibling does affect the layout
 *
 * To demonstrate, given the following layout
 *
 * X..
 * .O.
 * ...
 *
 * Where X is the neighbour that is "filled" in, O is the current tile, and . are empty tiles:
 * If this is a wall layout, the X has no effect - the wall in O is just a single "wall piece" that does not extend to any siblings
 * However for an overlap layout, there will be a small overlap from X into O in the north-west corner
 *
 * Also due to the rendering of wall tiles, they can be flipped vertically but not rotated, whereas the overlap alpha masks may be rotated
 *
 */
public class OverlapLayout {

	public static final char NEIGHBOUR_CHARACTER = 'X';
	public static final char EMPTY_CHARACTER = '.';
	private final int id;

	private static final FloorType baseType = new FloorType("Overlap Layout base floor type", null,
			-1L, GameMaterialType.OTHER, 0, 1, new OverlapType("temp none"), false, null, null);
	private static final FloorType overlapType = new FloorType("Overlap Layout overlapping floor type", null,
			-2L, GameMaterialType.OTHER, 1, 1, new OverlapType("temp organic"), false, null, null);
	private static GameMaterial baseMaterial = new GameMaterial("Unused", -3L, GameMaterialType.OTHER);

	public static OverlapLayout fromNeighbours(TileNeighbours neighbours, FloorType targetType) {
		int layoutId = 0;
		for (Map.Entry<CompassDirection, MapTile> entry : neighbours.entrySet()) {
			if (entry.getValue() != null && entry.getValue().hasFloor()
					&& entry.getValue().getFloor().getFloorType().getFloorTypeId() == targetType.getFloorTypeId()) {
				layoutId |= entry.getKey().getBinaryMask();
			}
		}
		return new OverlapLayout(layoutId);
	}

	public static OverlapLayout fromString(String diagram) {
		diagram = diagram.replaceAll("\n", "");
		TileNeighbours neighbours = new TileNeighbours();

		for (CompassDirection compassDirection : CompassDirection.values()) {
			neighbours.put(compassDirection, new MapTile(0L, compassDirection.getXOffset(), compassDirection.getYOffset(), baseType, baseMaterial));
			if (overlapAtIndex(compassDirection.getIndex(), diagram)) {
				neighbours.get(compassDirection).getFloor().setFloorType(overlapType);
				neighbours.get(compassDirection).getFloor().setMaterial(baseMaterial);
			}
		}

		return fromNeighbours(neighbours, overlapType);
	}

	private static boolean overlapAtIndex(int index, String diagram) {
		return diagram.charAt(index) == NEIGHBOUR_CHARACTER;
	}

	public OverlapLayout(int id) {
		this.id = id;
	}

	public OverlapLayout reduceToMeaningfulForm() {
		char[] characters = this.toString().replaceAll("\n", "").toCharArray();

		ignoreCornerIfEitherSiblingIsSet(characters, NORTH_WEST, NORTH, WEST);
		ignoreCornerIfEitherSiblingIsSet(characters, NORTH_EAST, NORTH, EAST);
		ignoreCornerIfEitherSiblingIsSet(characters, SOUTH_WEST, SOUTH, WEST);
		ignoreCornerIfEitherSiblingIsSet(characters, SOUTH_EAST, SOUTH, EAST);

		return OverlapLayout.fromString(String.valueOf(characters));
	}

	public OverlapLayout flipX() {
		char[] original = this.toString().replaceAll("\n", "").toCharArray();
		char[] flipped = original.clone();

		flipped[NORTH_WEST.getIndex()] = original[NORTH_EAST.getIndex()];
		flipped[NORTH_EAST.getIndex()] = original[NORTH_WEST.getIndex()];

		flipped[WEST.getIndex()] = original[EAST.getIndex()];
		flipped[EAST.getIndex()] = original[WEST.getIndex()];

		flipped[SOUTH_WEST.getIndex()] = original[SOUTH_EAST.getIndex()];
		flipped[SOUTH_EAST.getIndex()] = original[SOUTH_WEST.getIndex()];

		return OverlapLayout.fromString(String.valueOf(flipped));
	}

	public OverlapLayout flipY() {
		char[] original = this.toString().replaceAll("\n", "").toCharArray();
		char[] flipped = original.clone();

		flipped[NORTH_WEST.getIndex()] = original[SOUTH_WEST.getIndex()];
		flipped[NORTH_EAST.getIndex()] = original[SOUTH_EAST.getIndex()];

		flipped[NORTH.getIndex()] = original[SOUTH.getIndex()];
		flipped[SOUTH.getIndex()] = original[NORTH.getIndex()];

		flipped[SOUTH_WEST.getIndex()] = original[NORTH_WEST.getIndex()];
		flipped[SOUTH_EAST.getIndex()] = original[NORTH_EAST.getIndex()];

		return OverlapLayout.fromString(String.valueOf(flipped));
	}

	private void ignoreCornerIfEitherSiblingIsSet(char[] characters, CompassDirection corner,
												  CompassDirection sibling1, CompassDirection sibling2) {
		if (characters[sibling1.getIndex()] == NEIGHBOUR_CHARACTER ||
				characters[sibling2.getIndex()] == NEIGHBOUR_CHARACTER) {
			characters[corner.getIndex()] = EMPTY_CHARACTER;
		}
	}

	public int getId() {
		return id;
	}

	@Override
	public String toString() {
		return topRow() + "\n" + middleRow() + "\n" + bottomRow();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		OverlapLayout layout = (OverlapLayout) o;
		return id == layout.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	private StringBuilder addNeighbourCharacterIf(boolean isNeighbour, StringBuilder builder) {
		if (isNeighbour) {
			builder.append(NEIGHBOUR_CHARACTER);
		} else {
			builder.append(EMPTY_CHARACTER);
		}
		return builder;
	}

	public boolean neighbourInDirection(CompassDirection direction) {
		return (id & direction.getBinaryMask()) > 0;
	}

	public String topRow() {
		StringBuilder row = new StringBuilder();
		addNeighbourCharacterIf(neighbourInDirection(NORTH_WEST), row);
		addNeighbourCharacterIf(neighbourInDirection(NORTH), row);
		addNeighbourCharacterIf(neighbourInDirection(NORTH_EAST), row);
		return row.toString();
	}

	public String middleRow() {
		StringBuilder row = new StringBuilder();
		addNeighbourCharacterIf(neighbourInDirection(WEST), row);
		row.append(EMPTY_CHARACTER); // Always empty in the middle
		addNeighbourCharacterIf(neighbourInDirection(EAST), row);
		return row.toString();
	}

	public String bottomRow() {
		StringBuilder row = new StringBuilder();
		addNeighbourCharacterIf(neighbourInDirection(SOUTH_WEST), row);
		addNeighbourCharacterIf(neighbourInDirection(SOUTH), row);
		addNeighbourCharacterIf(neighbourInDirection(SOUTH_EAST), row);
		return row.toString();
	}

}
