package technology.rocketjump.undermount.mapping.tile.roof;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;

public class TileRoof implements ChildPersistable {

	private TileRoofState state = TileRoofState.MOUNTAIN_ROOF;
	private GameMaterial roofMaterial = NULL_MATERIAL;
	private boolean roofConstructionQueued = false;

	public TileRoof() {

	}

	public TileRoof(TileRoofState roofState, GameMaterial material) {
		this.state = roofState;
		this.roofMaterial = material;
	}

	public TileRoofState getState() {
		return state;
	}

	public void setState(TileRoofState state) {
		this.state = state;
	}

	public GameMaterial getRoofMaterial() {
		return roofMaterial;
	}

	public void setRoofMaterial(GameMaterial roofMaterial) {
		this.roofMaterial = roofMaterial;
	}

	public boolean isRoofConstructionQueued() {
		return roofConstructionQueued;
	}

	public void setRoofConstructionQueued(boolean roofConstructionQueued) {
		this.roofConstructionQueued = roofConstructionQueued;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {


		if (!state.equals(TileRoofState.MOUNTAIN_ROOF)) {
			asJson.put("state", state.name());
		}
		if (!roofMaterial.equals(NULL_MATERIAL)) {
			asJson.put("roofMaterial", roofMaterial.getMaterialName());
		}
		if (roofConstructionQueued) {
			asJson.put("constructionQueued", true);
		}

	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.state = EnumParser.getEnumValue(asJson, "state", TileRoofState.class, TileRoofState.MOUNTAIN_ROOF);

		String roofMaterialName = asJson.getString("roofMaterial");
		if (roofMaterialName != null) {
			this.roofMaterial = relatedStores.gameMaterialDictionary.getByName(roofMaterialName);
			if (this.roofMaterial == null) {
				throw new InvalidSaveException("Could not find material with name " + roofMaterialName);
			}
		}

		this.roofConstructionQueued = asJson.getBooleanValue("constructionQueued");
	}
}
