package technology.rocketjump.undermount.entities.behaviour.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

/**
 * This is used to track a group of Creatures such as a herd of deer, so they can move around a central point
 */
public class CreatureGroup implements Persistable {

	private long groupId;
	private GridPoint2 homeLocation;

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

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.creatureGroups.containsKey(groupId)) {
			return;
		}
		JSONObject asJson = new JSONObject(true);

		asJson.put("groupId", groupId);
		asJson.put("home", JSONUtils.toJSON(homeLocation));

		savedGameStateHolder.creatureGroupJson.add(asJson);
		savedGameStateHolder.creatureGroups.put(groupId, this);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.groupId = asJson.getLong("groupId");
		this.homeLocation = JSONUtils.gridPoint2(asJson.getJSONObject("home"));

		savedGameStateHolder.creatureGroups.put(groupId, this);
	}

}
