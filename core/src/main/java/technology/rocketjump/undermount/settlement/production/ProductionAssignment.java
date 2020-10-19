package technology.rocketjump.undermount.settlement.production;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.crafting.model.CraftingRecipe;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class ProductionAssignment implements Persistable {

	public long productionAssignmentId;
	public CraftingRecipe targetRecipe;
	private Entity assignedCraftingStation;

	public ProductionAssignment(CraftingRecipe targetRecipe, Entity assignedCraftingStation) {
		this.productionAssignmentId = SequentialIdGenerator.nextId();
		this.targetRecipe = targetRecipe;
		this.assignedCraftingStation = assignedCraftingStation;
	}

	public ProductionAssignment() {
	}

	public Entity getAssignedCraftingStation() {
		return assignedCraftingStation;
	}

	public void setAssignedCraftingStation(Entity assignedCraftingStation) {
		this.assignedCraftingStation = assignedCraftingStation;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.productionAssignments.containsKey(productionAssignmentId)) {
			return;
		}

		JSONObject asJson = new JSONObject(true);

		asJson.put("id", productionAssignmentId);
		asJson.put("targetRecipe", targetRecipe.getRecipeName());

		savedGameStateHolder.productionAssignments.put(productionAssignmentId, this);
		savedGameStateHolder.productionAssignmentsJson.add(asJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.productionAssignmentId = asJson.getLongValue("id");
		this.targetRecipe = relatedStores.craftingRecipeDictionary.getByName(asJson.getString("targetRecipe"));
		if (targetRecipe == null) {
			throw new InvalidSaveException("Could not find crafting recipe with name " + asJson.getString("targetRecipe"));
		}
		savedGameStateHolder.productionAssignments.put(productionAssignmentId, this);
	}
}
