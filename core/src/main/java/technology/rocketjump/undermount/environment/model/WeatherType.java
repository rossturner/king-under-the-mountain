package technology.rocketjump.undermount.environment.model;

import com.badlogic.gdx.graphics.Color;
import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent;
import technology.rocketjump.undermount.misc.Name;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class WeatherType {

	private static final Double DEFAULT_SNOW_ACCUMULATION = -0.08;

	@Name
	private String name;
	private String i18nKey;
	private String particleEffectTypeName;
	@JsonIgnore
	private ParticleEffectType particleEffectType;

	private String maxSunlight;
	@JsonIgnore
	private Color maxSunlightColor = Color.WHITE;

	private String dayAmbienceSoundAssetName;
	@JsonIgnore
	private SoundAsset dayAmbienceSoundAsset;
	private String nightAmbienceSoundAssetName;
	@JsonIgnore
	private SoundAsset nightAmbienceSoundAsset;

	private boolean oxidises;
	private Float chanceToExtinguishFire;
	private Float chanceToFreezeToDeathFromSleeping;
	private Double lightningStrikesPerHour;
	private Double accumulatesSnowPerHour = DEFAULT_SNOW_ACCUMULATION;

	private Map<HappinessInteraction, HappinessComponent.HappinessModifier> happinessModifiers = new EnumMap<>(HappinessInteraction.class);

	public Double getLightningStrikesPerHour() {
		return lightningStrikesPerHour;
	}

	public void setLightningStrikesPerHour(Double lightningStrikesPerHour) {
		this.lightningStrikesPerHour = lightningStrikesPerHour;
	}

	public enum HappinessInteraction {

		STANDING,
		WORKING,
		SLEEPING

	}

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

	public Float getChanceToFreezeToDeathFromSleeping() {
		return chanceToFreezeToDeathFromSleeping;
	}

	public void setChanceToFreezeToDeathFromSleeping(Float chanceToFreezeToDeathFromSleeping) {
		this.chanceToFreezeToDeathFromSleeping = chanceToFreezeToDeathFromSleeping;
	}

	public Map<HappinessInteraction, HappinessComponent.HappinessModifier> getHappinessModifiers() {
		return happinessModifiers;
	}

	public void setHappinessModifiers(Map<HappinessInteraction, HappinessComponent.HappinessModifier> happinessModifiers) {
		this.happinessModifiers = happinessModifiers;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public void setI18nKey(String i18nKey) {
		this.i18nKey = i18nKey;
	}

	public String getDayAmbienceSoundAssetName() {
		return dayAmbienceSoundAssetName;
	}

	public void setDayAmbienceSoundAssetName(String dayAmbienceSoundAssetName) {
		this.dayAmbienceSoundAssetName = dayAmbienceSoundAssetName;
	}

	public SoundAsset getDayAmbienceSoundAsset() {
		return dayAmbienceSoundAsset;
	}

	public void setDayAmbienceSoundAsset(SoundAsset dayAmbienceSoundAsset) {
		this.dayAmbienceSoundAsset = dayAmbienceSoundAsset;
	}

	public String getNightAmbienceSoundAssetName() {
		return nightAmbienceSoundAssetName;
	}

	public void setNightAmbienceSoundAssetName(String nightAmbienceSoundAssetName) {
		this.nightAmbienceSoundAssetName = nightAmbienceSoundAssetName;
	}

	public SoundAsset getNightAmbienceSoundAsset() {
		return nightAmbienceSoundAsset;
	}

	public void setNightAmbienceSoundAsset(SoundAsset nightAmbienceSoundAsset) {
		this.nightAmbienceSoundAsset = nightAmbienceSoundAsset;
	}

	public Double getAccumulatesSnowPerHour() {
		return accumulatesSnowPerHour;
	}

	public void setAccumulatesSnowPerHour(Double accumulatesSnowPerHour) {
		this.accumulatesSnowPerHour = accumulatesSnowPerHour;
	}

	@Override
	public String toString() {
		return name;
	}
}
