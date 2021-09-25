package technology.rocketjump.undermount.mapping.tile.wall;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.google.inject.ProvidedBy;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.undermount.doors.Doorway;
import technology.rocketjump.undermount.doors.DoorwayOrientation;
import technology.rocketjump.undermount.doors.DoorwaySize;
import technology.rocketjump.undermount.guice.WallEdgeAtlasProvider;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static technology.rocketjump.undermount.mapping.tile.wall.WallEdgeDefinition.*;

@ProvidedBy(WallEdgeAtlasProvider.class)
@Singleton
public class WallEdgeAtlas {

	private IntMap<WallEdgeDefinition> layoutToDefinitionMap = new IntMap<>(256);
	private Map<String, Float> coordinateTranslationMap = new HashMap<>();

	private Map<DoorwaySize, Map<DoorwayOrientation, WallEdgeDefinition>> doorwayClosedDefinitionMap = new HashMap<>();
	private Map<DoorwaySize, Map<DoorwayOrientation, WallEdgeDefinition>> doorwayDefinitionMap = new HashMap<>();
	private static final WallEdgeDefinition emptyDefinition = new WallEdgeDefinition(new Array<>(), new Array<>());

	public WallEdgeAtlas(File edgeVerticesFile, File doorwayClosedVerticesFile, File doorwayEdgeVerticesFile) throws IOException {
		JSONObject wallEdgeFileJson = JSON.parseObject(FileUtils.readFileToString(edgeVerticesFile, "UTF-8"));
		JSONObject doorwayClosedFileJson = JSON.parseObject(FileUtils.readFileToString(doorwayClosedVerticesFile, "UTF-8"));
		JSONObject doorwayEdgeFileJson = JSON.parseObject(FileUtils.readFileToString(doorwayEdgeVerticesFile, "UTF-8"));

		for (DoorwaySize doorwaySize : DoorwaySize.values()) {
			doorwayDefinitionMap.put(doorwaySize, new HashMap<>());
			doorwayClosedDefinitionMap.put(doorwaySize, new HashMap<>());
		}


		/**
		 * Each vertex for a wall edge start or end point are one of 6 points, so rather than write
		 * the float values into the file and possibly write the wrong values, we're using a set of
		 * synonyms as x1, x2, y1, y2 which map to
		 * 0.3125, 0.671875, 0.59375, 0.96875 in the lower-left 0, 0 coordinate system, themselves mapping to
		 * 20px, 43px, 38px and 62px in the same lower-left (0, 0) co-ord system, mateching the points the walls converge at
		 */
		coordinateTranslationMap.put("0", 0.0f);
		coordinateTranslationMap.put("x1", INNER_EDGE_X1);
		coordinateTranslationMap.put("x2", INNER_EDGE_X2);
		coordinateTranslationMap.put("y1", INNER_EDGE_Y1);
		coordinateTranslationMap.put("y2", INNER_EDGE_Y2);
		coordinateTranslationMap.put("1", 1.0f);

		for (String layoutId : wallEdgeFileJson.keySet()) {
			JSONObject layoutJson = wallEdgeFileJson.getJSONObject(layoutId);

			WallEdgeDefinition definition = buildWallDefinitionFromJson(layoutJson);
			int layoutIdInteger = Integer.valueOf(layoutId);
			layoutToDefinitionMap.put(layoutIdInteger, definition);
		}

		for (String doorwaySizeName : doorwayClosedFileJson.keySet()) {
			DoorwaySize doorwaySize = DoorwaySize.valueOf(doorwaySizeName);
			JSONObject doorwaySizeJson = doorwayClosedFileJson.getJSONObject(doorwaySizeName);
			for (String doorOrientationName : doorwaySizeJson.keySet()) {
				DoorwayOrientation doorOrientation = DoorwayOrientation.valueOf(doorOrientationName);
				JSONObject layoutJson = doorwaySizeJson.getJSONObject(doorOrientationName);
				WallEdgeDefinition definition = buildWallDefinitionFromJson(layoutJson);
				doorwayClosedDefinitionMap.get(doorwaySize).put(doorOrientation, definition);
			}
		}

		for (String doorwaySizeName : doorwayEdgeFileJson.keySet()) {
			DoorwaySize doorwaySize = DoorwaySize.valueOf(doorwaySizeName);
			JSONObject doorwaySizeJson = doorwayEdgeFileJson.getJSONObject(doorwaySizeName);
			for (String doorOrientationName : doorwaySizeJson.keySet()) {
				DoorwayOrientation doorOrientation = DoorwayOrientation.valueOf(doorOrientationName);
				JSONObject layoutJson = doorwaySizeJson.getJSONObject(doorOrientationName);
				WallEdgeDefinition definition = buildWallDefinitionFromJson(layoutJson);
				doorwayDefinitionMap.get(doorwaySize).put(doorOrientation, definition);
			}
		}
	}

	private WallEdgeDefinition buildWallDefinitionFromJson(JSONObject layoutJson) {
		Array<Edge> innerEdges = new Array<>();
		JSONArray innerEdgeArray = layoutJson.getJSONArray("inner");
		for (int cursor = 0; cursor < innerEdgeArray.size(); cursor++) {
			JSONArray pointArray = innerEdgeArray.getJSONArray(cursor);
			innerEdges.add(pointArrayToWallEdge(pointArray));
		}

		Array<Edge> outerEdges = new Array<>();
		JSONArray outerEdgeArray = layoutJson.getJSONArray("outer");
		for (int cursor = 0; cursor < outerEdgeArray.size(); cursor++) {
			JSONArray pointArray = outerEdgeArray.getJSONArray(cursor);
			outerEdges.add(pointArrayToWallEdge(pointArray));
		}

		return new WallEdgeDefinition(innerEdges, outerEdges);
	}

	public WallEdgeDefinition getForLayoutId(int layoutId) {
		if (!layoutToDefinitionMap.containsKey(layoutId)) {
			throw new RuntimeException(this.getClass().getName() + " does not contain an entry for " + layoutId);
		}
		return layoutToDefinitionMap.get(layoutId);
	}

	public WallEdgeDefinition getForDoorway(Doorway doorway) {
		return doorwayDefinitionMap.get(doorway.getDoorwaySize()).getOrDefault(doorway.getOrientation(), emptyDefinition);
	}

	public WallEdgeDefinition getForClosedDoor(Doorway doorway) {
		return doorwayClosedDefinitionMap.get(doorway.getDoorwaySize()).getOrDefault(doorway.getOrientation(), emptyDefinition);
	}

	private Edge pointArrayToWallEdge(JSONArray pointArray) {
		// Assumes point data is an array of 4 strings out of 0, x1, y1, x2, y2, 1
		return new Edge(
				new Vector2(translatePoint(pointArray.getString(0)), translatePoint(pointArray.getString(1))),
				new Vector2(translatePoint(pointArray.getString(2)), translatePoint(pointArray.getString(3)))
		);
	}

	private float translatePoint(String pointValue) {
		if (coordinateTranslationMap.containsKey(pointValue)) {
			return coordinateTranslationMap.get(pointValue);
		} else {
			return Float.valueOf(pointValue);
		}
	}
}
