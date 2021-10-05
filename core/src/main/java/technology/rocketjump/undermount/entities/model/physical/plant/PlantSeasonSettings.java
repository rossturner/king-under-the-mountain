package technology.rocketjump.undermount.entities.model.physical.plant;

import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;

import java.util.EnumMap;
import java.util.Map;

public class PlantSeasonSettings {

	private boolean growth = true;
	private Map<ColoringLayer, SpeciesColor> colors = new EnumMap<>(ColoringLayer.class);
	private Integer switchToGrowthStage = null; // Can be used to switch to a decaying-type stage
	private boolean shedsLeaves = false;

	public boolean isGrowth() {
		return growth;
	}

	public void setGrowth(boolean growth) {
		this.growth = growth;
	}

	public Map<ColoringLayer, SpeciesColor> getColors() {
		return colors;
	}

	public void setColors(Map<ColoringLayer, SpeciesColor> colors) {
		this.colors = colors;
	}

	public Integer getSwitchToGrowthStage() {
		return switchToGrowthStage;
	}

	public void setSwitchToGrowthStage(Integer switchToGrowthStage) {
		this.switchToGrowthStage = switchToGrowthStage;
	}

	public boolean isShedsLeaves() {
		return shedsLeaves;
	}

	public void setShedsLeaves(boolean shedsLeaves) {
		this.shedsLeaves = shedsLeaves;
	}
}
