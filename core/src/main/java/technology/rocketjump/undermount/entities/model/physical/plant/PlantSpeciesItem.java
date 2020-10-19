package technology.rocketjump.undermount.entities.model.physical.plant;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.assets.entities.item.model.ItemSize;
import technology.rocketjump.undermount.assets.entities.item.model.ItemStyle;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class PlantSpeciesItem implements ChildPersistable {

	private String itemTypeName;
	private String materialName;
	private int quantity = 1;
	private ItemSize itemSize = ItemSize.AVERAGE;
	private ItemStyle itemStyle = ItemStyle.DEFAULT;

	@JsonIgnore
	private ItemType itemType;
	@JsonIgnore
	private GameMaterial material;

	public String getItemTypeName() {
		return itemTypeName;
	}

	public void setItemTypeName(String itemTypeName) {
		this.itemTypeName = itemTypeName;
	}

	public String getMaterialName() {
		return materialName;
	}

	public void setMaterialName(String materialName) {
		this.materialName = materialName;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public ItemSize getItemSize() {
		return itemSize;
	}

	public void setItemSize(ItemSize itemSize) {
		this.itemSize = itemSize;
	}

	public ItemStyle getItemStyle() {
		return itemStyle;
	}

	public void setItemStyle(ItemStyle itemStyle) {
		this.itemStyle = itemStyle;
	}

	public ItemType getItemType() {
		return itemType;
	}

	public void setItemType(ItemType itemType) {
		this.itemType = itemType;
	}

	public GameMaterial getMaterial() {
		return material;
	}

	public void setMaterial(GameMaterial material) {
		this.material = material;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("itemType", itemType.getItemTypeName());
		asJson.put("material", material.getMaterialName());
		asJson.put("quantity", quantity);
		if (!itemSize.equals(ItemSize.AVERAGE)) {
			asJson.put("size", itemSize.name());
		}
		if (!itemStyle.equals(ItemStyle.DEFAULT)) {
			asJson.put("style", itemStyle.name());
		}
	}
	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		itemTypeName = asJson.getString("itemType");
		itemType = relatedStores.itemTypeDictionary.getByName(itemTypeName);
		if (this.itemType == null) {
			throw new InvalidSaveException("Could not find item type by name " + itemTypeName + " in " + getClass().getSimpleName());
		}
		materialName = asJson.getString("material");
		material = relatedStores.gameMaterialDictionary.getByName(materialName);
		if (material == null) {
			throw new InvalidSaveException("Could not find material by name " + materialName + " in " + getClass().getSimpleName());
		}
		quantity = asJson.getIntValue("quantity");
		itemSize = EnumParser.getEnumValue(asJson, "size", ItemSize.class, ItemSize.AVERAGE);
		itemStyle = EnumParser.getEnumValue(asJson, "style", ItemStyle.class, ItemStyle.DEFAULT);
	}
}
