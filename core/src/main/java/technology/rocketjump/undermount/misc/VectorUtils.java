package technology.rocketjump.undermount.misc;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

public class VectorUtils {

	public static GridPoint2 toGridPoint(Vector2 vector) {
		if (vector == null) {
			return null;
		} else {
			return new GridPoint2((int)Math.floor(vector.x), (int)Math.floor(vector.y));
		}
	}

	public static Vector2 toVector(GridPoint2 targetTile) {
		if (targetTile == null) {
			return null;
		} else {
			return new Vector2(targetTile.x + 0.5f, targetTile.y + 0.5f);
		}
	}

	// Wtih thanks to https://www.redblobgames.com/grids/line-drawing.html
	public static List<GridPoint2> getGridpointsBetween(Vector2 p0, Vector2 p1) {
		float dx = p1.x - p0.x, dy = p1.y - p0.y;
		float nx = Math.abs(dx), ny = Math.abs(dy);
		float signX = dx > 0 ? 1 : -1, signY = dy > 0 ? 1 : -1;

		GridPoint2 p = new GridPoint2((int)Math.floor(p0.x), (int)Math.floor(p0.y));
		List<GridPoint2> points = new ArrayList<>();
		points.add(p);

		for (int ix = 0, iy = 0; ix < (int)nx || iy < (int)ny;) {
			float decision = (1 + 2*ix) * ny - (1 + 2*iy) * nx;
			p = p.cpy();
			if (decision == 0) {
				// next step is diagonal
				p.x += signX;
				p.y += signY;
				ix++;
				iy++;
			} else if (decision < 0) {
				// next step is horizontal
				p.x += signX;
				ix++;
			} else {
				// next step is vertical
				p.y += signY;
				iy++;
			}
			points.add(p);
		}

		return points;
	}

}
