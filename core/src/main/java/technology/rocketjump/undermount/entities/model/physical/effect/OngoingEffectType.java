package technology.rocketjump.undermount.entities.model.physical.effect;

import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.entities.behaviour.effects.BaseOngoingEffectBehaviour;
import technology.rocketjump.undermount.entities.tags.Tag;
import technology.rocketjump.undermount.misc.Name;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;

import java.util.*;

public class OngoingEffectType {

	@Name
	private String name;

	private String particleEffectTypeName;
	@JsonIgnore
	private ParticleEffectType particleEffectType;

	private String initialColor;

	private Map<BaseOngoingEffectBehaviour.OngoingEffectState, EffectStateConfig> states = new HashMap<>();

	private String playSoundAssetName;
	@JsonIgnore
	private SoundAsset playSoundAsset;

	private Map<String, List<String>> tags = new HashMap<>();
	@JsonIgnore
	private List<Tag> processedTags = new ArrayList<>();

	private String triggersNotification;

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

	public Map<BaseOngoingEffectBehaviour.OngoingEffectState, EffectStateConfig> getStates() {
		return states;
	}

	public void setStates(Map<BaseOngoingEffectBehaviour.OngoingEffectState, EffectStateConfig> states) {
		this.states = states;
	}

	public String getPlaySoundAssetName() {
		return playSoundAssetName;
	}

	public void setPlaySoundAssetName(String playSoundAssetName) {
		this.playSoundAssetName = playSoundAssetName;
	}

	public SoundAsset getPlaySoundAsset() {
		return playSoundAsset;
	}

	public void setPlaySoundAsset(SoundAsset playSoundAsset) {
		this.playSoundAsset = playSoundAsset;
	}

	public Map<String, List<String>> getTags() {
		return tags;
	}

	public void setTags(Map<String, List<String>> tags) {
		this.tags = tags;
	}

	public void setProcessedTags(List<Tag> processedTags) {
		this.processedTags = processedTags;
	}

	public List<Tag> getProcessedTags() {
		return processedTags;
	}

	public String getInitialColor() {
		return initialColor;
	}

	public void setInitialColor(String initialColor) {
		this.initialColor = initialColor;
	}

	public String getTriggersNotification() {
		return triggersNotification;
	}

	public void setTriggersNotification(String triggersNotification) {
		this.triggersNotification = triggersNotification;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		OngoingEffectType that = (OngoingEffectType) o;
		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}
