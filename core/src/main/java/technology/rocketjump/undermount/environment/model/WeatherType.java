package technology.rocketjump.undermount.environment.model;

import com.badlogic.gdx.graphics.Color;
import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent;
import technology.rocketjump.undermount.misc.Name;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;

import java.util.Map;
import java.util.Objects;

public class WeatherType {

	@Name
	private String name;
	private String i18nKey;
	private String particleEffectTypeName;
	@JsonIgnore
	private ParticleEffectType particleEffectType;

	private String maxSunlight;
	@JsonIgnore
	private Color maxSunlightColor = Color.WHITE;

	private boolean oxidises;
	private Float chanceToExtinguishFire;

	private Map<String, HappinessComponent.HappinessModifier> happinessModifiers;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		WeatherType that = (WeatherType) o;
		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getParticleEffectTypeName() {
		return particleEffectTypeName;
	}

	public void setParticleEffectTypeName(String particleEffectTypeName) {
		this.particleEffectTypeName = particleEffectTypeName;
	}

	public ParticleEffectType getParticleEffectType() {
		return particleEffectType;
	}

	public void setParticleEffectType(ParticleEffectType particleEffectType) {
		this.particleEffectType = particleEffectType;
	}

	public String getMaxSunlight() {
		return maxSunlight;
	}

	public void setMaxSunlight(String maxSunlight) {
		this.maxSunlight = maxSunlight;
	}

	public Color getMaxSunlightColor() {
		return maxSunlightColor;
	}

	public void setMaxSunlightColor(Color maxSunlightColor) {
		this.maxSunlightColor = maxSunlightColor;
	}

	public boolean isOxidises() {
		return oxidises;
	}

	public void setOxidises(boolean oxidises) {
		this.oxidises = oxidises;
	}

	public Float getChanceToExtinguishFire() {
		return chanceToExtinguishFire;
	}

	public void setChanceToExtinguishFire(Float chanceToExtinguishFire) {
		this.chanceToExtinguishFire = chanceToExtinguishFire;
	}

	public Map<String, HappinessComponent.HappinessModifier> getHappinessModifiers() {
		return happinessModifiers;
	}

	public void setHappinessModifiers(Map<String, HappinessComponent.HappinessModifier> happinessModifiers) {
		this.happinessModifiers = happinessModifiers;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public void setI18nKey(String i18nKey) {
		this.i18nKey = i18nKey;
	}
}
