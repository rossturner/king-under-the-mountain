package technology.rocketjump.undermount.entities.model.physical.plant;

import com.badlogic.gdx.graphics.Color;
import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.entities.tags.Tag;
import technology.rocketjump.undermount.environment.model.Season;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.misc.Name;

import java.util.*;

public class PlantSpecies {

	@Name
	private String speciesName;
	private PlantSpeciesType plantType;
	private String representativeColor;
	private String materialName;
	private float maxGrowthSpeedVariance = 0f; // A positive or negative max multiplier on growth speed;
	private int replacesOtherPlantsInRegion = 0;

	private Map<ColoringLayer, SpeciesColor> defaultColors = new EnumMap<>(ColoringLayer.class);
	// Base material type for catching fire, that kind of thing
	private Map<Season, PlantSeasonSettings> seasons = new EnumMap<>(Season.class);
	private List<PlantSpeciesGrowthStage> growthStages = new ArrayList<>();
	private PlantSpeciesSeed seed = null;
	private float occurenceWeight = 0f;

	@JsonIgnore
	private Color representationColor = Color.MAGENTA;
	@JsonIgnore
	private GameMaterial material;

	private Map<String, List<String>> tags = new HashMap<>();
	@JsonIgnore
	private List<Tag> processedTags = new ArrayList<>();

	public String getSpeciesName() {
		return speciesName;
	}

	public void setSpeciesName(String speciesName) {
		this.speciesName = speciesName;
	}

	public PlantSpeciesType getPlantType() {
		return plantType;
	}

	public void setPlantType(PlantSpeciesType plantType) {
		this.plantType = plantType;
	}

	public String getRepresentativeColor() {
		return representativeColor;
	}

	public void setRepresentativeColor(String representativeColor) {
		this.representativeColor = representativeColor;
	}

	public Map<ColoringLayer, SpeciesColor> getDefaultColors() {
		return defaultColors;
	}

	public void setDefaultColors(Map<ColoringLayer, SpeciesColor> defaultColors) {
		this.defaultColors = defaultColors;
	}

	public Map<Season, PlantSeasonSettings> getSeasons() {
		return seasons;
	}

	public void setSeasons(Map<Season, PlantSeasonSettings> seasons) {
		this.seasons = seasons;
	}

	public List<PlantSpeciesGrowthStage> getGrowthStages() {
		return growthStages;
	}

	public void setGrowthStages(List<PlantSpeciesGrowthStage> growthStages) {
		this.growthStages = growthStages;
	}

	public float getMaxGrowthSpeedVariance() {
		return maxGrowthSpeedVariance;
	}

	public void setMaxGrowthSpeedVariance(float maxGrowthSpeedVariance) {
		this.maxGrowthSpeedVariance = maxGrowthSpeedVariance;
	}

	public Color getRepresentationColor() {
		return representationColor;
	}

	public void setRepresentationColor(Color representationColor) {
		this.representationColor = representationColor;
	}

	public String getMaterialName() {
		return materialName;
	}

	public void setMaterialName(String materialName) {
		this.materialName = materialName;
	}

	public GameMaterial getMaterial() {
		return material;
	}

	public void setMaterial(GameMaterial material) {
		this.material = material;
	}

	public PlantSpeciesSeed getSeed() {
		return seed;
	}

	public void setSeed(PlantSpeciesSeed seed) {
		this.seed = seed;
	}

	public float getOccurenceWeight() {
		return occurenceWeight;
	}

	public void setOccurenceWeight(float occurenceWeight) {
		this.occurenceWeight = occurenceWeight;
	}

	public int getReplacesOtherPlantsInRegion() {
		return replacesOtherPlantsInRegion;
	}

	public void setReplacesOtherPlantsInRegion(int replacesOtherPlantsInRegion) {
		this.replacesOtherPlantsInRegion = replacesOtherPlantsInRegion;
	}

	public List<SpeciesColor> getAllColorObjects() {
		List<SpeciesColor> allColors = new LinkedList<>();

		allColors.addAll(defaultColors.values());

		for (PlantSeasonSettings plantSeasonSettings : seasons.values()) {
			allColors.addAll(plantSeasonSettings.getColors().values());
		}

		for (PlantSpeciesGrowthStage growthStage : growthStages) {
			allColors.addAll(growthStage.getColors().values());
		}

		return allColors;
	}

	public Map<String, List<String>> getTags() {
		return tags;
	}

	public void setTags(Map<String, List<String>> tags) {
		this.tags = tags;
	}

	public void setProcessedTags(List<Tag> processedTags) {
		this.processedTags = processedTags;
	}

	public List<Tag> getProcessedTags() {
		return processedTags;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PlantSpecies that = (PlantSpecies) o;
		return Objects.equals(this.speciesName, that.speciesName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(speciesName);
	}

	public boolean anyStageHasFruit() {
		for (PlantSpeciesGrowthStage growthStage : growthStages) {
			if (growthStage.isShowFruit()) {
				return true;
			}
		}
		return false;
	}

	public boolean anyStageProducesItem() {
		for (PlantSpeciesGrowthStage growthStage : growthStages) {
			if (growthStage.getHarvestedItems() != null) {
				return true;
			}
		}
		return false;
	}
}
