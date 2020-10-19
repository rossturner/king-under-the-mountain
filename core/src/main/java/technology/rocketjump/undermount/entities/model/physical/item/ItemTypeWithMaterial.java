package technology.rocketjump.undermount.entities.model.physical.item;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemTypeWithMaterial implements ChildPersistable {

	private String itemTypeName;
	@JsonIgnore
	private ItemType itemType;

	private String materialName;
	@JsonIgnore
	private GameMaterial material;

	public static List<ItemTypeWithMaterial> convert(List<QuantifiedItemType> withoutMaterial) {
		List<ItemTypeWithMaterial> withMaterialList = new ArrayList<>(withoutMaterial.size());
		for (QuantifiedItemType quantifiedItemType : withoutMaterial) {
			ItemTypeWithMaterial withMaterial = new ItemTypeWithMaterial();
			withMaterial.setItemTypeName(quantifiedItemType.getItemTypeName());
			withMaterial.setItemType(quantifiedItemType.getItemType());

			withMaterialList.add(withMaterial);
		}
		return withMaterialList;
	}


	public GameMaterial getMaterial() {
		return material;
	}

	public void setMaterial(GameMaterial material) {
		this.material = material;
	}

	public String getMaterialName() {
		return materialName;
	}

	public void setMaterialName(String materialName) {
		this.materialName = materialName;
	}

	public String getItemTypeName() {
		return itemTypeName;
	}

	public void setItemTypeName(String itemTypeName) {
		this.itemTypeName = itemTypeName;
	}

	public ItemType getItemType() {
		return itemType;
	}

	public void setItemType(ItemType itemType) {
		this.itemType = itemType;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (itemType != null) {
			sb.append(itemType.getItemTypeName());
		} else {
			sb.append(itemTypeName);
		}
		if (material != null) {
			sb.append(" (").append(material.toString()).append(") ");
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ItemTypeWithMaterial that = (ItemTypeWithMaterial) o;
		return Objects.equals(itemType, that.itemType) &&
				Objects.equals(material, that.material);
	}

	@Override
	public int hashCode() {
		return Objects.hash(itemType, material);
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("itemType", itemType.getItemTypeName());
		asJson.put("material", material.getMaterialName());
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.itemTypeName = asJson.getString("itemType");
		this.itemType = relatedStores.itemTypeDictionary.getByName(itemTypeName);
		if (itemType == null) {
			throw new InvalidSaveException("Could not find item type with name " + itemTypeName);
		}

		this.materialName = asJson.getString("material");
		this.material = relatedStores.gameMaterialDictionary.getByName(materialName);
		if (this.material == null) {
			throw new InvalidSaveException("Could not find material with name " + materialName);
		}
	}
}
