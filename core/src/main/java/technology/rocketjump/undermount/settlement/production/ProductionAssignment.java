package technology.rocketjump.undermount.settlement.production;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.crafting.model.CraftingRecipe;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;

public class ProductionAssignment implements Persistable {

	public long productionAssignmentId;
	public CraftingRecipe targetRecipe;
	public final List<QuantifiedItemTypeWithMaterial> inputMaterialSelections = new ArrayList<>();
	private Entity assignedCraftingStation;
	private double inputSelectionsLastUpdated;

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

	public List<QuantifiedItemTypeWithMaterial> getInputMaterialSelections() {
		return inputMaterialSelections;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.productionAssignments.containsKey(productionAssignmentId)) {
			return;
		}

		JSONObject asJson = new JSONObject(true);

		asJson.put("id", productionAssignmentId);
		asJson.put("targetRecipe", targetRecipe.getRecipeName());
		if (!inputMaterialSelections.isEmpty()) {
			JSONArray materialSelectionsJson = new JSONArray();
			for (QuantifiedItemTypeWithMaterial inputMaterialSelection : inputMaterialSelections) {
				JSONObject selectionAsJson = new JSONObject(true);
				inputMaterialSelection.writeTo(selectionAsJson, savedGameStateHolder);
				materialSelectionsJson.add(selectionAsJson);
			}
			asJson.put("inputMaterials", materialSelectionsJson);
		}
		asJson.put("lastUpdated", inputSelectionsLastUpdated);

		savedGameStateHolder.productionAssignments.put(productionAssignmentId, this);
		savedGameStateHolder.productionAssignmentsJson.add(asJson);
	}

	public void setInputSelectionsLastUpdated(double inputSelectionsLastUpdated) {
		this.inputSelectionsLastUpdated = inputSelectionsLastUpdated;
	}

	public double getInputSelectionsLastUpdated() {
		return inputSelectionsLastUpdated;
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.productionAssignmentId = asJson.getLongValue("id");
		this.targetRecipe = relatedStores.craftingRecipeDictionary.getByName(asJson.getString("targetRecipe"));
		if (targetRecipe == null) {
			throw new InvalidSaveException("Could not find crafting recipe with name " + asJson.getString("targetRecipe"));
		}

		JSONArray materialSelectionsJson = asJson.getJSONArray("inputMaterials");
		if (materialSelectionsJson != null) {
			for (Object o : materialSelectionsJson) {
				JSONObject selectionJson = (JSONObject) o;
				QuantifiedItemTypeWithMaterial selection = new QuantifiedItemTypeWithMaterial();
				selection.readFrom(selectionJson, savedGameStateHolder, relatedStores);
				this.inputMaterialSelections.add(selection);
			}
		}
		this.inputSelectionsLastUpdated = asJson.getDoubleValue("lastUpdated");

		savedGameStateHolder.productionAssignments.put(productionAssignmentId, this);
	}

}
