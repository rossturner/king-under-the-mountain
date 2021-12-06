package technology.rocketjump.undermount.mapping.tile.underground;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TileLiquidFlow implements ChildPersistable {

	public static final int MAX_LIQUID_FLOW_PER_TILE = 7;
	private static final int MAX_FLOW_DIRECTION_STORAGE = 50;
	private static final float CHANCE_FULL_WATER_SPAWNS_MORE = 0.4f;

	private GameMaterial liquidMaterial;
	private int liquidAmount;
	private Vector2 averagedFlowDirection = new Vector2();
	private List<Vector2> lastFlowDirections = new ArrayList<>();

	public GameMaterial getLiquidMaterial() {
		return liquidMaterial;
	}

	public void setLiquidMaterial(GameMaterial liquidMaterial) {
		this.liquidMaterial = liquidMaterial;
	}

	public int getLiquidAmount() {
		return liquidAmount;
	}

	public void setLiquidAmount(int liquidAmount) {
		this.liquidAmount = liquidAmount;
	}

	public Vector2 getAveragedFlowDirection() {
		return averagedFlowDirection;
	}

	public List<Vector2> getLastFlowDirections() {
		return lastFlowDirections;
	}

	public void incrementWater(CompassDirection inputDirection) {
		liquidAmount++;
		if (liquidAmount > MAX_LIQUID_FLOW_PER_TILE) {
			liquidAmount = MAX_LIQUID_FLOW_PER_TILE;
		}
		addFlowDirection(inputDirection);
	}

	public void decrementWater(CompassDirection outputDirection, Random random) {
		if (liquidAmount == MAX_LIQUID_FLOW_PER_TILE) {
			// "Full" water can spawn more, half of the time
			if (random.nextFloat() > CHANCE_FULL_WATER_SPAWNS_MORE) {
				liquidAmount--;
			}
		} else {
			liquidAmount--;
		}
		if (liquidAmount < 0) {
			liquidAmount = 0;
		}
		addFlowDirection(outputDirection);
	}

	private void addFlowDirection(CompassDirection inputDirection) {
		lastFlowDirections.add(inputDirection.toVector().cpy().nor());
		if (lastFlowDirections.size() > MAX_FLOW_DIRECTION_STORAGE) {
			lastFlowDirections.remove(0);
		}

		averagedFlowDirection = new Vector2();
		for (Vector2 lastFlowDirection : lastFlowDirections) {
			averagedFlowDirection.add(lastFlowDirection);
		}
		averagedFlowDirection.nor().scl(0.5f);
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (liquidMaterial != null) {
			asJson.put("liquidMaterial", liquidMaterial.getMaterialName());
		}

		if (liquidAmount != 0) {
			asJson.put("liquidAmount", liquidAmount);
		}

		asJson.put("averagedFlowDirection", JSONUtils.toJSON(averagedFlowDirection));

		JSONArray lastFlowDirectionsJson = new JSONArray();
		for (Vector2 lastFlowDirection : lastFlowDirections) {
			JSONObject lastFlowJson = JSONUtils.toJSON(lastFlowDirection);
			lastFlowDirectionsJson.add(lastFlowJson);
		}
		if (lastFlowDirectionsJson.size() > 0) {
			asJson.put("lastFlowDirections", lastFlowDirectionsJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		String liquidMaterialName = asJson.getString("liquidMaterial");
		if (liquidMaterialName != null) {
			liquidMaterial = relatedStores.gameMaterialDictionary.getByName(liquidMaterialName);
			if (liquidMaterial == null) {
				throw new InvalidSaveException("Could not find material with name " + liquidMaterialName);
			}
		}

		this.liquidAmount = asJson.getIntValue("liquidAmount");

		this.averagedFlowDirection = JSONUtils.vector2(asJson.getJSONObject("averagedFlowDirection"));

		JSONArray lastFlowDirectionsJson = asJson.getJSONArray("lastFlowDirections");
		if (lastFlowDirectionsJson != null) {
			for (int cursor = 0; cursor < lastFlowDirectionsJson.size(); cursor++) {
				JSONObject lastFlowJson = lastFlowDirectionsJson.getJSONObject(cursor);
				Vector2 lastFlow = JSONUtils.vector2(lastFlowJson);
				this.lastFlowDirections.add(lastFlow);
			}
		}
	}
}
