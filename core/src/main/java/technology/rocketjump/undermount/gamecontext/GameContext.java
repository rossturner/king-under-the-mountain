package technology.rocketjump.undermount.gamecontext;

import technology.rocketjump.undermount.constants.ConstantsRepo;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.mapping.model.MapEnvironment;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.types.JobRequestMessage;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.rooms.constructions.Construction;
import technology.rocketjump.undermount.settlement.SettlementState;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Simple bean to hold all state related to a game instance
 */
public class GameContext {

	private final Map<Long, Job> jobs = new ConcurrentHashMap<>();
	private final Map<Long, Entity> entities = new ConcurrentHashMap<>();
	private final Map<Long, Construction> constructions = new ConcurrentHashMap<>();
	private final Map<Long, Room> rooms = new ConcurrentHashMap<>();
	private final Deque<JobRequestMessage> jobRequestQueue = new LinkedBlockingDeque<>();
	private final Map<String, GameMaterial> dynamicallyCreatedMaterialsByCombinedId = new HashMap<>();
	private SettlementState settlementState = new SettlementState();

	private TiledMap areaMap;
	private MapEnvironment mapEnvironment;
	private Random random;
	private GameClock gameClock;
	private ConstantsRepo constantsRepo; // not persisted, assigned by ConstantsRepo singleton

	public TiledMap getAreaMap() {
		return areaMap;
	}

	public void setAreaMap(TiledMap areaMap) {
		this.areaMap = areaMap;
	}

	public MapEnvironment getMapEnvironment() {
		return mapEnvironment;
	}

	public void setMapEnvironment(MapEnvironment mapEnvironment) {
		this.mapEnvironment = mapEnvironment;
	}

	public Random getRandom() {
		return random;
	}

	public void setRandom(Random random) {
		this.random = random;
	}

	public void setGameClock(GameClock gameClock) {
		this.gameClock = gameClock;
	}

	public GameClock getGameClock() {
		return gameClock;
	}

	public Map<Long, Job> getJobs() {
		return jobs;
	}

	public Map<Long, Entity> getEntities() {
		return entities;
	}

	public Map<Long, Room> getRooms() {
		return rooms;
	}

	public Map<Long, Construction> getConstructions() {
		return constructions;
	}

	public Deque<JobRequestMessage> getJobRequestQueue() {
		return jobRequestQueue;
	}

	public void setSettlementState(SettlementState settlementState) {
		this.settlementState = settlementState;
	}

	public Map<String, GameMaterial> getDynamicallyCreatedMaterialsByCombinedId() {
		return dynamicallyCreatedMaterialsByCombinedId;
	}

	public SettlementState getSettlementState() {
		return settlementState;
	}

	public void setConstantsRepo(ConstantsRepo constantsRepo) {
		this.constantsRepo = constantsRepo;
	}

	public ConstantsRepo getConstantsRepo() {
		return constantsRepo;
	}

}
