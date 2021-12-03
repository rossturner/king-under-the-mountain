package technology.rocketjump.undermount.zones;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class Zone implements Persistable {

	private long zoneId;
	private ZoneClassification classification;
	private final List<ZoneTile> zoneTiles = new ArrayList<>();
	private int regionId = -1;
	private boolean active;
	private boolean requiresAllocation;

	private final Vector2 avgWorldPosition = new Vector2();

	public Zone() {

	}

	public Zone(ZoneClassification classification) {
		zoneId = SequentialIdGenerator.nextId();
		this.classification = classification;
	}

	public void add(MapTile accessTile, MapTile targetTile) {
		if (accessTile.isNavigable(null)) {
			zoneTiles.add(new ZoneTile(accessTile, targetTile));
			accessTile.addToZone(this);
			regionId = accessTile.getRegionId();
			recalculatePosition();
		}
	}

	public void remove(ZoneTile tile, TiledMap map) {
		if (zoneTiles.remove(tile)) {
			MapTile accessTile = map.getTile(tile.getAccessLocation());
			accessTile.removeFromZone(this);
			recalculatePosition();
		}
	}

	public void removeFromAllTiles(TiledMap map) {
		for (ZoneTile zoneTile : zoneTiles) {
			MapTile accessTile = map.getTile(zoneTile.getAccessLocation());
			accessTile.removeFromZone(this);
		}
		recalculatePosition();
	}

	/**
	 * Used after removeFromAllTiles is called during a zone move
	 */
	public void addToAllTiles(TiledMap tiledMap) {
		for (ZoneTile zoneTile : zoneTiles) {
			MapTile accessTile = tiledMap.getTile(zoneTile.getAccessLocation());
			accessTile.addToZone(this);
		}
		recalculatePosition();
	}

	/**
	 * Region ID might have changed
	 */
	public void recalculate(TiledMap map) {
		for (ZoneTile zoneTile : new ArrayList<>(zoneTiles)) {
			MapTile accessTile = map.getTile(zoneTile.getAccessLocation());
			if (accessTile.isNavigable(null)) {
				regionId = accessTile.getRegionId();
			} else {
				zoneTiles.remove(zoneTile);
				accessTile.removeFromZone(this);
			}
		}
	}

	public boolean isEmpty() {
		return zoneTiles.isEmpty();
	}

	public Iterator<ZoneTile> iterator() {
		return zoneTiles.iterator();
	}

	private void recalculatePosition() {
		// This could be done a bit neater as a moving average rather than recalculating every time
		avgWorldPosition.set(0, 0);
		for (ZoneTile tile : zoneTiles) {
			avgWorldPosition.add(tile.getAccessLocation().x + 0.5f, tile.getAccessLocation().y + 0.5f);
		}
		avgWorldPosition.scl(1f / zoneTiles.size());
	}

	public Vector2 getAvgWorldPosition() {
		return avgWorldPosition;
	}

	public long getZoneId() {
		return zoneId;
	}

	public ZoneClassification getClassification() {
		return classification;
	}

	public int getRegionId() {
		return regionId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Zone zone = (Zone) o;
		return zoneId == zone.zoneId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(zoneId);
	}

	@Override
	public String toString() {
		String description = "Zone: " +
				"id=" + zoneId +
				"," + classification +
				", regionId=" + regionId +
				"avgPos=" + avgWorldPosition.toString();
		if (!active) {
			description += ", not active";
		}

		return description;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void setRegionId(int regionId) {
		this.regionId = regionId;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.zones.containsKey(zoneId)) {
			return;
		}

		JSONObject asJson = new JSONObject(true);

		asJson.put("id", zoneId);
		JSONObject classificationJson = new JSONObject(true);
		classification.writeTo(classificationJson, savedGameStateHolder);
		asJson.put("classification", classificationJson);

		JSONArray zoneTilesJson = new JSONArray();
		for (ZoneTile zoneTile : zoneTiles) {
			JSONObject zoneTileJson = new JSONObject(true);
			zoneTile.writeTo(zoneTileJson, savedGameStateHolder);
			zoneTilesJson.add(zoneTileJson);
		}
		asJson.put("zoneTiles", zoneTilesJson);

		asJson.put("regionId", regionId);
		if (active) {
			asJson.put("active", true);
		}


		savedGameStateHolder.zones.put(zoneId, this);
		savedGameStateHolder.zonesJson.add(asJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.zoneId = asJson.getLongValue("id");

		this.classification = new ZoneClassification();
		this.classification.readFrom(asJson.getJSONObject("classification"), savedGameStateHolder, relatedStores);

		JSONArray zoneTilesJson = asJson.getJSONArray("zoneTiles");
		for (int cursor = 0; cursor < zoneTilesJson.size(); cursor++) {
			ZoneTile zoneTile = new ZoneTile();
			zoneTile.readFrom(zoneTilesJson.getJSONObject(cursor), savedGameStateHolder, relatedStores);
			this.zoneTiles.add(zoneTile);
		}

		this.regionId = asJson.getIntValue("regionId");
		this.active = asJson.getBooleanValue("active");

		recalculatePosition();

		savedGameStateHolder.zones.put(zoneId, this);
	}

	public boolean isRequiresAllocation() {
		return requiresAllocation;
	}

	public void setRequiresAllocation(boolean requiresAllocation) {
		this.requiresAllocation = requiresAllocation;
	}
}
