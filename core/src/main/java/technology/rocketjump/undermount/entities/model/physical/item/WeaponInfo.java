package technology.rocketjump.undermount.entities.model.physical.item;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.entities.model.physical.combat.CombatDamageType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeaponInfo {

	private int range;
	private CombatDamageType damageType;
	private boolean modifiedByStrength;
	private int minDamage;
	private int maxDamage;
	private AmmoType requiresAmmoType;

	public int getRange() {
		return range;
	}

	public void setRange(int range) {
		this.range = range;
	}

	public CombatDamageType getDamageType() {
		return damageType;
	}

	public void setDamageType(CombatDamageType damageType) {
		this.damageType = damageType;
	}

	public boolean isModifiedByStrength() {
		return modifiedByStrength;
	}

	public void setModifiedByStrength(boolean modifiedByStrength) {
		this.modifiedByStrength = modifiedByStrength;
	}

	public int getMinDamage() {
		return minDamage;
	}

	public void setMinDamage(int minDamage) {
		this.minDamage = minDamage;
	}

	public int getMaxDamage() {
		return maxDamage;
	}

	public void setMaxDamage(int maxDamage) {
		this.maxDamage = maxDamage;
	}

	public AmmoType getRequiresAmmoType() {
		return requiresAmmoType;
	}

	public void setRequiresAmmoType(AmmoType requiresAmmoType) {
		this.requiresAmmoType = requiresAmmoType;
	}
}
