package technology.rocketjump.undermount.rooms.constructions;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rooms.Bridge;

import java.util.*;

import static technology.rocketjump.undermount.rooms.constructions.ConstructionType.BRIDGE_CONSTRUCTION;

public class BridgeConstruction extends Construction {

	private Bridge bridge;

	public BridgeConstruction() {
	}

	public BridgeConstruction(Bridge bridge) {
		this.constructionId = SequentialIdGenerator.nextId();
		this.bridge = bridge;
		this.primaryMaterialType = bridge.getBridgeType().getMaterialType();
		QuantifiedItemType tileRequirement = bridge.getBridgeType().getBuildingRequirement();

		QuantifiedItemTypeWithMaterial constructionRequirements = new QuantifiedItemTypeWithMaterial();
		constructionRequirements.setItemType(tileRequirement.getItemType());
		constructionRequirements.setQuantity(tileRequirement.getQuantity() * bridge.getLocations().size());
		if (!bridge.getMaterial().equals(GameMaterial.NULL_MATERIAL)) {
			this.setPlayerSpecifiedPrimaryMaterial(Optional.of(bridge.getMaterial()));
		}

		this.requirements.add(constructionRequirements);
	}

	public Bridge getBridge() {
		return bridge;
	}

	@Override
	public Set<GridPoint2> getTileLocations() {
		return bridge.getLocations();
	}

	@Override
	public ConstructionType getConstructionType() {
		return BRIDGE_CONSTRUCTION;
	}

	@Override
	public Entity getEntity() {
		return null;
	}

	@Override
	public GridPoint2 getPrimaryLocation() {
		// Need to randomly pick to get either side
		List<GridPoint2> locations = new ArrayList<>(bridge.getLocations());
		Collections.shuffle(locations, new RandomXS128());
		return locations.get(0);
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.constructions.containsKey(getId())) {
			return;
		}
		super.writeTo(savedGameStateHolder);
		JSONObject asJson = savedGameStateHolder.constructionsJson.getJSONObject(savedGameStateHolder.constructionsJson.size() - 1);

		bridge.writeTo(savedGameStateHolder);
		asJson.put("bridge", bridge.getBridgeId());
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		long bridgeId = asJson.getLongValue("bridge");
		this.bridge = savedGameStateHolder.bridges.get(bridgeId);
		if (this.bridge == null) {
			throw new InvalidSaveException("Can not find bridge with ID " + bridgeId + " for " + getClass().getSimpleName());
		}
	}
}
