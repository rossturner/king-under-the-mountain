package technology.rocketjump.undermount.entities.model.physical.item;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static technology.rocketjump.undermount.entities.model.EntityType.ITEM;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class QuantifiedItemTypeWithMaterial implements ChildPersistable {

	private String itemTypeName;
	@JsonIgnore
	private ItemType itemType;

	private int quantity;

	private String materialName;
	@JsonIgnore
	private GameMaterial material;

	private boolean liquid = false;

	public QuantifiedItemTypeWithMaterial clone() {
		QuantifiedItemTypeWithMaterial clone = new QuantifiedItemTypeWithMaterial();
		clone.itemTypeName = this.itemTypeName;
		clone.itemType = this.itemType;
		clone.quantity = this.quantity;
		clone.materialName = this.materialName;
		clone.material = this.material;
		clone.liquid = this.liquid;
		return clone;
	}

	public static List<QuantifiedItemTypeWithMaterial> convert(List<QuantifiedItemType> withoutMaterial) {
		List<QuantifiedItemTypeWithMaterial> withMaterialList = new ArrayList<>(withoutMaterial.size());
		for (QuantifiedItemType quantifiedItemType : withoutMaterial) {
			QuantifiedItemTypeWithMaterial withMaterial = new QuantifiedItemTypeWithMaterial();
			withMaterial.setItemTypeName(quantifiedItemType.getItemTypeName());
			withMaterial.setItemType(quantifiedItemType.getItemType());
			withMaterial.setQuantity(quantifiedItemType.getQuantity());
			withMaterial.setLiquid(quantifiedItemType.isLiquid());

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

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public boolean isLiquid() {
		return liquid;
	}

	public void setLiquid(boolean liquid) {
		this.liquid = liquid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		QuantifiedItemTypeWithMaterial that = (QuantifiedItemTypeWithMaterial) o;
		return quantity == that.quantity &&
				liquid == that.liquid &&
				Objects.equals(itemType, that.itemType) &&
				Objects.equals(material, that.material);
	}

	@Override
	public int hashCode() {
		return Objects.hash(itemType, quantity, material, liquid);
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
		sb.append(" x").append(quantity);
		return sb.toString();
	}

	public boolean matches(Entity entity) {
		if (entity.getType().equals(ITEM)) {
			ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
			return (this.itemType == null || attributes.getItemType().equals(this.itemType)) &&
					(!this.liquid) &&
					(this.material == null || attributes.getMaterial(this.material.getMaterialType()).equals(this.material));
		} else {
			return false;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (itemType != null) {
			asJson.put("itemType", itemType.getItemTypeName());
		}
		if (quantity != 0) {
			asJson.put("quantity", quantity);
		}
		if (material != null) {
			asJson.put("material", material.getMaterialName());
			if (material.equals(GameMaterial.NULL_MATERIAL)) {
				asJson.put("materialType", material.getMaterialType().name());
			}
		}
		if (liquid) {
			asJson.put("liquid", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		String itemTypeName = asJson.getString("itemType");
		if (itemTypeName != null) {
			this.itemType = relatedStores.itemTypeDictionary.getByName(itemTypeName);
			if (this.itemType == null) {
				throw new InvalidSaveException("Could not find item type with name " + itemTypeName);
			}
		}

		this.quantity = asJson.getIntValue("quantity");

		String materialName = asJson.getString("material");
		if (materialName != null) {
			this.material = relatedStores.gameMaterialDictionary.getByName(materialName);
			if (this.material == null) {
				throw new InvalidSaveException("Could not find material by name " + materialName);
			}
			if (this.material.equals(GameMaterial.NULL_MATERIAL)) {
				GameMaterialType type = EnumParser.getEnumValue(asJson, "materialType", GameMaterialType.class, GameMaterialType.STONE);
				this.material = GameMaterial.nullMaterialWithType(type);
			}
		}

		this.liquid = asJson.getBooleanValue("liquid");
	}
}
