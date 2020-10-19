package technology.rocketjump.undermount.sprites.model;

import static technology.rocketjump.undermount.sprites.model.BridgeOrientation.EAST_WEST;

public enum BridgeTileLayout {

	CENTRE		(true, true, true, true),
	NORTH		(false, true, true, true),
	NORTH_EAST	(false, true, true, false),
	EAST		(true, true, true, false),
	SOUTH_EAST	(true, false, true, false),
	SOUTH		(true, false, true, true),
	SOUTH_WEST	(true, false, false, true),
	WEST		(true, true, false, true),
	NORTH_WEST	(false, true, false, true);

	private final boolean northNeighbour;
	private final boolean southNeighbour;
	private final boolean westNeighbour;
	private final boolean eastNeighbour;

	BridgeTileLayout(boolean northNeighbour, boolean southNeighbour, boolean westNeighbour, boolean eastNeighbour) {
		this.northNeighbour = northNeighbour;
		this.southNeighbour = southNeighbour;
		this.westNeighbour = westNeighbour;
		this.eastNeighbour = eastNeighbour;
	}

	public static BridgeTileLayout byNeighbours(boolean northNeighbour, boolean southNeighbour, boolean westNeighbour, boolean eastNeighbour) {
		for (BridgeTileLayout layout : values()) {
			if (layout.northNeighbour == northNeighbour && layout.southNeighbour == southNeighbour && layout.westNeighbour == westNeighbour && layout.eastNeighbour == eastNeighbour) {
				return layout;
			}
		}
		return CENTRE;
	}

	public boolean isNavigable(BridgeOrientation bridgeOrientation) {
		if (CENTRE.equals(this)) {
			return true;
		} else if (bridgeOrientation.equals(EAST_WEST)) {
			return WEST.equals(this) || EAST.equals(this);
		} else {
			// north/south bridge
			return NORTH.equals(this) || SOUTH.equals(this);
		}
	}
}
