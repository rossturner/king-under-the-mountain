package technology.rocketjump.undermount.audio.model;

import technology.rocketjump.undermount.misc.Name;
import technology.rocketjump.undermount.misc.SequentialId;

import java.util.ArrayList;
import java.util.List;

public class SoundAsset {

	@Name
	private String name;
	@SequentialId
	private long soundAssetId;
	private List<String> filenames = new ArrayList<>();
	private float minPitch = 1f;
	private float maxPitch;
	private float volumeModifier = 1f;
	private boolean looping;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getFilenames() {
		return filenames;
	}

	public void setFilenames(List<String> filenames) {
		this.filenames = filenames;
	}

	public float getMinPitch() {
		return minPitch;
	}

	public void setMinPitch(float minPitch) {
		this.minPitch = minPitch;
	}

	public float getMaxPitch() {
		return maxPitch;
	}

	public void setMaxPitch(float maxPitch) {
		this.maxPitch = maxPitch;
	}

	public long getSoundAssetId() {
		return soundAssetId;
	}

	public void setSoundAssetId(long soundAssetId) {
		this.soundAssetId = soundAssetId;
	}

	public boolean isLooping() {
		return looping;
	}

	public void setLooping(boolean looping) {
		this.looping = looping;
	}

	public float getVolumeModifier() {
		return volumeModifier;
	}

	public void setVolumeModifier(float volumeModifier) {
		this.volumeModifier = volumeModifier;
	}
}
