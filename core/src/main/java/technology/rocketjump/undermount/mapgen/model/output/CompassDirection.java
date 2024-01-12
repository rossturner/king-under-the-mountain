package technology.rocketjump.undermount.mapgen.model.output;

public enum CompassDirection {

	NORTH, EAST, SOUTH, WEST;

	public boolean isOppositeTo(CompassDirection other) {
		if (this == NORTH && other == SOUTH) {
			return true;
		} else if (this == EAST && other == WEST) {
			return true;
		} else if (this == SOUTH && other == NORTH) {
			return true;
		} else if (this == WEST && other == EAST) {
			return true;
		} else {
			return false;
		}
	}
}
