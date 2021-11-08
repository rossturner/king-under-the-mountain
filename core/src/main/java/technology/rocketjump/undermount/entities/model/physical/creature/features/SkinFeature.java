package technology.rocketjump.undermount.entities.model.physical.creature.features;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.entities.model.physical.combat.CombatDamageType;
import technology.rocketjump.undermount.materials.model.GameMaterial;

import java.util.EnumMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SkinFeature {

	private String skinMaterialName;
	@JsonIgnore
	private GameMaterial skinMaterial;
	private Map<CombatDamageType, Integer> damageReduction = new EnumMap<>(CombatDamageType.class);

	public Map<CombatDamageType, Integer> getDamageReduction() {
		return damageReduction;
	}

	public String getSkinMaterialName() {
		return skinMaterialName;
	}

	public void setSkinMaterialName(String skinMaterialName) {
		this.skinMaterialName = skinMaterialName;
	}

	public GameMaterial getSkinMaterial() {
		return skinMaterial;
	}

	public void setSkinMaterial(GameMaterial skinMaterial) {
		this.skinMaterial = skinMaterial;
	}

	public void setDamageReduction(Map<CombatDamageType, Integer> damageReduction) {
		this.damageReduction = damageReduction;
	}
}
