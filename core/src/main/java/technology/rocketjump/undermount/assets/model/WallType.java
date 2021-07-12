package technology.rocketjump.undermount.assets.model;

import com.fasterxml.jackson.annotation.*;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.misc.Name;
import technology.rocketjump.undermount.misc.SequentialId;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WallType {

	@Name
	private final String wallTypeName;
	private final String i18nKey;
	@SequentialId
	private long wallTypeId;
	private final GameMaterialType materialType;
	private final boolean useMaterialColor;

	private final String overlayWallTypeName;
	@JsonIgnore
	private WallType overlayWallType;

	private final String craftingTypeName; // Informs us which profession and tool is needed to construct the wall
	@JsonIgnore
	private CraftingType craftingType;
	// This is the list of items (with quantities) needed to build the type for each listed GameMaterialType
	private Map<GameMaterialType, List<QuantifiedItemType>> requirements;

	@JsonCreator
	public WallType(@JsonProperty("wallTypeName") String wallTypeName,
					@JsonProperty("getI18nKey") String i18nKey,
					@JsonProperty("wallTypeId") long wallTypeId,
					@JsonProperty("materialType") GameMaterialType materialType,
					@JsonProperty("useMaterialColor") boolean useMaterialColor,
					@JsonProperty("overlayWallTypeName") String overlayWallTypeName,
					@JsonProperty("craftingTypeName") String craftingTypeName,
					@JsonProperty("requirements") Map<GameMaterialType, List<QuantifiedItemType>> requirements) {
		this.wallTypeName = wallTypeName;
		this.i18nKey = i18nKey;
		this.wallTypeId = wallTypeId;
		this.materialType = materialType;
		this.useMaterialColor = useMaterialColor;
		this.overlayWallTypeName = overlayWallTypeName;
		this.craftingTypeName = craftingTypeName;
		this.requirements = requirements;
	}

	public String getWallTypeName() {
		return wallTypeName;
	}

	public long getWallTypeId() {
		return wallTypeId;
	}

	public GameMaterialType getMaterialType() {
		return materialType;
	}

	public boolean isUseMaterialColor() {
		return useMaterialColor;
	}

	public void setWallTypeId(long id) {
		this.wallTypeId = id;
	}

	@Override
	public String toString() {
		return wallTypeName + ", wallTypeId=" + wallTypeId + ", materialType=" + materialType;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public boolean isConstructed() {
		return craftingType != null && requirements != null;
	}

	public Map<GameMaterialType, List<QuantifiedItemType>> getRequirements() {
		return requirements;
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

	public String getOverlayWallTypeName() {
		return overlayWallTypeName;
	}

	public WallType getOverlayWallType() {
		return overlayWallType;
	}

	public void setOverlayWallType(WallType overlayWallType) {
		this.overlayWallType = overlayWallType;
	}
}
