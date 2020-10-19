package technology.rocketjump.undermount.rooms;

import com.badlogic.gdx.graphics.Color;
import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.entities.tags.Tag;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.misc.Name;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.ui.i18n.I18nText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomType {

	@Name
	private String roomName;
	private String i18nKey;
	private I18nText i18nValue;
	private String colorCode;
	@JsonIgnore
	private Color color;
	private String iconName;
	private GameMaterialType requiredFloorMaterialType;
	private List<String> furnitureNames = new ArrayList<>();
	private String edgeName = "soft";
	private Map<String, List<String>> tags = new HashMap<>();
	@JsonIgnore
	private List<Tag> processedTags = new ArrayList<>();

	public String getRoomName() {
		return roomName;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public void setI18nKey(String i18nKey) {
		this.i18nKey = i18nKey;
	}

	public I18nText getI18nValue() {
		return i18nValue;
	}

	public void setI18nValue(I18nText i18nValue) {
		this.i18nValue = i18nValue;
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

	public List<String> getFurnitureNames() {
		return furnitureNames;
	}

	public void setFurnitureNames(List<String> furnitureNames) {
		this.furnitureNames = furnitureNames;
	}

	public String getEdgeName() {
		return edgeName;
	}

	public void setEdgeName(String edgeName) {
		this.edgeName = edgeName;
	}

	public GameMaterialType getRequiredFloorMaterialType() {
		return requiredFloorMaterialType;
	}

	public void setRequiredFloorMaterialType(GameMaterialType requiredFloorMaterialType) {
		this.requiredFloorMaterialType = requiredFloorMaterialType;
	}

	public Map<String, List<String>> getTags() {
		return tags;
	}

	public void setTags(Map<String, List<String>> tags) {
		this.tags = tags;
	}

	public List<Tag> getProcessedTags() {
		return processedTags;
	}

	public void setProcessedTags(List<Tag> processedTags) {
		this.processedTags = processedTags;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RoomType roomType = (RoomType) o;
		return roomName.equals(roomType.roomName);
	}

	@Override
	public int hashCode() {
		return roomName.hashCode();
	}

	@Override
	public String toString() {
		return roomName;
	}
}
