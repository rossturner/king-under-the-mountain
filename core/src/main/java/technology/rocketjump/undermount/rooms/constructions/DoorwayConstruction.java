package technology.rocketjump.undermount.rooms.constructions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.messaging.types.DoorwayPlacementMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class DoorwayConstruction extends FurnitureConstruction {

	private DoorwayPlacementMessage placeDoorwayMessage;

	public DoorwayConstruction() {
		super();
	}

	public DoorwayConstruction(Entity furnitureEntityToBePlaced, DoorwayPlacementMessage placeDoorwayMessage) {
		super(furnitureEntityToBePlaced);
		this.placeDoorwayMessage = placeDoorwayMessage;
	}

	@Override
	public ConstructionType getConstructionType() {
		return ConstructionType.DOORWAY_CONSTRUCTION;
	}

	public DoorwayPlacementMessage getPlaceDoorwayMessage() {
		return placeDoorwayMessage;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.constructions.containsKey(getId())) {
			return;
		}
		super.writeTo(savedGameStateHolder);

		if (placeDoorwayMessage != null) {
			JSONObject asJson = savedGameStateHolder.constructionsJson.getJSONObject(savedGameStateHolder.constructionsJson.size() - 1);
			JSONObject messageJson = new JSONObject(true);
			placeDoorwayMessage.writeTo(messageJson, savedGameStateHolder);
			asJson.put("placeDoorwayMessage", messageJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		JSONObject messageJson = asJson.getJSONObject("placeDoorwayMessage");
		if (messageJson != null) {
			this.placeDoorwayMessage = new DoorwayPlacementMessage();
			this.placeDoorwayMessage.readFrom(messageJson, savedGameStateHolder, relatedStores);
		}
	}
}
