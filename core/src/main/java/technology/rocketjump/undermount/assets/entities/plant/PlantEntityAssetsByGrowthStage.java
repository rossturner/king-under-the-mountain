package technology.rocketjump.undermount.assets.entities.plant;

import com.badlogic.gdx.utils.IntMap;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.plant.model.PlantEntityAsset;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesDictionary;

import java.util.LinkedList;
import java.util.List;

public class PlantEntityAssetsByGrowthStage {

	private static final int MAX_GROWTH_STAGES = 10;

	private IntMap<PlantEntityAssetsBySpecies> byGrowthStageIndex = new IntMap<>();

	public PlantEntityAssetsByGrowthStage(PlantSpeciesDictionary speciesDictionary) {
		for (int cursor = 0; cursor < MAX_GROWTH_STAGES; cursor++) {
			byGrowthStageIndex.put(cursor, new PlantEntityAssetsBySpecies(speciesDictionary));
		}
	}

	public void add(PlantEntityAsset asset) {
		if (asset.getGrowthStages().isEmpty()) {
			// Add to all
			for (int cursor = 0; cursor < MAX_GROWTH_STAGES; cursor++) {
				byGrowthStageIndex.get(cursor).add(asset);
			}
		} else {
			for (Integer growthStage : asset.getGrowthStages()) {
				if (growthStage >= MAX_GROWTH_STAGES) {
					throw new RuntimeException("Too many growth stages for " + asset.getUniqueName() + ", hard limit of " + MAX_GROWTH_STAGES + " needs increasing");
				}
				byGrowthStageIndex.get(growthStage).add(asset);
			}
		}
	}

	public PlantEntityAsset get(PlantEntityAttributes attributes) {
		PlantEntityAssetsBySpecies bySpecies = byGrowthStageIndex.get(attributes.getGrowthStageCursor());
		if (bySpecies == null) {
			Logger.error("No plant assets for growth stage: " + attributes.getGrowthStageCursor());
			return PlantEntityAssetsBySpecies.NULL_ENTITY_ASSET;
		}
		return bySpecies.get(attributes);
	}

	public List<PlantEntityAsset> getAll(PlantEntityAttributes attributes) {
		PlantEntityAssetsBySpecies bySpecies = byGrowthStageIndex.get(attributes.getGrowthStageCursor());
		if (bySpecies == null) {
			Logger.error("No list of plant assets for height: " + attributes.getGrowthStageCursor());
			return new LinkedList<>();
		}
		return bySpecies.getAll(attributes);
	}
}
