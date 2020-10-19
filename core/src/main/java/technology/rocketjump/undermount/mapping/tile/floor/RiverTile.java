package technology.rocketjump.undermount.mapping.tile.floor;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.Random;

public class RiverTile implements ChildPersistable {

	public static final int MAX_WATER_PER_TILE = 7;
	private static final float CHANCE_WATER_LOST_WHEN_FULL = 0.2f;
	public static final int INACTIVITY_THRESHOLD = 32; // This needs to be high enough to let a river propagate from a single tile - found by testing

	private int waterAmount = 0; // Max of 7
	private boolean isSourceTile; // Source tiles are always 7 of 7 water
	private boolean isActive = true;
	private int inactivityCounter = 0; // Tile is properly inactive if set to inactive over multiple updates

	private Vector2 flowDirection = new Vector2(0, 0);

	public RiverTile(boolean isSourceTile) {
		setIsSourceTile(isSourceTile);
	}

	public boolean isSourceTile() {
		return isSourceTile;
	}

	public int getWaterAmount() {
		return waterAmount;
	}

	public void setWaterAmount(int waterAmount) {
		this.waterAmount = waterAmount;
	}

	public void incrementWater(CompassDirection inputDirection) {
		waterAmount++;
		if (waterAmount > MAX_WATER_PER_TILE) {
			waterAmount = MAX_WATER_PER_TILE;
		}
		flowDirection.add(inputDirection.toVector());
	}

	public void decrementWater(CompassDirection outputDirection, Random random) {
		if (!isSourceTile) {
			if (waterAmount == MAX_WATER_PER_TILE) {
				// "Full" water can spawn more, most of the time, but not always
				if (random.nextFloat() < CHANCE_WATER_LOST_WHEN_FULL) {
					waterAmount--;
				}
			} else {
				waterAmount--;
			}
			if (waterAmount < 0) {
				waterAmount = 0;
			}
		}
		flowDirection.add(outputDirection.toVector());
	}

	public Vector2 getFlowDirection() {
		return flowDirection.nor().scl(0.5f);
	}

	public void active() {
		this.inactivityCounter = 0;
		this.isActive = true;
	}

	public void inactive() {
		this.inactivityCounter++;
		if (inactivityCounter >= INACTIVITY_THRESHOLD) {
			this.isActive = false;
		}
	}

	public boolean isActive() {
		return isActive;
	}

	public void setIsSourceTile(boolean isSource) {
		this.isSourceTile = isSource;
		if (isSourceTile) {
			waterAmount = MAX_WATER_PER_TILE;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (waterAmount != 7) {
			asJson.put("amount", waterAmount);
		}
		if (isSourceTile) {
			asJson.put("sourceTile", true);
		}
		if (isActive) {
			asJson.put("active", true);
		}
		if (inactivityCounter != 0) {
			asJson.put("inactivityCounter", inactivityCounter);
		}
		asJson.put("direction", JSONUtils.toJSON(flowDirection));
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		Integer amount = asJson.getInteger("amount");
		if (amount == null) {
			this.waterAmount = 7;
		} else {
			this.waterAmount = amount;
		}
		isSourceTile = asJson.getBooleanValue("sourceTile");
		isActive = asJson.getBooleanValue("active");
		inactivityCounter = asJson.getIntValue("inactivityCounter");
		flowDirection = JSONUtils.vector2(asJson.getJSONObject("direction"));
	}
}
