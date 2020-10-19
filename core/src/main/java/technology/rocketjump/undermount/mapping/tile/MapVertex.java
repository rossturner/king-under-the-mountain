package technology.rocketjump.undermount.mapping.tile;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.Objects;

/**
 * This class represents a point at the corner of a tile in the world map,
 * as decribed in http://www-cs-students.stanford.edu/~amitp/game-programming/grids/
 *
 * It is used to hold the amount of light from "outside" light sources that permeate
 * "indoor" areas.
 */
public class MapVertex implements Persistable {

	private float outsideLightAmount = 0.0f; // This is from 0.0 (none) to 1.0 (total) amount of lighting from "outside" e.g. the sun or moon
	private float heightmapValue;
	private Vector2 waterFlowDirection = Vector2.Zero;
	private final int vertexX;
	private final int vertexY;
	private float explorationVisibility = 0f; // 0 to 1 for unexplored to explored

	public MapVertex(int vertexX, int vertexY) {
		this.vertexX = vertexX;
		this.vertexY = vertexY;
	}

	public float getOutsideLightAmount() {
		return outsideLightAmount;
	}

	public void setOutsideLightAmount(float outsideLightAmount) {
		this.outsideLightAmount = outsideLightAmount;
	}

	public int getVertexX() {
		return vertexX;
	}

	public int getVertexY() {
		return vertexY;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MapVertex mapVertex = (MapVertex) o;
		return vertexX == mapVertex.vertexX &&
				vertexY == mapVertex.vertexY;
	}

	@Override
	public int hashCode() {
		return Objects.hash(vertexX, vertexY);
	}

	public float getHeightmapValue() {
		return heightmapValue;
	}

	public void setHeightmapValue(float heightmapValue) {
		this.heightmapValue = heightmapValue;
	}

	public Vector2 getWaterFlowDirection() {
		return waterFlowDirection;
	}

	public void setWaterFlowDirection(Vector2 waterFlowDirection) {
		this.waterFlowDirection = waterFlowDirection;
	}

	public float getExplorationVisibility() {
		return explorationVisibility;
	}

	public void setExplorationVisibility(float explorationVisibility) {
		this.explorationVisibility = explorationVisibility;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		// Not checking if this is already in stateHolder

		JSONObject asJson = new JSONObject(true);
		if (outsideLightAmount > 0f) {
			asJson.put("light", outsideLightAmount);
		}

		asJson.put("height", heightmapValue);

		if (!waterFlowDirection.equals(Vector2.Zero)) {
			asJson.put("flow", JSONUtils.toJSON(waterFlowDirection));
		}

		if (explorationVisibility > 0) {
			asJson.put("exploration", explorationVisibility);
		}


		savedGameStateHolder.vertices.put(new GridPoint2(vertexX, vertexY), this);
		savedGameStateHolder.vertexJson.add(asJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.outsideLightAmount = asJson.getFloatValue("light");
		this.heightmapValue = asJson.getFloatValue("height");
		this.waterFlowDirection = JSONUtils.vector2(asJson.getJSONObject("flow"));
		if (this.waterFlowDirection == null) {
			this.waterFlowDirection = Vector2.Zero;
		}

		this.explorationVisibility = asJson.getFloatValue("exploration");

		savedGameStateHolder.vertices.put(new GridPoint2(vertexX, vertexY), this);
	}
}
