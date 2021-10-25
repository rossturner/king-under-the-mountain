package technology.rocketjump.undermount.entities.model.physical.creature.features;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RaceFeatures {

	private SkinFeature skin;
	private BonesFeature bones;
	private BloodFeature blood;

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

	public BloodFeature getBlood() {
		return blood;
	}

	public void setBlood(BloodFeature blood) {
		this.blood = blood;
	}
}
