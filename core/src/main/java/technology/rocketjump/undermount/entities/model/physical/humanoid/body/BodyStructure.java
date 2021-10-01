package technology.rocketjump.undermount.entities.model.physical.humanoid.body;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.entities.model.physical.humanoid.body.features.BodyStructureFeatures;
import technology.rocketjump.undermount.misc.Name;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BodyStructure {

	@Name
	private String name;

	private BodyStructureFeatures features = new BodyStructureFeatures();

	private String rootPartName;
	@JsonIgnore
	private BodyPartDefinition rootPart;

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

	public String getRootPartName() {
		return rootPartName;
	}

	public void setRootPartName(String rootPartName) {
		this.rootPartName = rootPartName;
	}

	public Optional<BodyPartDefinition> getPartDefinitionByName(String name) {
		return partDefinitions.stream().filter(p -> p.getName().equals(name)).findFirst();
	}

	public BodyPartDefinition getRootPart() {
		return rootPart;
	}

	public void setRootPart(BodyPartDefinition rootPart) {
		this.rootPart = rootPart;
	}

	public List<BodyPartDefinition> getPartDefinitions() {
		return partDefinitions;
	}

	public void setPartDefinitions(List<BodyPartDefinition> partDefinitions) {
		this.partDefinitions = partDefinitions;
	}

	@Override
	public String toString() {
		return name;
	}
}
