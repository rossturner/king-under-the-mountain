package technology.rocketjump.undermount.entities.model.physical.item;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class QuantifiedItemType {

	private String itemTypeName;
	@JsonIgnore
	private ItemType itemType;
	private int quantity;
	private boolean liquid = false;

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
	public String toString() {
		if (itemType != null) {
			return itemType.getItemTypeName() + " x" + quantity + (liquid ? " (liquid)" : "");
		} else {
			return itemTypeName + " x" + quantity + (liquid ? " (liquid)" : "");
		}
	}
}
