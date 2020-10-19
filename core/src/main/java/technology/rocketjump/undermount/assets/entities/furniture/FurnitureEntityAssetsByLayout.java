package technology.rocketjump.undermount.assets.entities.furniture;

import technology.rocketjump.undermount.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureLayoutDictionary;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FurnitureEntityAssetsByLayout {

	private final FurnitureLayoutDictionary layoutDictionary;
	private Map<String, FurnitureEntityAssetsByMaterialType> byLayoutName = new HashMap<>();

	public FurnitureEntityAssetsByLayout(FurnitureLayoutDictionary layoutDictionary) {
		this.layoutDictionary = layoutDictionary;
		for (FurnitureLayout furnitureLayout : layoutDictionary.getAll()) {
			byLayoutName.put(furnitureLayout.getUniqueName(), new FurnitureEntityAssetsByMaterialType());
		}
	}

	public void add(FurnitureEntityAsset asset) {
		String layoutName = asset.getFurnitureLayoutName();
		if (layoutName == null) {
			for (FurnitureLayout furnitureLayout : layoutDictionary.getAll()) {
				byLayoutName.get(furnitureLayout.getUniqueName()).add(asset);
			}
		} else if (!byLayoutName.containsKey(layoutName)) {
			throw new RuntimeException("Could not match layout by name " + layoutName + " in " + this.getClass().getSimpleName());
		} else {
			byLayoutName.get(layoutName).add(asset);
		}
	}

	public FurnitureEntityAsset get(FurnitureEntityAttributes attributes) {
		String layoutName = attributes.getCurrentLayout().getUniqueName();
		return byLayoutName.get(layoutName).get(attributes);
	}

	public List<FurnitureEntityAsset> getAll(FurnitureEntityAttributes attributes) {
		String layoutName = attributes.getCurrentLayout().getUniqueName();
		return byLayoutName.get(layoutName).getAll(attributes);
	}
}
