package technology.rocketjump.undermount.entities.model.physical.plant;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.entities.model.physical.EntityAttributes;
import technology.rocketjump.undermount.environment.model.Season;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.utils.HexColors;

import java.util.EnumMap;
import java.util.Map;

import static technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesGrowthStage.PlantSpeciesHarvestType.FARMING;

public class PlantEntityAttributes implements EntityAttributes {

	private long seed;
	private PlantSpecies species;

	private Map<ColoringLayer, Color> actualColors = new EnumMap<>(ColoringLayer.class);

	private int growthStageCursor = 0;
	private float growthStageProgress = 0.75f;
	private float growthRate = 1;

	private double seasonProgress = 0;

	private boolean afflictedByPests;
	private Job removePestsJob;

	public PlantEntityAttributes() {

	}

	public PlantEntityAttributes(long seed, PlantSpecies species) {
		this.seed = seed;
		this.species = species;

		updateColors(null);
	}

	/**
	 * @return true if a colour changed from CLEAR to something else or back, necessitating an asset update
	 */
	public boolean updateColors(Season currentSeason) {
		boolean assetUpdateRequired = false;
		PlantSpeciesGrowthStage currentGrowthStage = species.getGrowthStages().get(this.growthStageCursor);
		float progress = growthStageProgress;
		for (Map.Entry<ColoringLayer, PlantSpeciesColor> colorEntry : species.getDefaultColors().entrySet()) {
			PlantSpeciesColor colorToUse = colorEntry.getValue();

			if (currentGrowthStage.getColors().containsKey(colorEntry.getKey())) {
				colorToUse = currentGrowthStage.getColors().get(colorEntry.getKey());
			}

			PlantSeasonSettings seasonSettings = null;

			if (currentSeason != null && species.getSeasons().containsKey(currentSeason)) {
				seasonSettings = species.getSeasons().get(currentSeason);
			}

			if (seasonSettings != null) {
				if (seasonSettings.getColors().containsKey(colorEntry.getKey())) {
					colorToUse = species.getSeasons().get(currentSeason).getColors().get(colorEntry.getKey());
					progress = (float) seasonProgress;
				}
			}

			Color oldColor = actualColors.get(colorEntry.getKey());
			Color newColor = colorToUse.getColor(progress, seed);

			actualColors.put(colorEntry.getKey(), newColor);

			if (Color.CLEAR.equals(oldColor) && !Color.CLEAR.equals(newColor) ||
					!Color.CLEAR.equals(oldColor) && Color.CLEAR.equals(newColor)) {
				assetUpdateRequired = true;
			}
		}
		return assetUpdateRequired;
	}

	@Override
	public long getSeed() {
		return seed;
	}

	@Override
	public Color getColor(ColoringLayer coloringLayer) {
		return actualColors.get(coloringLayer);
	}

	@Override
	public EntityAttributes clone() {
		PlantEntityAttributes cloned = new PlantEntityAttributes(seed, species);

		cloned.actualColors.putAll(this.actualColors);

		cloned.growthStageCursor = this.growthStageCursor;
		cloned.growthRate = this.growthRate;
		cloned.growthStageProgress = this.growthStageProgress;

		return cloned;
	}

	public int getGrowthStageCursor() {
		return growthStageCursor;
	}

	public void setGrowthStageCursor(int growthStageCursor) {
		this.growthStageCursor = growthStageCursor;
		setGrowthStageProgress(0);
	}

	public float getGrowthStageProgress() {
		return growthStageProgress;
	}

	public void setGrowthStageProgress(float growthStageProgress) {
		if (growthStageProgress > 1f) {
			growthStageProgress = 1f;
		}
		this.growthStageProgress = growthStageProgress;
	}

	public float getGrowthRate() {
		return growthRate;
	}

	public void setGrowthRate(float growthRate) {
		this.growthRate = growthRate;
	}

	public PlantSpecies getSpecies() {
		return species;
	}

	public double getSeasonProgress() {
		return seasonProgress;
	}

	public boolean isAfflictedByPests() {
		return afflictedByPests;
	}

	public void setAfflictedByPests(Job removePestsJob) {
		this.afflictedByPests = true;
		this.removePestsJob = removePestsJob;
	}

	public void clearAfflitctedByPests() {
		this.afflictedByPests = false;
		this.removePestsJob = null;
	}

	public Job getRemovePestsJob() {
		return removePestsJob;
	}

	public void setSeasonProgress(double seasonProgress) {
		if (seasonProgress > 1.0) {
			seasonProgress = 1.0;
		}
		this.seasonProgress = seasonProgress;
	}

	public float estimatedProgressToHarvesting() {
		if (species.getPlantType().equals(PlantSpeciesType.CROP)) {
			float totalSeasonsUntilHarvest = 0;
			float seasonProgressCompleted = 0;

			Integer growthStageIterator = 0;
			boolean currentStageReached = false;
			while (growthStageIterator != null) {
				PlantSpeciesGrowthStage iteratorGrowthStage = species.getGrowthStages().get(growthStageIterator);
				if (FARMING.equals(iteratorGrowthStage.getHarvestType())) {
					break;
				}

				totalSeasonsUntilHarvest += iteratorGrowthStage.getSeasonsUntilComplete();
				if (this.growthStageCursor == growthStageIterator) {
					 seasonProgressCompleted += (iteratorGrowthStage.getSeasonsUntilComplete() * this.growthStageProgress);
					currentStageReached = true;
				} else if (!currentStageReached) {
					seasonProgressCompleted += iteratorGrowthStage.getSeasonsUntilComplete();
				}

				growthStageIterator = iteratorGrowthStage.getNextGrowthStage();
			}

			return seasonProgressCompleted / totalSeasonsUntilHarvest;
		} else {
			return 0;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("seed", seed);
		asJson.put("species", species.getSpeciesName());

		if (!actualColors.isEmpty()) {
			JSONObject colorsJson = new JSONObject(true);
			for (Map.Entry<ColoringLayer, Color> entry : actualColors.entrySet()) {
				colorsJson.put(entry.getKey().name(), HexColors.toHexString(entry.getValue()));
			}
			asJson.put("colors", colorsJson);
		}
		asJson.put("growthStage", growthStageCursor);
		asJson.put("stageProgress", growthStageProgress);
		asJson.put("growthRate", growthRate);
		asJson.put("seasonProgress", seasonProgress);
		if (afflictedByPests) {
			asJson.put("afflicted", true);
		}
		if (removePestsJob != null) {
			removePestsJob.writeTo(savedGameStateHolder);
			asJson.put("removePestsJob", removePestsJob.getJobId());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		seed = asJson.getLongValue("seed");
		species = relatedStores.plantSpeciesDictionary.getByName(asJson.getString("species"));
		if (species == null) {
			throw new InvalidSaveException("Could not find plant species by name " + asJson.getString("species"));
		}
		JSONObject colorsJson = asJson.getJSONObject("colors");
		if (colorsJson != null) {
			for (String coloringLayerName : colorsJson.keySet()) {
				ColoringLayer coloringLayer = EnumUtils.getEnum(ColoringLayer.class, coloringLayerName);
				if (coloringLayer == null) {
					throw new InvalidSaveException("Could not find coloring layer by name " + coloringLayerName);
				}
				Color color = HexColors.get(colorsJson.getString(coloringLayerName));
				actualColors.put(coloringLayer, color);
			}
		}
		growthStageCursor = asJson.getIntValue("growthStage");
		growthStageProgress = asJson.getFloatValue("stageProgress");
		growthRate = asJson.getFloatValue("growthRate");
		seasonProgress = asJson.getFloatValue("seasonProgress");
		afflictedByPests = asJson.getBooleanValue("afflicted");
		Long removePestsJobId = asJson.getLong("removePestsJob");
		if (removePestsJobId != null) {
			removePestsJob = savedGameStateHolder.jobs.get(removePestsJobId);
			if (removePestsJob == null) {
				throw new InvalidSaveException("Could not find job by ID " + removePestsJobId);
			}
		}
	}
}
