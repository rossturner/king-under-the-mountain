package technology.rocketjump.undermount.entities.model.physical.creature;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.assets.entities.creature.model.CreatureBodyTypeDescriptor;
import technology.rocketjump.undermount.entities.model.physical.creature.features.RaceFeatures;
import technology.rocketjump.undermount.misc.Name;

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Race {

	@Name
	private String name;
	private String i18nKey;

	private float minStrength;
	private float maxStrength;

	private List<CreatureBodyTypeDescriptor> bodyTypes;

	private RaceFeatures features = new RaceFeatures();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public void setI18nKey(String i18nKey) {
		this.i18nKey = i18nKey;
	}

	public float getMinStrength() {
		return minStrength;
	}

	public void setMinStrength(float minStrength) {
		this.minStrength = minStrength;
	}

	public float getMaxStrength() {
		return maxStrength;
	}

	public void setMaxStrength(float maxStrength) {
		this.maxStrength = maxStrength;
	}

	public RaceFeatures getFeatures() {
		return features;
	}

	public void setFeatures(RaceFeatures features) {
		this.features = features;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Race race = (Race) o;
		return name.equals(race.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		return name;
	}

	public List<CreatureBodyTypeDescriptor> getBodyTypes() {
		return bodyTypes;
	}

	public void setBodyTypes(List<CreatureBodyTypeDescriptor> bodyTypes) {
		this.bodyTypes = bodyTypes;
	}
}
