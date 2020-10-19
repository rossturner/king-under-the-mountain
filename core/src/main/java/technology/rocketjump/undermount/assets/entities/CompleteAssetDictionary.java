package technology.rocketjump.undermount.assets.entities;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.entities.furniture.FurnitureEntityAssetDictionary;
import technology.rocketjump.undermount.assets.entities.humanoid.HumanoidEntityAssetDictionary;
import technology.rocketjump.undermount.assets.entities.item.ItemEntityAssetDictionary;
import technology.rocketjump.undermount.assets.entities.model.EntityAsset;
import technology.rocketjump.undermount.assets.entities.plant.PlantEntityAssetDictionary;
import technology.rocketjump.undermount.assets.entities.wallcap.WallCapAssetDictionary;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class CompleteAssetDictionary {

	private final Map<String, EntityAsset> allAssetsByName = new HashMap<>();

	@Inject
	public CompleteAssetDictionary(HumanoidEntityAssetDictionary humanoidEntityAssetDictionary, FurnitureEntityAssetDictionary furnitureEntityAssetDictionary,
								   PlantEntityAssetDictionary plantEntityAssetDictionary, ItemEntityAssetDictionary itemEntityAssetDictionary,
								   WallCapAssetDictionary wallCapAssetDictionary) {
		allAssetsByName.putAll(humanoidEntityAssetDictionary.getAll());
		allAssetsByName.putAll(plantEntityAssetDictionary.getAll());
		allAssetsByName.putAll(itemEntityAssetDictionary.getAll());
		allAssetsByName.putAll(furnitureEntityAssetDictionary.getAll());
		allAssetsByName.putAll(wallCapAssetDictionary.getAll());
	}

	public EntityAsset getByUniqueName(String uniqueAssetName) {
		return allAssetsByName.get(uniqueAssetName);
	}


}
