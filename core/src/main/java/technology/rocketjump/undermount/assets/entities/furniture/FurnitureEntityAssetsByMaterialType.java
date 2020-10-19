package technology.rocketjump.undermount.assets.entities.furniture;

import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class FurnitureEntityAssetsByMaterialType {

	private Map<GameMaterialType, List<FurnitureEntityAsset>> byMaterialType = new EnumMap<>(GameMaterialType.class);

	public FurnitureEntityAssetsByMaterialType() {
		for (GameMaterialType gameMaterialType : GameMaterialType.values()) {
			byMaterialType.put(gameMaterialType, new ArrayList<>());
		}
	}

	public void add(FurnitureEntityAsset asset) {
		List<GameMaterialType> materialTypes = asset.getValidMaterialTypes();
		if (materialTypes == null) {
			throw new RuntimeException("Material types must be specified for " + asset);
		} else {
			for (GameMaterialType materialType : materialTypes) {
				byMaterialType.get(materialType).add(asset);
			}
		}
	}

	public FurnitureEntityAsset get(FurnitureEntityAttributes attributes) {
		List<FurnitureEntityAsset> assets = byMaterialType.get(attributes.getPrimaryMaterialType());
		if (assets.size() == 0) {
			Logger.error("Could not find applicable asset for " + attributes.toString());
			return null;
		} else {
			return assets.get((Math.abs((int)attributes.getSeed())) % assets.size());
		}

	}

	public List<FurnitureEntityAsset> getAll(FurnitureEntityAttributes attributes) {
		return byMaterialType.get(attributes.getPrimaryMaterialType());
	}

}
