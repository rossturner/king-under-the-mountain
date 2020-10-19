package technology.rocketjump.undermount.assets.entities.item.model;

import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.misc.Name;

import java.util.ArrayList;
import java.util.List;

public class ItemEntityAssetDescriptor {

	@Name
	private String uniqueName;
	private EntityAssetType type;
	private String itemTypeName;
	private List<String> itemTypeNames;
	private int minQuantity = 1; // The fewest amount that this asset represents
	private int maxQuantity = 1; // The largest amount that this asset can represent
	private ItemSize itemSize;
	private ItemStyle itemStyle;
	private List<ItemPlacement> itemPlacements = new ArrayList<>();

	public boolean matches(ItemEntityAttributes entityAttributes) {
		if (itemTypeName != null && !itemTypeName.equals(entityAttributes.getItemType().getItemTypeName())) {
			return false;
		}
		if (itemTypeNames != null && !itemTypeNames.contains(entityAttributes.getItemType().getItemTypeName())) {
			return false;
		}
		if (minQuantity > entityAttributes.getQuantity() || maxQuantity < entityAttributes.getQuantity()) {
			return false;
		}
		if (itemSize != null && entityAttributes.getItemSize() != null && !itemSize.equals(entityAttributes.getItemSize())) {
			return false;
		}
		if (itemStyle != null && entityAttributes.getItemStyle() != null && !itemStyle.equals(entityAttributes.getItemStyle())) {
			return false;
		}
		return true;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public void setUniqueName(String uniqueName) {
		this.uniqueName = uniqueName;
	}

	public EntityAssetType getType() {
		return type;
	}

	public void setType(EntityAssetType type) {
		this.type = type;
	}

	public String getItemTypeName() {
		return itemTypeName;
	}

	public void setItemTypeName(String itemTypeName) {
		this.itemTypeName = itemTypeName;
	}

	public List<String> getItemTypeNames() {
		return itemTypeNames;
	}

	public void setItemTypeNames(List<String> itemTypeNames) {
		this.itemTypeNames = itemTypeNames;
	}

	public ItemSize getItemSize() {
		return itemSize;
	}

	public void setItemSize(ItemSize itemSize) {
		this.itemSize = itemSize;
	}

	public int getMinQuantity() {
		return minQuantity;
	}

	public void setMinQuantity(int minQuantity) {
		this.minQuantity = minQuantity;
	}

	public int getMaxQuantity() {
		return maxQuantity;
	}

	public void setMaxQuantity(int maxQuantity) {
		this.maxQuantity = maxQuantity;
	}

	public ItemStyle getItemStyle() {
		return itemStyle;
	}

	public void setItemStyle(ItemStyle itemStyle) {
		this.itemStyle = itemStyle;
	}

	public List<ItemPlacement> getItemPlacements() {
		return itemPlacements;
	}

	public void setItemPlacements(List<ItemPlacement> itemPlacements) {
		this.itemPlacements = itemPlacements;
	}
}
