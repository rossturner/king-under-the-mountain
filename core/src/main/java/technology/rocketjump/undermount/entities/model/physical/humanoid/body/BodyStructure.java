package technology.rocketjump.undermount.entities.model.physical.humanoid.body;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.entities.model.physical.humanoid.body.features.BodyStructureFeatures;
import technology.rocketjump.undermount.misc.Name;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BodyStructure {

	@Name
	private String name;

	private BodyStructureFeatures features = new BodyStructureFeatures();

	private List<BodyPartDefinition> partDefinitions = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BodyStructureFeatures getFeatures() {
		return features;
	}

	public void setFeatures(BodyStructureFeatures features) {
		this.features = features;
	}

	public List<BodyPartDefinition> getPartDefinitions() {
		return partDefinitions;
	}

	public void setPartDefinitions(List<BodyPartDefinition> partDefinitions) {
		this.partDefinitions = partDefinitions;
	}
}
