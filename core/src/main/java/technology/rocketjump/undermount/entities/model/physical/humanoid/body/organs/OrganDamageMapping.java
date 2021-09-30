package technology.rocketjump.undermount.entities.model.physical.humanoid.body.organs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.EnumMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganDamageMapping {

	private Map<OrganDamageLevel, OrganDamageEffect> finalInstance = new EnumMap<>(OrganDamageLevel.class);
	private Map<OrganDamageLevel, OrganDamageEffect> other = new EnumMap<>(OrganDamageLevel.class);

	public Map<OrganDamageLevel, OrganDamageEffect> getFinalInstance() {
		return finalInstance;
	}

	public void setFinalInstance(Map<OrganDamageLevel, OrganDamageEffect> finalInstance) {
		this.finalInstance = finalInstance;
	}

	public Map<OrganDamageLevel, OrganDamageEffect> getOther() {
		return other;
	}

	public void setOther(Map<OrganDamageLevel, OrganDamageEffect> other) {
		this.other = other;
	}
}
