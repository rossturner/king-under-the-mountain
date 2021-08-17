package technology.rocketjump.undermount.entities.model.physical.mechanism;

import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.entities.tags.Tag;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.misc.Name;

import java.util.*;

public class MechanismType {

	@Name
	private String name;

	private GameMaterialType primaryMaterialType;

	private Map<String, List<String>> tags = new HashMap<>();
	@JsonIgnore
	private List<Tag> processedTags = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
		MechanismType that = (MechanismType) o;
		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	public GameMaterialType getPrimaryMaterialType() {
		return primaryMaterialType;
	}

	public void setPrimaryMaterialType(GameMaterialType primaryMaterialType) {
		this.primaryMaterialType = primaryMaterialType;
	}
}
