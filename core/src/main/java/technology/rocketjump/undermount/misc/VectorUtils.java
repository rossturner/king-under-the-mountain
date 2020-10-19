package technology.rocketjump.undermount.misc;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;

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

}
