package technology.rocketjump.undermount.mapgen.model.output;

import com.badlogic.gdx.math.GridPoint2;

/**
 * This class represents part of the edge of a map (not the full edge)
 */
public class MapEdgeSection {

	public final CompassDirection direction;
	public final GridPoint2 minimum, maximum;

	public MapEdgeSection(CompassDirection direction, GridPoint2 initialPoint) {
		this.direction = direction;
		minimum = initialPoint.cpy();
		maximum = initialPoint.cpy();
	}

	public MapEdgeSection(MapEdgeSection other) {
		direction = other.direction;
		minimum = other.minimum.cpy();
		maximum = other.maximum.cpy();
	}

	public void addAdjacentPoint(GridPoint2 adjacentPoint) {
		minimum.x = Math.min(minimum.x, adjacentPoint.x);
		minimum.y = Math.min(minimum.y, adjacentPoint.y);

		maximum.x = Math.max(maximum.x, adjacentPoint.x);
		maximum.y = Math.max(maximum.y, adjacentPoint.y);
	}

	public boolean isAdjacent(GridPoint2 adjacentPoint) {
		if (Math.abs(adjacentPoint.x - minimum.x) <= 1 && Math.abs(adjacentPoint.y - minimum.y) <= 1) {
			return true;
		} else if (Math.abs(adjacentPoint.x - maximum.x) <= 1 && Math.abs(adjacentPoint.y - maximum.y) <= 1) {
			return true;
		} else {
			return false;
		}
	}

}
