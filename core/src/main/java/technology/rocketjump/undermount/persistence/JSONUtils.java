package technology.rocketjump.undermount.persistence;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;

public class JSONUtils {

	public static JSONObject toJSON(Vector2 vector2) {
		if (vector2 == null) {
			return null;
		}
		JSONObject json = new JSONObject(true);
		json.put("x", vector2.x);
		json.put("y", vector2.y);
		return json;
	}

	public static JSONObject toJSON(GridPoint2 gridPoint2) {
		if (gridPoint2 == null) {
			return null;
		}
		JSONObject json = new JSONObject(true);
		json.put("x", gridPoint2.x);
		json.put("y", gridPoint2.y);
		return json;
	}

	public static GridPoint2 gridPoint2(JSONObject gridPointAsJson) {
		if (gridPointAsJson == null) {
			return null;
		} else {
			return new GridPoint2(gridPointAsJson.getIntValue("x"), gridPointAsJson.getIntValue("y"));
		}
	}

	public static Vector2 vector2(JSONObject vectorAsJson) {
		if (vectorAsJson == null) {
			return null;
		} else {
			return new Vector2(vectorAsJson.getFloatValue("x"), vectorAsJson.getFloatValue("y"));
		}
	}
}
