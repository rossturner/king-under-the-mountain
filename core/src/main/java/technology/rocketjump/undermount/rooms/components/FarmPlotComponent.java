package technology.rocketjump.undermount.rooms.components;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rooms.Room;

public class FarmPlotComponent extends RoomComponent {

	private FloorType farmingFloorType;
	private GameMaterial farmingFloorMaterial;
	private PlantSpecies selectedCrop;

	public FarmPlotComponent(Room parent, MessageDispatcher messageDispatcher) {
		super(parent, messageDispatcher);
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {

	}

	@Override
	public RoomComponent clone(Room newParent) {
		FarmPlotComponent cloned = new FarmPlotComponent(newParent, messageDispatcher);
		cloned.farmingFloorType = this.farmingFloorType;
		cloned.farmingFloorMaterial = this.farmingFloorMaterial;
		cloned.selectedCrop = this.selectedCrop;
		return cloned;
	}

	@Override
	public void mergeFrom(RoomComponent otherComponent) {
		FarmPlotComponent other = (FarmPlotComponent)otherComponent;
		if (this.farmingFloorType == null) {
			this.farmingFloorType = other.farmingFloorType;
		}
		if (this.farmingFloorMaterial == null) {
			this.farmingFloorMaterial = other.farmingFloorMaterial;
		}
		if (this.selectedCrop == null) {
			this.selectedCrop = other.selectedCrop;
		}
	}

	@Override
	public void tileRemoved(GridPoint2 location) {
		// Do nothing, FarmPlotBehaviour deals with removing jobs
	}

	public FloorType getFarmingFloorType() {
		return farmingFloorType;
	}

	public void setFarmingFloorType(FloorType farmingFloorType) {
		this.farmingFloorType = farmingFloorType;
	}

	public PlantSpecies getSelectedCrop() {
		return selectedCrop;
	}

	public void setSelectedCrop(PlantSpecies selectedCrop) {
		this.selectedCrop = selectedCrop;
	}

	public GameMaterial getFarmingFloorMaterial() {
		return farmingFloorMaterial;
	}

	public void setFarmingFloorMaterial(GameMaterial farmingFloorMaterial) {
		this.farmingFloorMaterial = farmingFloorMaterial;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (farmingFloorType != null) {
			asJson.put("floorType", farmingFloorType.getFloorTypeName());
		}
		if (farmingFloorMaterial != null) {
			asJson.put("material", farmingFloorMaterial.getMaterialName());
		}
		if (selectedCrop != null) {
			asJson.put("crop", selectedCrop.getSpeciesName());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		String floorTypeName = asJson.getString("floorType");
		if (floorTypeName != null) {
			this.farmingFloorType = relatedStores.floorTypeDictionary.getByFloorTypeName(floorTypeName);
			if (this.farmingFloorType == null) {
				throw new InvalidSaveException("Could not find floor type by name " + floorTypeName);
			}
		}

		String materialName = asJson.getString("material");
		if (materialName != null) {
			this.farmingFloorMaterial = relatedStores.gameMaterialDictionary.getByName(materialName);
			if (this.farmingFloorMaterial == null) {
				throw new InvalidSaveException("Could not find material by name " + materialName);
			}
		}

		String cropName = asJson.getString("crop");
		if (cropName != null) {
			this.selectedCrop = relatedStores.plantSpeciesDictionary.getByName(cropName);
			if (this.selectedCrop == null) {
				throw new InvalidSaveException("Could not find plant species by name " + cropName);
			}
		}
	}
}
