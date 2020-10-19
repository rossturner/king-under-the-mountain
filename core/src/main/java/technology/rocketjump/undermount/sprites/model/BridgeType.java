package technology.rocketjump.undermount.sprites.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.misc.Name;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BridgeType {

	@Name
	private GameMaterialType materialType;
	private QuantifiedItemType buildingRequirement;
	private Map<BridgeOrientation, Map<BridgeTileLayout, String>> assets = new HashMap<>();
	private String i18nKey;
	private String craftingTypeName; // Informs us which profession and tool is needed to construct the wall
	@JsonIgnore
	private CraftingType craftingType;

	public GameMaterialType getMaterialType() {
		return materialType;
	}

	public void setMaterialType(GameMaterialType materialType) {
		this.materialType = materialType;
	}

	public QuantifiedItemType getBuildingRequirement() {
		return buildingRequirement;
	}

	public void setBuildingRequirement(QuantifiedItemType buildingRequirement) {
		this.buildingRequirement = buildingRequirement;
	}

	public Map<BridgeOrientation, Map<BridgeTileLayout, String>> getAssets() {
		return assets;
	}

	public void setAssets(Map<BridgeOrientation, Map<BridgeTileLayout, String>> assets) {
		this.assets = assets;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public void setI18nKey(String i18nKey) {
		this.i18nKey = i18nKey;
	}

	public String getCraftingTypeName() {
		return craftingTypeName;
	}

	public CraftingType getCraftingType() {
		return craftingType;
	}

	public void setCraftingType(CraftingType craftingType) {
		this.craftingType = craftingType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BridgeType that = (BridgeType) o;
		return materialType == that.materialType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(materialType);
	}

}
