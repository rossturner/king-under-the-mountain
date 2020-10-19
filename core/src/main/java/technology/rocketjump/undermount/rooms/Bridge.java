package technology.rocketjump.undermount.rooms;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.floor.BridgeTile;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.sprites.model.BridgeOrientation;
import technology.rocketjump.undermount.sprites.model.BridgeTileLayout;
import technology.rocketjump.undermount.sprites.model.BridgeType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;

public class Bridge implements Persistable {

	private long bridgeId;
	private BridgeType bridgeType;
	private GameMaterial material;
	private BridgeOrientation orientation = BridgeOrientation.EAST_WEST;
	private final Map<GridPoint2, BridgeTile> bridgeTiles = new HashMap<>();
	private final Vector2 avgWorldPosition = new Vector2();
	private Job deconstructionJob = null;

	public Bridge() {
	}

	public Bridge(List<MapTile> mapTiles, GameMaterial material, BridgeOrientation orientation, BridgeType bridgeType) {
		bridgeId = SequentialIdGenerator.nextId();
		this.material = material;
		this.orientation = orientation;
		for (MapTile mapTile : mapTiles) {
			BridgeTile bridgeTile = new BridgeTile();
			addTile(mapTile.getTilePosition(), bridgeTile);
		}
		this.bridgeType = bridgeType;

		updateTileLayouts();
	}

	public BridgeType getBridgeType() {
		return bridgeType;
	}

	public GameMaterial getMaterial() {
		return material;
	}

	public BridgeOrientation getOrientation() {
		return orientation;
	}
	// TODO move this to constructor so recalculatePosition only needs running once

	public void addTile(GridPoint2 position, BridgeTile tile) {
		bridgeTiles.put(position, tile);
		recalculatePosition();
	}

	public Vector2 getAvgWorldPosition() {
		return avgWorldPosition;
	}

	private void recalculatePosition() {
		avgWorldPosition.set(0, 0);
		for (GridPoint2 tilePosition : bridgeTiles.keySet()) {
			avgWorldPosition.add(tilePosition.x + 0.5f, tilePosition.y + 0.5f);
		}
		avgWorldPosition.scl(1f / bridgeTiles.size());
	}

	private void updateTileLayouts() {
		for (Map.Entry<GridPoint2, BridgeTile> entry : bridgeTiles.entrySet()) {
			boolean northTile = bridgeTiles.containsKey(entry.getKey().cpy().add(0, 1));
			boolean southTile = bridgeTiles.containsKey(entry.getKey().cpy().add(0, -1));
			boolean westTile = bridgeTiles.containsKey(entry.getKey().cpy().add(-1, 0));
			boolean eastTile = bridgeTiles.containsKey(entry.getKey().cpy().add(1, 0));
			entry.getValue().setBridgeTileLayout(BridgeTileLayout.byNeighbours(northTile, southTile, westTile, eastTile));
		}
	}

	public long getBridgeId() {
		return bridgeId;
	}

	public Set<GridPoint2> getLocations() {
		return bridgeTiles.keySet();
	}

	public Set<Map.Entry<GridPoint2, BridgeTile>> entrySet() {
		return bridgeTiles.entrySet();
	}

	public void setMaterial(GameMaterial material) {
		this.material = material;
	}

	public boolean isBeingDeconstructed() {
		return deconstructionJob != null;
	}

	public Job getDeconstructionJob() {
		return deconstructionJob;
	}

	public void setDeconstructionJob(Job job) {
		deconstructionJob = job;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.bridges.containsKey(bridgeId)) {
			return;
		}
		JSONObject asJson = new JSONObject(true);
		asJson.put("id", bridgeId);
		if (!bridgeType.getMaterialType().equals(GameMaterialType.STONE)) {
			asJson.put("type", bridgeType.getMaterialType().name());
		}

		asJson.put("material", material.getMaterialName());
		if (material.equals(NULL_MATERIAL)) {
			asJson.put("materialType", material.getMaterialType().name());
		}

		if (!BridgeOrientation.EAST_WEST.equals(orientation)) {
			asJson.put("orientation", orientation.name());
		}

		JSONArray tilesJson = new JSONArray();
		for (Map.Entry<GridPoint2, BridgeTile> tileEntry : bridgeTiles.entrySet()) {
			JSONObject tileJson = new JSONObject(true);
			tileJson.put("location", JSONUtils.toJSON(tileEntry.getKey()));
			tileEntry.getValue().writeTo(tileJson, savedGameStateHolder);
			tilesJson.add(tileJson);
		}
		asJson.put("tiles", tilesJson);

		if (deconstructionJob != null) {
			deconstructionJob.writeTo(savedGameStateHolder);
			asJson.put("deconstructionJobId", deconstructionJob.getJobId());
		}

		savedGameStateHolder.bridgesJson.add(asJson);
		savedGameStateHolder.bridges.put(bridgeId, this);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.bridgeId = asJson.getLongValue("id");
		GameMaterialType bridgeMaterialType = EnumParser.getEnumValue(asJson, "type", GameMaterialType.class, GameMaterialType.STONE);
		this.bridgeType = relatedStores.bridgeTypeDictionary.getByMaterialType(bridgeMaterialType);

		this.material = relatedStores.gameMaterialDictionary.getByName(asJson.getString("material"));
		if (this.material == null) {
			throw new InvalidSaveException("Could not find material with name " + asJson.getString("material") + " for " + getClass().getSimpleName());
		} else if (NULL_MATERIAL.equals(material)) {
			GameMaterialType type = EnumParser.getEnumValue(asJson, "materialType", GameMaterialType.class, GameMaterialType.STONE);
			this.material = GameMaterial.nullMaterialWithType(type);
		}

		this.orientation = EnumParser.getEnumValue(asJson, "orientation", BridgeOrientation.class, BridgeOrientation.EAST_WEST);

		JSONArray tilesJson = asJson.getJSONArray("tiles");
		for (int cursor = 0; cursor < tilesJson.size(); cursor++) {
			JSONObject tileJson = tilesJson.getJSONObject(cursor);
			GridPoint2 location = JSONUtils.gridPoint2(tileJson.getJSONObject("location"));
			BridgeTile tile = new BridgeTile();
			tile.readFrom(tileJson, savedGameStateHolder, relatedStores);
 			bridgeTiles.put(location, tile);
		}

		Long deconstructionJobId = asJson.getLong("deconstructionJobId");
		if (deconstructionJobId != null) {
			this.deconstructionJob = savedGameStateHolder.jobs.get(deconstructionJobId);
			if (this.deconstructionJob == null) {
				throw new InvalidSaveException("Could not find job with ID " + deconstructionJobId);
			}
		}

		recalculatePosition();

		savedGameStateHolder.bridges.put(bridgeId, this);
	}
}
