package technology.rocketjump.undermount.persistence.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.components.ItemAllocation;
import technology.rocketjump.undermount.entities.components.LiquidAllocation;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.MapVertex;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.types.JobRequestMessage;
import technology.rocketjump.undermount.misc.versioning.Version;
import technology.rocketjump.undermount.modding.model.ParsedMod;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;
import technology.rocketjump.undermount.rooms.Bridge;
import technology.rocketjump.undermount.rooms.HaulingAllocation;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.rooms.constructions.Construction;
import technology.rocketjump.undermount.rooms.constructions.ConstructionType;
import technology.rocketjump.undermount.settlement.SettlementState;
import technology.rocketjump.undermount.settlement.production.ProductionAssignment;
import technology.rocketjump.undermount.zones.Zone;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class SavedGameStateHolder {

	public final Map<String, GameMaterial> dynamicMaterials = new HashMap<>();
	public final Map<Long, ItemAllocation> itemAllocations = new HashMap<>();
	public final Map<Long, LiquidAllocation> liquidAllocations = new HashMap<>();
	public final Map<Long, HaulingAllocation> haulingAllocations = new HashMap<>();
	public final Map<Long, Job> jobs = new HashMap<>();
	public final Map<Long, JobRequestMessage> jobRequests = new TreeMap<>();
	public final Map<Long, Entity> entities = new HashMap<>();
	public final Map<Long, Bridge> bridges = new HashMap<>();
	public final Map<Long, Construction> constructions = new HashMap<>();
	public final Map<Long, Room> rooms = new HashMap<>();
	public final Map<Long, Zone> zones = new HashMap<>();
	public final Map<GridPoint2, MapTile> tiles = new HashMap<>();
	public final Map<GridPoint2, MapVertex> vertices = new HashMap<>();
	public final Map<Long, ProductionAssignment> productionAssignments = new HashMap<>();
	public final Map<String, Version> activeModNamesToVersions = new LinkedHashMap<>();
	private SettlementState settlementState;
	private List<Telegram> messages = new LinkedList<>();
	private GameClock gameClock;
	private long sequentialIdPointer;
	private Camera camera;
	private TiledMap map;

	public final JSONArray dynamicMaterialsJson;
	public final JSONArray itemAllocationsJson;
	public final JSONArray liquidAllocationsJson;
	public final JSONArray haulingAllocationsJson;
	public final JSONArray jobsJson;
	public final JSONArray jobRequestsJson;
	public final JSONArray entitiesJson;
	public final JSONArray entityIdsToLoad;
	public final JSONArray bridgesJson;
	public final JSONArray constructionsJson;
	public final JSONObject mapJson;
	public final JSONArray roomsJson;
	public final JSONArray zonesJson;
	public final JSONArray tileJson;
	public final JSONArray vertexJson;
	public final JSONArray productionAssignmentsJson;
	public final JSONObject settlementStateJson;
	public final JSONArray messagesJson;
	public final JSONObject gameClockJson;
	public final JSONObject cameraJson;
	public final JSONObject modsJson;

	public SavedGameStateHolder() {
		dynamicMaterialsJson = new JSONArray();
		itemAllocationsJson = new JSONArray();
		liquidAllocationsJson = new JSONArray();
		haulingAllocationsJson = new JSONArray();
		jobsJson = new JSONArray();
		jobRequestsJson = new JSONArray();
		entitiesJson = new JSONArray();
		entityIdsToLoad = new JSONArray();
		bridgesJson = new JSONArray();
		constructionsJson = new JSONArray();
		mapJson = new JSONObject(true);
		roomsJson = new JSONArray();
		zonesJson = new JSONArray();
		tileJson = new JSONArray();
		vertexJson = new JSONArray();
		productionAssignmentsJson = new JSONArray();
		settlementStateJson = new JSONObject(true);
		messagesJson = new JSONArray();
		gameClockJson = new JSONObject(true);
		cameraJson = new JSONObject(true);
		modsJson = new JSONObject(true);
	}

	public SavedGameStateHolder(JSONObject combined) {
		dynamicMaterialsJson = combined.getJSONArray("dynamicMaterials");
		itemAllocationsJson = combined.getJSONArray("itemAllocations");
		liquidAllocationsJson = combined.getJSONArray("liquidAllocations");
		haulingAllocationsJson = combined.getJSONArray("haulingAllocations");
		jobsJson = combined.getJSONArray("jobs");
		jobRequestsJson = combined.getJSONArray("jobRequests");
		entitiesJson = combined.getJSONArray("entities");
		entityIdsToLoad = combined.getJSONArray("entitiesToLoad");
		bridgesJson = combined.getJSONArray("bridges");
		constructionsJson = combined.getJSONArray("constructions");
		mapJson = combined.getJSONObject("map");
		roomsJson = combined.getJSONArray("rooms");
		zonesJson = combined.getJSONArray("zones");
		tileJson = combined.getJSONArray("tiles");
		vertexJson = combined.getJSONArray("vertices");
		productionAssignmentsJson = combined.getJSONArray("productionAssignments");
		settlementStateJson = combined.getJSONObject("settlementState");
		messagesJson = combined.getJSONArray("messages");
		gameClockJson = combined.getJSONObject("clock");
		sequentialIdPointer = combined.getLongValue("lastSequentialId");
		cameraJson = combined.getJSONObject("camera");
		modsJson = combined.getJSONObject("mods");
	}

	public JSONObject toCombinedJson() {
		JSONObject combined = new JSONObject(true);
		combined.put("version", GlobalSettings.VERSION.toString());
		combined.put("mods", modsJson);
		combined.put("dynamicMaterials", dynamicMaterialsJson);
		combined.put("itemAllocations", itemAllocationsJson);
		combined.put("liquidAllocations", liquidAllocationsJson);
		combined.put("haulingAllocations", haulingAllocationsJson);
		combined.put("jobs", jobsJson);
		combined.put("jobRequests", jobRequestsJson);
		combined.put("entities", entitiesJson);
		combined.put("entitiesToLoad", entityIdsToLoad);
		combined.put("bridges", bridgesJson);
		combined.put("constructions", constructionsJson);
		combined.put("map", mapJson);
		combined.put("rooms", roomsJson);
		combined.put("zones", zonesJson);
		combined.put("tiles", tileJson);
		combined.put("vertices", vertexJson);
		combined.put("productionAssignments", productionAssignmentsJson);
		combined.put("settlementState", settlementStateJson);
		combined.put("messages", messagesJson);
		combined.put("clock", gameClockJson);
		combined.put("lastSequentialId", sequentialIdPointer);
		combined.put("camera", cameraJson);
		return combined;
	}

	/**
	 * This is the inverse of turning in-game instances into json
	 */
	public void jsonToObjects(SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// Load in mod info first in case this is responsible for a later error
		for (Map.Entry<String, Object> entry : modsJson.entrySet()) {
			String modName = entry.getKey();
			Version modVersion = new Version(entry.getValue().toString());
			activeModNamesToVersions.put(modName, modVersion);
		}

		convertJsonToInstances(dynamicMaterialsJson, GameMaterial.class, relatedStores);
		// Add dynamic materials to GameMaterialDictionary as shim before GameContext is reset
		for (GameMaterial dynamicMaterial : dynamicMaterials.values()) {
			relatedStores.gameMaterialDictionary.add(dynamicMaterial);
		}

		convertJsonToInstances(itemAllocationsJson, ItemAllocation.class, relatedStores);
		convertJsonToInstances(liquidAllocationsJson, LiquidAllocation.class, relatedStores);
		convertJsonToInstances(haulingAllocationsJson, HaulingAllocation.class, relatedStores);
		convertJsonToInstances(zonesJson, Zone.class, relatedStores);
		convertJsonToInstances(jobsJson, Job.class, relatedStores);
		convertJsonToInstances(jobRequestsJson, JobRequestMessage.class, relatedStores);
		convertJsonToInstances(productionAssignmentsJson, ProductionAssignment.class, relatedStores);
		convertJsonToInstances(entitiesJson, Entity.class, relatedStores);
		convertJsonToInstances(bridgesJson, Bridge.class, relatedStores);

		for (int cursor = 0; cursor < constructionsJson.size(); cursor++) {
			try {
				JSONObject asJson = constructionsJson.getJSONObject(cursor);
				ConstructionType type = EnumParser.getEnumValue(asJson, "_type", ConstructionType.class, null);
				Construction construction = type.classType.getDeclaredConstructor().newInstance();
				construction.readFrom(asJson, this, relatedStores);
				constructions.put(construction.getId(), construction);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e); // This should only happen by programmer error - the persistable classes not having a no-arg constructor
			}
		}

		map = createMap(relatedStores);
		for (int y = 0; y < map.getHeight(); y++) {
			for (int x = 0; x < map.getWidth(); x++) {
				JSONObject tileAsJson = tileJson.getJSONObject(x + (y * map.getWidth()));
				map.getTile(x, y).readFrom(tileAsJson, this, relatedStores);
			}
		}
		for (int y = 0; y <= map.getHeight(); y++) {
			for (int x = 0; x <= map.getWidth(); x++) {
				JSONObject vertexAsJson = vertexJson.getJSONObject(x + (y * (map.getWidth() + 1)));
				map.getVertex(x, y).readFrom(vertexAsJson, this, relatedStores);
			}
		}

		convertJsonToInstances(roomsJson, Room.class, relatedStores);

		this.settlementState = new SettlementState();
		this.settlementState.readFrom(settlementStateJson, this, relatedStores);

		this.gameClock = new GameClock();
		this.gameClock.readFrom(gameClockJson, this, relatedStores);

		SequentialIdGenerator.setLastId(sequentialIdPointer);

		// Messages/telegrams are handled elsewhere
		// PrimaryCameraWrapper handled elsewhere
	}

	private TiledMap createMap(SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		long seed = mapJson.getLongValue("seed");
		int width = mapJson.getIntValue("width");
		int height = mapJson.getIntValue("height");
		FloorType defaultFloor = relatedStores.floorTypeDictionary.getByFloorTypeName(mapJson.getString("defaultFloor"));
		if (defaultFloor == null) {
			throw new InvalidSaveException("Could not find floor type with name " + mapJson.getString("defaultFloor"));
		}
		GameMaterial defaultFloorMaterial = relatedStores.gameMaterialDictionary.getByName(mapJson.getString("defaultFloorMaterial"));
		if (defaultFloorMaterial == null) {
			throw new InvalidSaveException("Could not find material with name " + mapJson.getString("defaultFloorMaterial"));
		}
		TiledMap map = new TiledMap(seed, width, height, defaultFloor, defaultFloorMaterial);
		map.setEmbarkPoint(JSONUtils.gridPoint2(mapJson.getJSONObject("embarkPoint")));
		map.setNumRegions(mapJson.getIntValue("numRegions"));

		for (Zone value : zones.values()) {
			map.addZone(value);
		}


		return map;
	}

	private  void convertJsonToInstances(JSONArray jsonArray,
			 Class<? extends Persistable> persistableType, SavedGameDependentDictionaries relatedStores)
			throws InvalidSaveException {
		for (int cursor = 0; cursor < jsonArray.size(); cursor++) {
			try {
				JSONObject asJson = jsonArray.getJSONObject(cursor);
				Persistable persistable = persistableType.getDeclaredConstructor().newInstance();
				persistable.readFrom(asJson, this, relatedStores);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e); // This should only happen by programmer error - the persistable classes not having a no-arg constructor
			}
		}
	}

	public void setActiveMods(List<ParsedMod> activeMods) {
		for (ParsedMod activeMod : activeMods) {
			if (!activeMod.getInfo().isBaseMod()) {
				activeModNamesToVersions.put(activeMod.getInfo().getName(), activeMod.getInfo().getVersion());
				modsJson.put(activeMod.getInfo().getName(), activeMod.getInfo().getVersion().toString());
			}
		}
	}

	public List<Telegram> getMessages() {
		return messages;
	}

	public void setMessages(List<Telegram> messages) {
		this.messages = messages;
	}

	public GameClock getGameClock() {
		return gameClock;
	}

	public void setGameClock(GameClock gameClock) {
		this.gameClock = gameClock;
	}

	public long getSequentialIdPointer() {
		return sequentialIdPointer;
	}

	public void setSequentialIdPointer(long sequentialIdPointer) {
		this.sequentialIdPointer = sequentialIdPointer;
	}

	public Camera getCamera() {
		return camera;
	}

	public void setCamera(Camera camera) {
		this.camera = camera;
	}

	public SettlementState getSettlementState() {
		return settlementState;
	}

	public void setSettlementState(SettlementState settlementState) {
		this.settlementState = settlementState;
	}

	public TiledMap getMap() {
		return map;
	}
}
