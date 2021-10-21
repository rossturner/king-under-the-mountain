package technology.rocketjump.undermount.entities.model.physical.item;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.entities.model.physical.combat.CombatDamageType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeaponInfo {

	private float range;
	private CombatDamageType damageType;
	private boolean modifiedByStrength;
	private int minDamage;
	private int maxDamage;
	private AmmoType requiresAmmoType;

	public static WeaponInfo UNARMED = new WeaponInfo();
	static {
		UNARMED.setRange(0.5f);
		UNARMED.setDamageType(CombatDamageType.CRUSHING);
		UNARMED.setModifiedByStrength(true);
		UNARMED.setMinDamage(0);
		UNARMED.setMaxDamage(2);
	}

	public float getRange() {
		return range;
	}

	public void setRange(float range) {
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
