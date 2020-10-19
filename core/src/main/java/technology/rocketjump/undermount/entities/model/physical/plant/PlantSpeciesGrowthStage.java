package technology.rocketjump.undermount.entities.model.physical.plant;

import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;

import java.util.*;

public class PlantSpeciesGrowthStage {

	private String name; // Mostly to help with debugging, not used by code (index in list of growth stages used instead)
	private Integer nextGrowthStage = null; // Null representing no subsequent stage to go to
	private double seasonsUntilComplete = 1;

	private float initialPlantScale = 1;
	private float completionPlantScale = 1;
	private float initialFruitScale = 1;
	private float completionFruitScale = 1;
	private int tileHeight = 1;
	private boolean showFruit = false;
	private Map<ColoringLayer, PlantSpeciesColor> colors = new EnumMap<>(ColoringLayer.class);
	private List<PlantGrowthCompleteTag> onCompletion = new LinkedList<>();

	private PlantSpeciesHarvestType harvestType = null;
	private Integer harvestSwitchesToGrowthStage = null;
	private List<PlantSpeciesItem> harvestedItems = new ArrayList<>();

	// TODO might work better to replace this with JobType
	public enum PlantSpeciesHarvestType {

		LOGGING, FORAGING, FARMING

	}

	public enum PlantGrowthCompleteTag {

		DISPERSE_SEEDS, DESTROY_PLANT

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getSeasonsUntilComplete() {
		return seasonsUntilComplete;
	}

	public void setSeasonsUntilComplete(double seasonsUntilComplete) {
		this.seasonsUntilComplete = seasonsUntilComplete;
	}

	public float getInitialPlantScale() {
		return initialPlantScale;
	}

	public void setInitialPlantScale(float initialPlantScale) {
		this.initialPlantScale = initialPlantScale;
	}

	public float getCompletionPlantScale() {
		return completionPlantScale;
	}

	public void setCompletionPlantScale(float completionPlantScale) {
		this.completionPlantScale = completionPlantScale;
	}

	public float getInitialFruitScale() {
		return initialFruitScale;
	}

	public void setInitialFruitScale(float initialFruitScale) {
		this.initialFruitScale = initialFruitScale;
	}

	public float getCompletionFruitScale() {
		return completionFruitScale;
	}

	public void setCompletionFruitScale(float completionFruitScale) {
		this.completionFruitScale = completionFruitScale;
	}

	public Integer getNextGrowthStage() {
		return nextGrowthStage;
	}

	public void setNextGrowthStage(Integer nextGrowthStage) {
		this.nextGrowthStage = nextGrowthStage;
	}

	public int getTileHeight() {
		return tileHeight;
	}

	public void setTileHeight(int tileHeight) {
		this.tileHeight = tileHeight;
	}

	public Map<ColoringLayer, PlantSpeciesColor> getColors() {
		return colors;
	}

	public void setColors(Map<ColoringLayer, PlantSpeciesColor> colors) {
		this.colors = colors;
	}

	public List<PlantGrowthCompleteTag> getOnCompletion() {
		return onCompletion;
	}

	public void setOnCompletion(List<PlantGrowthCompleteTag> onCompletion) {
		this.onCompletion = onCompletion;
	}

	public PlantSpeciesHarvestType getHarvestType() {
		return harvestType;
	}

	public void setHarvestType(PlantSpeciesHarvestType harvestType) {
		this.harvestType = harvestType;
	}

	public List<PlantSpeciesItem> getHarvestedItems() {
		return harvestedItems;
	}

	public void setHarvestedItems(List<PlantSpeciesItem> harvestedItems) {
		this.harvestedItems = harvestedItems;
	}

	public boolean isShowFruit() {
		return showFruit;
	}

	public void setShowFruit(boolean showFruit) {
		this.showFruit = showFruit;
	}

	public Integer getHarvestSwitchesToGrowthStage() {
		return harvestSwitchesToGrowthStage;
	}

	public void setHarvestSwitchesToGrowthStage(Integer harvestSwitchesToGrowthStage) {
		this.harvestSwitchesToGrowthStage = harvestSwitchesToGrowthStage;
	}
}
