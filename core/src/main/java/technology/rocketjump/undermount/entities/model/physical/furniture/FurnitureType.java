package technology.rocketjump.undermount.entities.model.physical.furniture;

import com.badlogic.gdx.graphics.Color;
import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.undermount.entities.tags.Tag;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.misc.Name;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.rooms.RoomType;

import java.util.*;

/**
 * A furniture type is a sub-grouping of FurnitureCategory
 * This may represent the difference between a carpenter's table and a mason's table
 */
public class FurnitureType {

	@Name
	private String name;
	private String i18nKey;
	private String categoryName;
	@JsonIgnore
	private FurnitureCategory furnitureCategory;

	private String defaultLayoutName;
	@JsonIgnore
	private FurnitureLayout defaultLayout;

	private String colorCode;
	@JsonIgnore
	private Color color;

	private String iconName;

	private boolean placeAnywhere; // Not restricted by room type
	@JsonIgnore
	private final Set<RoomType> validRoomTypes = new HashSet<>(); // Only set by code on load

	private boolean autoConstructed; // Is automatically built as soon as all requirements are in place

	// This is the list of items (with quantities) needed to build the type for each listed GameMaterialType
	private Map<GameMaterialType, List<QuantifiedItemType>> requirements;

	private Map<String, List<String>> tags = new HashMap<>();
	@JsonIgnore
	private List<Tag> processedTags = new ArrayList<>();

	private GameMaterialType requiredFloorMaterialType;

	private boolean hiddenFromPlacementMenu = false; // For furniture types that can't be placed by the player

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public void setI18nKey(String i18nKey) {
		this.i18nKey = i18nKey;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public FurnitureCategory getFurnitureCategory() {
		return furnitureCategory;
	}

	public void setFurnitureCategory(FurnitureCategory furnitureCategory) {
		this.furnitureCategory = furnitureCategory;
	}

	public String getDefaultLayoutName() {
		return defaultLayoutName;
	}

	public void setDefaultLayoutName(String defaultLayoutName) {
		this.defaultLayoutName = defaultLayoutName;
	}

	public FurnitureLayout getDefaultLayout() {
		return defaultLayout;
	}

	public void setDefaultLayout(FurnitureLayout defaultLayout) {
		this.defaultLayout = defaultLayout;
	}

	public boolean isAutoConstructed() {
		return autoConstructed;
	}

	public void setAutoConstructed(boolean autoConstructed) {
		this.autoConstructed = autoConstructed;
	}

	public Map<GameMaterialType, List<QuantifiedItemType>> getRequirements() {
		return requirements;
	}

	public void setRequirements(Map<GameMaterialType, List<QuantifiedItemType>> requirements) {
		this.requirements = requirements;
	}

	public String getColorCode() {
		return colorCode;
	}

	public void setColorCode(String colorCode) {
		this.colorCode = colorCode;
		setColor(HexColors.get(colorCode));
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public String getIconName() {
		return iconName;
	}

	public void setIconName(String iconName) {
		this.iconName = iconName;
	}

	@Override
	public String toString() {
		return "FurnitureType{" +
				"name='" + name + '\'' +
				", categoryName='" + categoryName + '\'' +
				", furnitureCategory=" + furnitureCategory +
				'}';
	}

	public boolean isPlaceAnywhere() {
		return placeAnywhere;
	}

	public void setPlaceAnywhere(boolean placeAnywhere) {
		this.placeAnywhere = placeAnywhere;
	}

	public Set<RoomType> getValidRoomTypes() {
		return validRoomTypes;
	}

	public Map<String, List<String>> getTags() {
		return tags;
	}

	public void setTags(Map<String, List<String>> tags) {
		this.tags = tags;
	}

	public void setProcessedTags(List<Tag> processedTags) {
		this.processedTags = processedTags;
	}

	public List<Tag> getProcessedTags() {
		return processedTags;
	}

	public GameMaterialType getRequiredFloorMaterialType() {
		return requiredFloorMaterialType;
	}

	public void setRequiredFloorMaterialType(GameMaterialType requiredFloorMaterialType) {
		this.requiredFloorMaterialType = requiredFloorMaterialType;
	}

	public boolean isHiddenFromPlacementMenu() {
		return hiddenFromPlacementMenu;
	}

	public void setHiddenFromPlacementMenu(boolean hiddenFromPlacementMenu) {
		this.hiddenFromPlacementMenu = hiddenFromPlacementMenu;
	}
}
