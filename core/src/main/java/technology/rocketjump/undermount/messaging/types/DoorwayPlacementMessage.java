package technology.rocketjump.undermount.messaging.types;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.doors.DoorwayOrientation;
import technology.rocketjump.undermount.doors.DoorwaySize;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class DoorwayPlacementMessage implements ChildPersistable {

	private DoorwaySize doorwaySize;
	private DoorwayOrientation orientation;
	private GameMaterial doorwayMaterial;
	private GridPoint2 tilePosition;

	public DoorwayPlacementMessage() {

	}

	public DoorwayPlacementMessage(DoorwaySize doorwaySize, DoorwayOrientation orientation,
								   GameMaterial doorwayMaterial, GridPoint2 tilePosition) {
		this.doorwaySize = doorwaySize;
		this.orientation = orientation;
		this.doorwayMaterial = doorwayMaterial;
		this.tilePosition = tilePosition;
	}

	public DoorwaySize getDoorwaySize() {
		return doorwaySize;
	}

	public DoorwayOrientation getOrientation() {
		return orientation;
	}

	public GameMaterial getDoorwayMaterial() {
		return doorwayMaterial;
	}

	public GridPoint2 getTilePosition() {
		return tilePosition;
	}

	public void setDoorwayMaterial(GameMaterial doorwayMaterial) {
		this.doorwayMaterial = doorwayMaterial;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (doorwaySize != null) {
			asJson.put("size", doorwaySize.name());
		}
		if (orientation != null) {
			asJson.put("orientation", orientation.name());
		}
		if (doorwayMaterial != null) {
			if (doorwayMaterial.equals(GameMaterial.NULL_MATERIAL)) {
				// Need to write material type instead, not a great solution
				asJson.put("materialType", doorwayMaterial.getMaterialType().name());
			} else {
				asJson.put("material", doorwayMaterial.getMaterialName());
			}
		}
		if (tilePosition != null) {
			asJson.put("position", JSONUtils.toJSON(tilePosition));
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.doorwaySize = EnumParser.getEnumValue(asJson, "size", DoorwaySize.class, null);
		this.orientation = EnumParser.getEnumValue(asJson, "orientation", DoorwayOrientation.class, null);
		String materialName = asJson.getString("material");
		if (materialName != null) {
			this.doorwayMaterial = relatedStores.gameMaterialDictionary.getByName(materialName);
			if (this.doorwayMaterial == null || this.doorwayMaterial.equals(GameMaterial.NULL_MATERIAL)) {
				throw new InvalidSaveException("Could not find material by name " + materialName);
			}
		} else {
			GameMaterialType materialType = EnumParser.getEnumValue(asJson, "materialType", GameMaterialType.class, null);
			if (materialType == null) {
				// This save must be from before material type was persisted
				throw new InvalidSaveException("No material or type specific in " + this.getClass().getSimpleName());
			}
			this.doorwayMaterial = GameMaterial.nullMaterialWithType(materialType);
		}

		this.tilePosition = JSONUtils.gridPoint2(asJson.getJSONObject("position"));
	}
}
