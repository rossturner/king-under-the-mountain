package technology.rocketjump.undermount.assets.entities.plant.model;

import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.misc.Name;

import java.util.*;

public class PlantEntityAssetDescriptor {

	@Name
	private String uniqueName;
	private EntityAssetType type;
	private String speciesName;
	private List<String> speciesNames;
	private Set<Integer> growthStages = new HashSet<>();

	protected Map<String, List<String>> tags = new HashMap<>();

	public boolean matches(PlantEntityAttributes entityAttributes) {
		if (speciesName != null && !speciesName.equals(entityAttributes.getSpecies().getSpeciesName())) {
			return false;
		}
		if (speciesNames != null && !speciesNames.contains(entityAttributes.getSpecies().getSpeciesName())) {
			return false;
		}
		if (!growthStages.isEmpty() && !growthStages.contains(entityAttributes.getGrowthStageCursor())) {
			return false;
		}
		return true;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public void setUniqueName(String uniqueName) {
		this.uniqueName = uniqueName;
	}

	public EntityAssetType getType() {
		return type;
	}

	public void setType(EntityAssetType type) {
		this.type = type;
	}

	public String getSpeciesName() {
		return speciesName;
	}

	public void setSpeciesName(String speciesName) {
		this.speciesName = speciesName;
	}

	public List<String> getSpeciesNames() {
		return speciesNames;
	}

	public void setSpeciesNames(List<String> speciesNames) {
		this.speciesNames = speciesNames;
	}

	public Set<Integer> getGrowthStages() {
		return growthStages;
	}

	public void setGrowthStages(Set<Integer> growthStages) {
		this.growthStages = growthStages;
	}

	public Map<String, List<String>> getTags() {
		return tags;
	}

	public void setTags(Map<String, List<String>> tags) {
		this.tags = tags;
	}
}
