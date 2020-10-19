package technology.rocketjump.undermount.mapping.tile.wall;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;

import static technology.rocketjump.undermount.mapping.tile.CompassDirection.*;

public class WallEdgeDefinition {

	public static final float INNER_EDGE_X1 = 0.3125f;
	public static final float INNER_EDGE_X2 = 0.6875f;
	public static final float INNER_EDGE_Y1 = 0.59375f;
	public static final float INNER_EDGE_Y2 = 0.96875f;

	private final Array<Edge> inner;
	private final Array<Edge> outer;

	public WallEdgeDefinition(Array<Edge> inner, Array<Edge> outer) {
		this.inner = inner;
		this.outer = outer;
	}

	public WallEdgeDefinition flipX() {
		Array<Edge> flippedInner = new Array<>(inner.size);
		for (Edge originalInnerEdge : inner) {
			Vector2 flippedPointA = new Vector2(flipXPoint(originalInnerEdge.getPointA().x), originalInnerEdge.getPointA().y);
			Vector2 flippedPointB = new Vector2(flipXPoint(originalInnerEdge.getPointB().x), originalInnerEdge.getPointB().y);
			flippedInner.add(new Edge(flippedPointA, flippedPointB));
		}

		Array<Edge> flippedOuter = new Array<>(outer.size);
		for (Edge originalOuterEdge : outer) {
			Vector2 flippedPointA = new Vector2(flipXPoint(originalOuterEdge.getPointA().x), originalOuterEdge.getPointA().y);
			Vector2 flippedPointB = new Vector2(flipXPoint(originalOuterEdge.getPointB().x), originalOuterEdge.getPointB().y);
			flippedOuter.add(new Edge(flippedPointA, flippedPointB));
		}

		return new WallEdgeDefinition(flippedInner, flippedOuter);
	}

	/**
	 * This method returns the set of inner edges which face toward the source point
	 * plus the set of outer edges which face away from the source point
	 * <p>
	 * If the inner edges are the "tops" of walls and outer edges are the "outside"
	 * of walls, this lets us only consider the meaningful edges to cast light/visibility
	 * as in the answer to http://gamedev.stackexchange.com/questions/115125/where-to-cast-light-shadows-in-a-2-5d-view
	 * <p>
	 * Going to assume that the wall edges are always orthogonal (up/down, left/right) to simplify things for now
	 * and that winding is clockwise so a NORTH-directed wall is facing to the west, and a SOUTH-directed wall is facing to the east
	 */
	public Array<Edge> getEdgesForVisibilityPolygon(Vector2 sourcePoint) {
		Array<Edge> results = new Array<>(inner.size + outer.size);

		// Should always be able to use pointA as either x or y should be the same as pointB for each direction it is used in
		// e.g. North & South aX == bX, East & West aY == bY

		// Only those facing towards the sourcePoint
		for (Edge innerEdge : inner) {
			CompassDirection wallDirection = innerEdge.getDirection();
			if (wallDirection.equals(NORTH)) {
				if (innerEdge.getPointA().x > sourcePoint.x) {
					results.add(innerEdge);
				}
			} else if (wallDirection.equals(SOUTH)) {
				if (innerEdge.getPointA().x < sourcePoint.x) {
					results.add(innerEdge);
				}
			} else if (wallDirection.equals(EAST)) {
				if (innerEdge.getPointA().y < sourcePoint.y) {
					results.add(innerEdge);
				}
			} else if (wallDirection.equals(WEST)) {
				if (innerEdge.getPointA().y > sourcePoint.y) {
					results.add(innerEdge);
				}
			}
		}

		// Only those facing away from the source point
		for (Edge outerEdge : outer) {
			CompassDirection wallDirection = outerEdge.getDirection();
			if (wallDirection.equals(NORTH)) {
				if (outerEdge.getPointA().x < sourcePoint.x) {
					results.add(outerEdge);
				}
			} else if (wallDirection.equals(SOUTH)) {
				if (outerEdge.getPointA().x > sourcePoint.x) {
					results.add(outerEdge);
				}
			} else if (wallDirection.equals(EAST)) {
				if (outerEdge.getPointA().y > sourcePoint.y) {
					results.add(outerEdge);
				}
			} else if (wallDirection.equals(WEST)) {
				if (outerEdge.getPointA().y < sourcePoint.y) {
					results.add(outerEdge);
				}
			}
		}

		return results;
	}

	public Array<Edge> getInnerEdges() {
		return inner;
	}

	public Array<Edge> getOuterEdges() {
		return outer;
	}

	private float flipXPoint(float x) {
		if (x == 1.0f) {
			return 0.0f;
		} else if (x == 0.0f) {
			return 1.0f;
		} else if (x == INNER_EDGE_X1) {
			return INNER_EDGE_X2;
		} else if (x == INNER_EDGE_X2) {
			return INNER_EDGE_X1;
		} else {
			throw new IllegalArgumentException("Unrecognised x co-ordinate to flip for WallEdgeDefinition: " + x);
		}
	}
}
