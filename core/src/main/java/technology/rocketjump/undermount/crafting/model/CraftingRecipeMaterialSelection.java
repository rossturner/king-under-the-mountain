package technology.rocketjump.undermount.crafting.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;

/**
 * This class is set by the player and persisted per-game to specify materials to be used in crafting
 */
public class CraftingRecipeMaterialSelection implements ChildPersistable {

	private CraftingRecipe parent;

	private List<Optional<GameMaterial>> materialOverrides = new ArrayList<>(); // This is to be used as an index-based replacement for crafting recipe input requirements

	public CraftingRecipeMaterialSelection() {

	}

	public CraftingRecipeMaterialSelection(CraftingRecipe parent) {
		this.parent = parent;
		for (QuantifiedItemTypeWithMaterial inputRequirement : parent.getInput()) {
			if (inputRequirement.getMaterial() == null) {
				materialOverrides.add(Optional.empty());
			} else {
				materialOverrides.add(Optional.of(inputRequirement.getMaterial()));
			}
		}
	}

	public Optional<GameMaterial> getSelection(QuantifiedItemTypeWithMaterial inputRequirement) {
		for (int index = 0; index <= parent.getInput().size(); index++) {
			if (inputRequirement.equals(parent.getInput().get(index))) {
				return materialOverrides.get(index);
			}
		}
		Logger.error("Did not match on input requirement");
		return Optional.empty();
	}

	public void setSelection(QuantifiedItemTypeWithMaterial inputRequirement, GameMaterial value) {
		for (int index = 0; index <= parent.getInput().size(); index++) {
			if (inputRequirement.equals(parent.getInput().get(index))) {
				materialOverrides.remove(index);
				if (value.equals(NULL_MATERIAL)) {
					materialOverrides.add(index, Optional.empty());
				} else {
					materialOverrides.add(index, Optional.of(value));
				}
				return;
			}
		}
		Logger.error("Did not match on input requirement");
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("parent", parent.getRecipeName());

		JSONArray selectionsJson = new JSONArray();
		for (Optional<GameMaterial> materialOverride : materialOverrides) {
			if (materialOverride.isPresent()) {
				selectionsJson.add(materialOverride.get().getMaterialName());
			} else {
				selectionsJson.add(NULL_MATERIAL.getMaterialName());
			}
		}
		asJson.put("selections", selectionsJson);

	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.parent = relatedStores.craftingRecipeDictionary.getByName(asJson.getString("parent"));
		if (this.parent == null) {
			throw new InvalidSaveException("Could not find crafting recipe with name " + asJson.getString("parent"));
		}

		JSONArray selectionsJson = asJson.getJSONArray("selections");
		if (selectionsJson == null) {
			throw new InvalidSaveException("Could not find expected selections array on " + asJson.toString());
		}
		for (Object o : selectionsJson) {
			if (o.toString().equals(NULL_MATERIAL.getMaterialName())) {
				this.materialOverrides.add(Optional.empty());
			} else {
				GameMaterial material = relatedStores.gameMaterialDictionary.getByName(o.toString());
				if (material == null) {
					throw new InvalidSaveException("Could not find material with name " + o.toString());
				} else {
					this.materialOverrides.add(Optional.of(material));
				}
			}
		}


	}
}
