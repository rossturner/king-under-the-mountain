package technology.rocketjump.undermount.mapping.tile.wall;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;

import static technology.rocketjump.undermount.mapping.tile.CompassDirection.*;

/**
 * This class currently only supports orthogonal edges
 */
public class Edge {

	public static final float EPSILON = 0.0001f;
	private final Vector2 pointA;
	private final Vector2 pointB;

	public Edge(Vector2 pointA, Vector2 pointB) {
		this.pointA = pointA;
		this.pointB = pointB;
	}

	public Vector2 getPointA() {
		return pointA;
	}

	public Vector2 getPointB() {
		return pointB;
	}

	/**
	 * Assumes these edges are always orthogonal
	 */
	public CompassDirection getDirection() {
		if (pointA.x == pointB.x) {
			// edge is along Y axis
			if (pointA.y < pointB.y) {
				return NORTH;
			} else {
				return SOUTH;
			}
		} else {
			// edge is along X axis
			if (pointA.x < pointB.x) {
				return EAST;
			} else {
				return WEST;
			}
		}
	}

	public float averageEndpointDistanceSquared() {
		float pointADistanceSquared = (pointA.x * pointA.x) + (pointA.y * pointA.y);
		float pointBDistanceSquared = (pointB.x * pointB.x) + (pointB.y * pointB.y);
		return (pointADistanceSquared + pointBDistanceSquared) / 2.0f;
	}

	@Override
	public String toString() {
		return pointA.toString() + " -> " + pointB.toString();
	}

	/**
	 * This method sets A and B so that A comes before B with relation to (0, 0) ordered clockwise
	 *
	 * Also assumes edges are orthogonal
	 */
	public Edge reorderPointsClockwiseAroundOrigin() {
		if (Math.abs(pointA.x - pointB.x) < EPSILON) {
			if (pointA.x > 0) {
				// pointA.y should be > pointB.y
				if (pointA.y > pointB.y) {
					return this;
				} else {
					return new Edge(pointB, pointA);
				}
			} else {
				// pointA.y should be < pointB.y
				if (pointA.y < pointB.y) {
					return this;
				} else {
					return new Edge(pointB, pointA);
				}
			}
		} else {
			// points have identical Y components
			if (pointA.y > 0) {
				// pointA.x should be < pointB.x
				if (pointA.x < pointB.x) {
					return this;
				} else {
					return new Edge(pointB, pointA);
				}
			} else {
				// pointA.x should be > pointB.x
				if (pointA.x > pointB.x) {
					return this;
				} else {
					return new Edge(pointB, pointA);
				}
			}
		}
	}
}
