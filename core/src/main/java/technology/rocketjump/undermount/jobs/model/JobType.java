package technology.rocketjump.undermount.jobs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.entities.ai.goap.SpecialGoal;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.misc.Name;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobType {

	@Name
	private String name;
	private String overrideI18nKey;
	private boolean isAccessedFromAdjacentTile;
	private boolean removeJobWhenAssignmentCancelled;
	private boolean haulItemWhileWorking;
	private boolean requiresWeapon;
	private SpecialGoal switchToSpecialGoal;
	private Float defaultTimeToCompleteJob;
	private Float mightStartFire;

	private String requiredProfessionName;
	@JsonIgnore
	private Profession requiredProfession;

	private String requiredItemTypeName;
	@JsonIgnore
	private ItemType requiredItemType;

	private String activeSoundAssetName;
	@JsonIgnore
	private SoundAsset activeSoundAsset;

	private String onCompletionSoundAssetName;
	@JsonIgnore
	private SoundAsset onCompletionSoundAsset;

	private List<String> workOnJobParticleEffectNames;
	@JsonIgnore
	private List<ParticleEffectType> workOnJobParticleEffectTypes = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOverrideI18nKey() {
		return overrideI18nKey;
	}

	public void setOverrideI18nKey(String overrideI18nKey) {
		this.overrideI18nKey = overrideI18nKey;
	}

	public String getRequiredProfessionName() {
		return requiredProfessionName;
	}

	public void setRequiredProfessionName(String requiredProfessionName) {
		this.requiredProfessionName = requiredProfessionName;
	}

	public String getRequiredItemTypeName() {
		return requiredItemTypeName;
	}

	public void setRequiredItemTypeName(String requiredItemTypeName) {
		this.requiredItemTypeName = requiredItemTypeName;
	}

	public boolean isAccessedFromAdjacentTile() {
		return isAccessedFromAdjacentTile;
	}

	public void setAccessedFromAdjacentTile(boolean accessedFromAdjacentTile) {
		isAccessedFromAdjacentTile = accessedFromAdjacentTile;
	}

	public boolean isRemoveJobWhenAssignmentCancelled() {
		return removeJobWhenAssignmentCancelled;
	}

	public void setRemoveJobWhenAssignmentCancelled(boolean removeJobWhenAssignmentCancelled) {
		this.removeJobWhenAssignmentCancelled = removeJobWhenAssignmentCancelled;
	}

	public boolean isHaulItemWhileWorking() {
		return haulItemWhileWorking;
	}

	public void setHaulItemWhileWorking(boolean haulItemWhileWorking) {
		this.haulItemWhileWorking = haulItemWhileWorking;
	}

	public SpecialGoal getSwitchToSpecialGoal() {
		return switchToSpecialGoal;
	}

	public void setSwitchToSpecialGoal(SpecialGoal switchToSpecialGoal) {
		this.switchToSpecialGoal = switchToSpecialGoal;
	}

	public String getActiveSoundAssetName() {
		return activeSoundAssetName;
	}

	public void setActiveSoundAssetName(String activeSoundAssetName) {
		this.activeSoundAssetName = activeSoundAssetName;
	}

	public String getOnCompletionSoundAssetName() {
		return onCompletionSoundAssetName;
	}

	public void setOnCompletionSoundAssetName(String onCompletionSoundAssetName) {
		this.onCompletionSoundAssetName = onCompletionSoundAssetName;
	}

	public Profession getRequiredProfession() {
		return requiredProfession;
	}

	public void setRequiredProfession(Profession requiredProfession) {
		this.requiredProfession = requiredProfession;
	}

	public ItemType getRequiredItemType() {
		return requiredItemType;
	}

	public void setRequiredItemType(ItemType requiredItemType) {
		this.requiredItemType = requiredItemType;
	}

	public SoundAsset getActiveSoundAsset() {
		return activeSoundAsset;
	}

	public void setActiveSoundAsset(SoundAsset activeSoundAsset) {
		this.activeSoundAsset = activeSoundAsset;
	}

	public SoundAsset getOnCompletionSoundAsset() {
		return onCompletionSoundAsset;
	}

	public void setOnCompletionSoundAsset(SoundAsset onCompletionSoundAsset) {
		this.onCompletionSoundAsset = onCompletionSoundAsset;
	}

	public List<String> getWorkOnJobParticleEffectNames() {
		return workOnJobParticleEffectNames;
	}

	public void setWorkOnJobParticleEffectNames(List<String> workOnJobParticleEffectNames) {
		this.workOnJobParticleEffectNames = workOnJobParticleEffectNames;
	}

	public List<ParticleEffectType> getWorkOnJobParticleEffectTypes() {
		return workOnJobParticleEffectTypes;
	}

	public void setWorkOnJobParticleEffectTypes(List<ParticleEffectType> workOnJobParticleEffectTypes) {
		this.workOnJobParticleEffectTypes = workOnJobParticleEffectTypes;
	}

	@Override
	public String toString() {
		return name;
	}

	public Float getDefaultTimeToCompleteJob() {
		return defaultTimeToCompleteJob;
	}

	public void setDefaultTimeToCompleteJob(Float defaultTimeToCompleteJob) {
		this.defaultTimeToCompleteJob = defaultTimeToCompleteJob;
	}

	public Float getMightStartFire() {
		return mightStartFire;
	}

	public void setMightStartFire(Float mightStartFire) {
		this.mightStartFire = mightStartFire;
	}

	public boolean isRequiresWeapon() {
		return requiresWeapon;
	}

	public void setRequiresWeapon(boolean requiresWeapon) {
		this.requiresWeapon = requiresWeapon;
	}
}
