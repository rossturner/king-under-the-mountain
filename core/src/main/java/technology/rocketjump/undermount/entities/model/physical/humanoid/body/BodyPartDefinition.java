package technology.rocketjump.undermount.entities.model.physical.humanoid.body;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.misc.Name;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BodyPartDefinition {

	@Name
	private String name;
	private float size;
	private List<BoneType> bones = new ArrayList<>();
	private List<BodyPartOrgan> organs = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public float getSize() {
		return size;
	}

	public void setSize(float size) {
		this.size = size;
	}

	public List<BoneType> getBones() {
		return bones;
	}

	public void setBones(List<BoneType> bones) {
		this.bones = bones;
	}

	public List<BodyPartOrgan> getOrgans() {
		return organs;
	}

	public void setOrgans(List<BodyPartOrgan> organs) {
		this.organs = organs;
	}
}
