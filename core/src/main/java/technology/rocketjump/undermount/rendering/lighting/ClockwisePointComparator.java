package technology.rocketjump.undermount.rendering.lighting;

import com.badlogic.gdx.math.Vector2;

import java.util.Comparator;

/**
 * This class attributed to http://stackoverflow.com/questions/6989100/sort-points-in-clockwise-order
 *
 * Assumes all points are already around the origin
 */
public class ClockwisePointComparator implements Comparator<Vector2> {

	@Override
	public int compare(Vector2 a, Vector2 b) {
		if (a.x >= 0 && b.x < 0)
			return -1;
		if (a.x < 0 && b.x >= 0)
			return 1;
		if (a.x == 0 && b.x  == 0) {
			if (a.y >= 0 || b.y  >= 0) {
				if (a.y > b.y) {
					return -1;
				} else {
					return 1;
				}
			} else {
				if (b.y > a.y) {
					return -1;
				} else {
					return 1;
				}
			}
		}

		// compute the cross product of vectors (center -> a) x (center -> b)
		float det = a.x * b.y - b.x * a.y;
		if (det < 0) {
			return -1;
		} else if (det > 0) {
			return 1;
		} else {
			// points a and b are on the same line from the center
			// check which point is closer to the center
			float d1 = a.x * a.x + a.y * a.y;
			float d2 = b.x * b.x + b.y * b.y;
			if (d1 > d2) {
				return 1;
			} else  if (d1 < d2) {
				return -1;
			} else {
				return 0;
			}
		}
	}

}
