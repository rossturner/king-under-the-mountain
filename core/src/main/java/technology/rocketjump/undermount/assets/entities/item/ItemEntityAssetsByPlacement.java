package technology.rocketjump.undermount.assets.entities.item;

import technology.rocketjump.undermount.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.undermount.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;

import java.util.EnumMap;
import java.util.List;

public class ItemEntityAssetsByPlacement {

	private EnumMap<ItemPlacement, ItemEntityAssetsByStyle> byItemPlacement = new EnumMap<>(ItemPlacement.class);

	public ItemEntityAssetsByPlacement() {
		for (ItemPlacement iItemPlacement : ItemPlacement.values()) {
			byItemPlacement.put(iItemPlacement, new ItemEntityAssetsByStyle());
		}
	}

	public void add(ItemEntityAsset asset) {
		List<ItemPlacement> itemPlacements = asset.getItemPlacements();
		if (itemPlacements == null || itemPlacements.isEmpty()) {
			// Not specified, so add to all
			for (ItemPlacement itemPlacement : ItemPlacement.values()) {
				byItemPlacement.get(itemPlacement).add(asset);
			}
		} else {
			for (ItemPlacement itemPlacement : itemPlacements) {
				byItemPlacement.get(itemPlacement).add(asset);
			}
		}
	}

	public ItemEntityAsset get(ItemEntityAttributes attributes) {
		ItemPlacement itemPlacement = attributes.getItemPlacement();
		if (itemPlacement == null) {
			itemPlacement = ItemPlacement.ON_GROUND;
		}
		return byItemPlacement.get(itemPlacement).get(attributes);
	}

	public List<ItemEntityAsset> getAll(ItemEntityAttributes attributes) {
		ItemPlacement itemPlacement = attributes.getItemPlacement();
		if (itemPlacement == null) {
			itemPlacement = ItemPlacement.ON_GROUND;
		}
		return byItemPlacement.get(itemPlacement).getAll(attributes);
	}

	public ItemEntityAssetsByStyle getByPlacement(ItemPlacement itemPlacement) {
		return byItemPlacement.get(itemPlacement);
	}
}
