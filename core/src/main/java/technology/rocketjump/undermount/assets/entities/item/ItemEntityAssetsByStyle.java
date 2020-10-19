package technology.rocketjump.undermount.assets.entities.item;

import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.undermount.assets.entities.item.model.ItemStyle;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class ItemEntityAssetsByStyle {

	private EnumMap<ItemStyle, List<ItemEntityAsset>> styleMap = new EnumMap<>(ItemStyle.class);

	public ItemEntityAssetsByStyle() {
		for (ItemStyle itemStyle : ItemStyle.values()) {
			styleMap.put(itemStyle, new ArrayList<>());
		}

	}

	public void add(ItemEntityAsset asset) {
		ItemStyle itemStyle = asset.getItemStyle();
		if (itemStyle == null) {
			// Add to all
			for (ItemStyle style : ItemStyle.values()) {
				styleMap.get(style).add(asset);
			}
		} else {
			styleMap.get(itemStyle).add(asset);
		}
	}

	public ItemEntityAsset get(ItemEntityAttributes attributes) {
		ItemStyle itemStyle = attributes.getItemStyle();
		if (itemStyle == null) {
			itemStyle = ItemStyle.DEFAULT;
		}
		List<ItemEntityAsset> assets = styleMap.get(itemStyle);
		if (assets.size() == 0) {
			Logger.error("Could not find applicable asset for " + attributes.toString());
			return null;
		} else {
			return assets.get((Math.abs((int)attributes.getSeed())) % assets.size());
		}
	}

	public List<ItemEntityAsset> getAll(ItemEntityAttributes attributes) {
		ItemStyle itemStyle = attributes.getItemStyle();
		if (itemStyle == null) {
			itemStyle = ItemStyle.DEFAULT;
		}
		return styleMap.get(itemStyle);
	}

	public List<ItemEntityAsset> getByStyle(ItemStyle itemStyle) {
		return styleMap.get(itemStyle);
	}
}
