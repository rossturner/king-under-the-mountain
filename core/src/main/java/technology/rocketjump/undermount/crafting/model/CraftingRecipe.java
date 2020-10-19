package technology.rocketjump.undermount.crafting.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.misc.Name;

import java.util.List;

public class CraftingRecipe {

	@Name
	private String recipeName;
	private String verbOverrideI18nKey;

	private String craftingTypeName;
	@JsonIgnore
	private CraftingType craftingType;

	private String itemTypeRequiredName;
	@JsonIgnore
	private ItemType itemTypeRequired;

	private List<QuantifiedItemTypeWithMaterial> input;
	private List<QuantifiedItemTypeWithMaterial> output;

	private List<GameMaterialType> materialTypesToCopyOver;
	private Double extraGameHoursToComplete; // Only used in automated conversion process (for now)

	public String getRecipeName() {
		return recipeName;
	}

	public void setRecipeName(String name) {
		this.recipeName = name;
	}

	public String getVerbOverrideI18nKey() {
		return verbOverrideI18nKey;
	}

	public void setVerbOverrideI18nKey(String verbOverrideI18nKey) {
		this.verbOverrideI18nKey = verbOverrideI18nKey;
	}

	public String getCraftingTypeName() {
		return craftingTypeName;
	}

	public void setCraftingTypeName(String craftingTypeName) {
		this.craftingTypeName = craftingTypeName;
	}

	public CraftingType getCraftingType() {
		return craftingType;
	}

	public void setCraftingType(CraftingType craftingType) {
		this.craftingType = craftingType;
	}

	public List<QuantifiedItemTypeWithMaterial> getInput() {
		return input;
	}

	public void setInput(List<QuantifiedItemTypeWithMaterial> input) {
		this.input = input;
	}

	public List<QuantifiedItemTypeWithMaterial> getOutput() {
		return output;
	}

	public void setOutput(List<QuantifiedItemTypeWithMaterial> output) {
		this.output = output;
	}

	public List<GameMaterialType> getMaterialTypesToCopyOver() {
		return materialTypesToCopyOver;
	}

	public void setMaterialTypesToCopyOver(List<GameMaterialType> materialTypesToCopyOver) {
		this.materialTypesToCopyOver = materialTypesToCopyOver;
	}

	public String getItemTypeRequiredName() {
		return itemTypeRequiredName;
	}

	public void setItemTypeRequiredName(String itemTypeRequiredName) {
		this.itemTypeRequiredName = itemTypeRequiredName;
	}

	public ItemType getItemTypeRequired() {
		return itemTypeRequired;
	}

	public void setItemTypeRequired(ItemType itemTypeRequired) {
		this.itemTypeRequired = itemTypeRequired;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CraftingRecipe that = (CraftingRecipe) o;
		return recipeName.equals(that.recipeName);
	}

	@Override
	public int hashCode() {
		return recipeName.hashCode();
	}

	public Double getExtraGameHoursToComplete() {
		return extraGameHoursToComplete;
	}

	public void setExtraGameHoursToComplete(Double extraGameHoursToComplete) {
		this.extraGameHoursToComplete = extraGameHoursToComplete;
	}
}
