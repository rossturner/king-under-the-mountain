package technology.rocketjump.undermount.zones;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.Objects;

public class ZoneTile implements ChildPersistable {

	private GridPoint2 accessLocation;
	private GridPoint2 targetTile;

	public ZoneTile() {

	}

	public ZoneTile(MapTile accessLocation, MapTile targetTile) {
		this.accessLocation = accessLocation.getTilePosition();
		this.targetTile = targetTile.getTilePosition();
	}

	public GridPoint2 getAccessLocation() {
		return accessLocation;
	}

	public GridPoint2 getTargetTile() {
		return targetTile;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ZoneTile zoneTile = (ZoneTile) o;
		return Objects.equals(accessLocation, zoneTile.accessLocation) &&
				Objects.equals(targetTile, zoneTile.targetTile);
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessLocation, targetTile);
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("accessible", JSONUtils.toJSON(accessLocation));
		asJson.put("target", JSONUtils.toJSON(targetTile));
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.accessLocation = JSONUtils.gridPoint2(asJson.getJSONObject("accessible"));
		this.targetTile = JSONUtils.gridPoint2(asJson.getJSONObject("target"));
	}
}
