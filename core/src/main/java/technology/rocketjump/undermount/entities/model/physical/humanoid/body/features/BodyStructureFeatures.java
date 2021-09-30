package technology.rocketjump.undermount.entities.model.physical.humanoid.body.features;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BodyStructureFeatures {

	private SkinFeature skin;
	private BonesFeature bones;
	private Object blood;

	public SkinFeature getSkin() {
		return skin;
	}

	public void setSkin(SkinFeature skin) {
		this.skin = skin;
	}

	public BonesFeature getBones() {
		return bones;
	}

	public void setBones(BonesFeature bones) {
		this.bones = bones;
	}

	public Object getBlood() {
		return blood;
	}

	public void setBlood(Object blood) {
		this.blood = blood;
	}
}
