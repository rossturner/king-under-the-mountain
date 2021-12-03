package technology.rocketjump.undermount.entities.behaviour.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.entities.components.humanoid.MemoryComponent;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is used to track a group of Creatures such as a herd of deer, so they can move around a central point
 */
public class CreatureGroup implements Persistable {

	private static final double GAME_TIME_BETWEEN_UPDATES = 0.2;
	private long groupId;
	private GridPoint2 homeLocation;
	private double lastUpdateGameTime;

	private final MemoryComponent sharedMemoryComponent = new MemoryComponent();

	public long getGroupId() {
		return groupId;
	}

	public void setGroupId(long groupId) {
		this.groupId = groupId;
	}

	public GridPoint2 getHomeLocation() {
		return homeLocation;
	}

	public void setHomeLocation(GridPoint2 homeLocation) {
		this.homeLocation = homeLocation;
	}

	/**
	 * This is called by child entity infrequent updates so it is not accurately updated, but "every so often" is good enough
	 */
	public void infrequentUpdate(GameContext gameContext) {
		double now = gameContext.getGameClock().getCurrentGameTime();
		if (now - lastUpdateGameTime > GAME_TIME_BETWEEN_UPDATES) {
			lastUpdateGameTime = now;
			moveHomeLocation(gameContext);
		}
	}

	public MemoryComponent getSharedMemoryComponent() {
		return sharedMemoryComponent;
	}

	/**
	 * This moves the home location by one tile, orthogonally, randomly
	 */
	private void moveHomeLocation(GameContext gameContext) {
		List<CompassDirection> directions = new ArrayList<>(CompassDirection.CARDINAL_DIRECTIONS);
		Collections.shuffle(directions, gameContext.getRandom());

		for (CompassDirection direction : directions) {
			MapTile adjacentTile = gameContext.getAreaMap().getTile(homeLocation.x + direction.getXOffset(), homeLocation.y + direction.getYOffset());
			if (adjacentTile != null && adjacentTile.isNavigable(null) && !adjacentTile.hasDoorway()) {
				this.homeLocation = adjacentTile.getTilePosition();
				break;
			}
		}
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.creatureGroups.containsKey(groupId)) {
			return;
		}
		JSONObject asJson = new JSONObject(true);

		asJson.put("groupId", groupId);
		asJson.put("home", JSONUtils.toJSON(homeLocation));

		JSONObject memoryJson = new JSONObject(true);
		sharedMemoryComponent.writeTo(memoryJson, savedGameStateHolder);
		asJson.put("memories", memoryJson);

		savedGameStateHolder.creatureGroupJson.add(asJson);
		savedGameStateHolder.creatureGroups.put(groupId, this);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.groupId = asJson.getLong("groupId");
		this.homeLocation = JSONUtils.gridPoint2(asJson.getJSONObject("home"));

		JSONObject memoryJson = asJson.getJSONObject("memories");
		sharedMemoryComponent.readFrom(memoryJson, savedGameStateHolder, relatedStores);

		savedGameStateHolder.creatureGroups.put(groupId, this);
	}

}
