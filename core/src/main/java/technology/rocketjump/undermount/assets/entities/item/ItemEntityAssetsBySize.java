package technology.rocketjump.undermount.assets.entities.item;

import technology.rocketjump.undermount.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.undermount.assets.entities.item.model.ItemSize;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;

import java.util.EnumMap;
import java.util.List;

public class ItemEntityAssetsBySize {

	private EnumMap<ItemSize, ItemEntityAssetsByPlacement> byItemSize = new EnumMap<>(ItemSize.class);

	public ItemEntityAssetsBySize() {
		for (ItemSize itemSize : ItemSize.values()) {
			byItemSize.put(itemSize, new ItemEntityAssetsByPlacement());
		}
	}

	public void add(ItemEntityAsset asset) {
		ItemSize assetItemSize = asset.getItemSize();
		if (assetItemSize == null) {
			// Add to all
			for (ItemSize itemSize : ItemSize.values()) {
				byItemSize.get(itemSize).add(asset);
			}
		} else {
			byItemSize.get(assetItemSize).add(asset);
		}
	}

	public ItemEntityAsset get(ItemEntityAttributes attributes) {
		ItemSize itemSize = attributes.getItemSize();
		if (itemSize == null) {
			itemSize = ItemSize.AVERAGE;
		}
		return byItemSize.get(itemSize).get(attributes);
	}

	public List<ItemEntityAsset> getAll(ItemEntityAttributes attributes) {
		ItemSize itemSize = attributes.getItemSize();
		if (itemSize == null) {
			itemSize = ItemSize.AVERAGE;
		}
		return byItemSize.get(itemSize).getAll(attributes);
	}

	public ItemEntityAssetsByPlacement getBySize(ItemSize itemSize) {
		return byItemSize.get(itemSize);
	}
}
