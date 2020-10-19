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
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.doors.Doorway;
import technology.rocketjump.undermount.doors.DoorwayOrientation;
import technology.rocketjump.undermount.doors.DoorwaySize;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.guice.WallEdgeAtlasProvider;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

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

	private Map<EntityAssetOrientation, Map<GameMaterialType, WallEdgeDefinition>> wallCapDefinitionMap = new HashMap<>();
	private Map<DoorwaySize, Map<DoorwayOrientation, Map<GameMaterialType, WallEdgeDefinition>>> doorwayDefinitionMap = new HashMap<>();

	public WallEdgeAtlas(File edgeVerticesFile, File wallCapEdgeVerticesFile, File DoorwayEdgeVerticesFile) throws IOException {
		JSONObject wallEdgeFileJson = JSON.parseObject(FileUtils.readFileToString(edgeVerticesFile, "UTF-8"));
		JSONObject wallCapFileJson = JSON.parseObject(FileUtils.readFileToString(wallCapEdgeVerticesFile, "UTF-8"));
		JSONObject doorwayEdgeFileJson = JSON.parseObject(FileUtils.readFileToString(DoorwayEdgeVerticesFile, "UTF-8"));

		for (DoorwaySize doorwaySize : DoorwaySize.values()) {
			Map<DoorwayOrientation, Map<GameMaterialType, WallEdgeDefinition>> doorSizeMap = new HashMap<>();
			for (DoorwayOrientation doorwayOrientation : DoorwayOrientation.values()) {
				doorSizeMap.put(doorwayOrientation, new HashMap<>());
			}
			doorwayDefinitionMap.put(doorwaySize, doorSizeMap);
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

		for (String orientationName : wallCapFileJson.keySet()) {
			EntityAssetOrientation orientation = EntityAssetOrientation.valueOf(orientationName);
			JSONObject materialMapJson = wallCapFileJson.getJSONObject(orientationName);

			Map<GameMaterialType, WallEdgeDefinition> typeToDefinitions = wallCapDefinitionMap.computeIfAbsent(orientation, o -> new HashMap<>());

			for (String materialTypeName : materialMapJson.keySet()) {
				GameMaterialType materialType = GameMaterialType.valueOf(materialTypeName);
				JSONObject layoutJson = materialMapJson.getJSONObject(materialTypeName);
				WallEdgeDefinition definition = buildWallDefinitionFromJson(layoutJson);
				typeToDefinitions.put(materialType, definition);
			}
		}

		for (String doorwaySizeName : doorwayEdgeFileJson.keySet()) {
			DoorwaySize doorwaySize = DoorwaySize.valueOf(doorwaySizeName);
			JSONObject doorwaySizeJson = doorwayEdgeFileJson.getJSONObject(doorwaySizeName);
			for (String doorOrientationName : doorwaySizeJson.keySet()) {
				DoorwayOrientation doorOrientation = DoorwayOrientation.valueOf(doorOrientationName);
				JSONObject doorOrientationJson = doorwaySizeJson.getJSONObject(doorOrientationName);
				for (String materialTypeName : doorOrientationJson.keySet()) {
					GameMaterialType materialType = GameMaterialType.valueOf(materialTypeName);
					JSONObject layoutJson = doorOrientationJson.getJSONObject(materialTypeName);
					WallEdgeDefinition definition = buildWallDefinitionFromJson(layoutJson);
					doorwayDefinitionMap.get(doorwaySize).get(doorOrientation).put(materialType, definition);
				}

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

	public WallEdgeDefinition getForWallCap(Entity wallCapEntity, Doorway doorway) {
		EntityAssetOrientation orientation = wallCapEntity.getLocationComponent().getOrientation();
		GameMaterialType materialType = doorway.getDoorwayMaterialType();

		return wallCapDefinitionMap.get(orientation).get(materialType);
	}

	public WallEdgeDefinition getForClosedDoor(Doorway doorway) {
		return doorwayDefinitionMap.get(doorway.getDoorwaySize()).get(doorway.getOrientation()).get(doorway.getDoorwayMaterialType());
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
