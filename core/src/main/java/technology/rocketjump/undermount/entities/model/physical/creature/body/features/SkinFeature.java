package technology.rocketjump.undermount.entities.model.physical.creature.body.features;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.entities.model.physical.combat.CombatDamageType;

import java.util.EnumMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SkinFeature {

	private Map<CombatDamageType, Integer> damageReduction = new EnumMap<>(CombatDamageType.class);

	public Map<CombatDamageType, Integer> getDamageReduction() {
		return damageReduction;
	}

	public void setDamageReduction(Map<CombatDamageType, Integer> damageReduction) {
		this.damageReduction = damageReduction;
	}
}
