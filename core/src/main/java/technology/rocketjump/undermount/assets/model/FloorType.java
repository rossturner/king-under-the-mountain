package technology.rocketjump.undermount.assets.model;

import com.badlogic.gdx.graphics.Color;
import com.fasterxml.jackson.annotation.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.mapping.tile.floor.FloorOverlap;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.misc.Name;
import technology.rocketjump.undermount.misc.SequentialId;
import technology.rocketjump.undermount.rendering.utils.HexColors;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FloorType {

	@Name
	private final String floorTypeName;
	private final String i18nKey;
	@SequentialId
	private long floorTypeId;
	private final GameMaterialType materialType;
	private final int layer;
	private final int numSprites;
	private OverlapType overlapType;
	private final boolean useMaterialColor; // If false, low and high color codes should be set
	private float speedModifier = 1.0f;
	private final String craftingTypeName; // Informs us which profession and tool is needed to construct the wall
	@JsonIgnore
	private CraftingType craftingType;
	// This is the list of items (with quantities) needed to build the type for each listed GameMaterialType
	private Map<GameMaterialType, List<QuantifiedItemType>> requirements;

	private String lowColorCode;
	@JsonIgnore
	private Color lowColor;
	private String highColorCode;
	@JsonIgnore
	private Color highColor;

	public static FloorType NULL_FLOOR = new FloorType("NULL_FLOOR", "NULL_FLOOR", -1L, GameMaterialType.OTHER, -1, 0,
			null, false, 1f, null, null, "000000", "ffffff");

	@JsonCreator
	public FloorType(@JsonProperty("floorTypeName") String floorTypeName,
					 @JsonProperty("i18nKey") String i18nKey,
					 @JsonProperty("floorTypeId") long floorTypeId,
					 @JsonProperty("materialType") GameMaterialType materialType, @JsonProperty("layer") int layer,
					 @JsonProperty("numSprites") int numSprites, @JsonProperty("overlapType") OverlapType overlapType,
					 @JsonProperty("useMaterialColor") boolean useMaterialColor,
					 @JsonProperty("speedModifier") float speedModifier,
					 @JsonProperty("craftingTypeName") String craftingTypeName,
					 @JsonProperty("requirements") Map<GameMaterialType, List<QuantifiedItemType>> requirements,
					 @JsonProperty("lowColorCode") String lowColorCode, @JsonProperty("highColorCode") String highColorCode) {
		this.floorTypeName = floorTypeName;
		this.i18nKey = i18nKey;
		this.floorTypeId = floorTypeId;
		this.materialType = materialType;
		this.layer = layer;
		this.numSprites = numSprites;
		this.overlapType = overlapType;
		this.useMaterialColor = useMaterialColor;
		this.speedModifier = speedModifier;
		this.craftingTypeName = craftingTypeName;
		this.requirements = requirements;

		this.lowColorCode = lowColorCode;
		if (lowColorCode != null) {
			lowColor = HexColors.get(lowColorCode);
		}
		this.highColorCode = highColorCode;
		if (highColorCode != null) {
			highColor = HexColors.get(highColorCode);
		}
	}

	public boolean isConstructed() {
		return craftingType != null && requirements != null;
	}

	public String getFloorTypeName() {
		return floorTypeName;
	}

	public GameMaterialType getMaterialType() {
		return materialType;
	}

	public int getLayer() {
		return layer;
	}

	public int getNumSprites() {
		return numSprites;
	}

	public OverlapType getOverlapType() {
		return overlapType;
	}

	public boolean isUseMaterialColor() {
		return useMaterialColor;
	}

	public void setOverlapType(OverlapType overlapType) {
		this.overlapType = overlapType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FloorType that = (FloorType) o;
		return new EqualsBuilder()
				.append(layer, that.layer)
				.append(materialType, that.materialType)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(materialType)
				.append(layer)
				.toHashCode();
	}

	public long getFloorTypeId() {
		return floorTypeId;
	}

	public void setFloorTypeId(long floorTypeId) {
		this.floorTypeId = floorTypeId;
	}

	public String getLowColorCode() {
		return lowColorCode;
	}

	public void setLowColorCode(String lowColorCode) {
		this.lowColorCode = lowColorCode;
	}

	public Color getLowColor() {
		return lowColor;
	}

	public void setLowColor(Color lowColor) {
		this.lowColor = lowColor;
	}

	public String getHighColorCode() {
		return highColorCode;
	}

	public void setHighColorCode(String highColorCode) {
		this.highColorCode = highColorCode;
	}

	public Color getHighColor() {
		return highColor;
	}

	public void setHighColor(Color highColor) {
		this.highColor = highColor;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public Color getColorForHeightValue(float heightmapValue) {
		if (lowColor == null || highColor == null) {
			return Color.TEAL; // Debug color to highlight that this should be set up in json file
		}
		float cursor; // point between low color and high color
		if (heightmapValue < 0.5) {
			cursor = 1 - (heightmapValue * 2);
		} else {
			cursor  = (heightmapValue - 0.5f) * 2;
		}

		return lowColor.cpy().lerp(highColor, cursor);
//
//		float red = (lowColor.r * cursor) + (highColor.r * (1 - cursor));
//		float green = (lowColor.g * cursor) + (highColor.g * (1 - cursor));
//		float blue = (lowColor.b * cursor) + (highColor.b * (1 - cursor));
//
//		return new Color(red, green, blue, 1f);
	}

	public float getSpeedModifier() {
		return speedModifier;
	}

	public void setSpeedModifier(float speedModifier) {
		this.speedModifier = speedModifier;
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

	public Map<GameMaterialType, List<QuantifiedItemType>> getRequirements() {
		return requirements;
	}

	public static class FloorDefinitionComparator implements Comparator<FloorOverlap> {
		@Override
		public int compare(FloorOverlap o1, FloorOverlap o2) {
			return o1.getFloorType().getLayer() - o2.getFloorType().getLayer();
		}

	}
}
