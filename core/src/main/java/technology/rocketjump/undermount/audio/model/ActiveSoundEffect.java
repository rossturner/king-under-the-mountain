package technology.rocketjump.undermount.audio.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALSound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import org.pmw.tinylog.Logger;

import java.util.Random;

public class ActiveSoundEffect implements Disposable {

	private final Vector2 worldPosition;
	private final SoundAsset asset;
	private final OpenALSound resource; // or perhaps sounds ID
	private final Random random;
	private final long parentEntityId;
	private float playedDuration;
	private Long soundId; // LibGDX sound ID
	private boolean paused;
	private float pitch;
	private float currentVolume;
	private boolean isStopped;
	private float totalElapsedTime;

	public ActiveSoundEffect(SoundAsset asset, long parentEntityId, Vector2 worldPosition) throws GdxAudioException {
		if (Gdx.audio == null) {
			throw new GdxAudioException();
		}
		this.parentEntityId = parentEntityId;
		this.worldPosition = worldPosition;
		this.asset = asset;

		random = new RandomXS128(parentEntityId);
		String filename = asset.getFilenames().get(random.nextInt(asset.getFilenames().size()));
		this.resource = (OpenALSound) Gdx.audio.newSound(new FileHandle(filename));
	}

	public void play() {
		if (soundId == null) {
			soundId = resource.play();
			currentVolume = 1f;
			pitch = asset.getMinPitch() + (random.nextFloat() * (asset.getMaxPitch() - asset.getMinPitch()));
			resource.setPitch(soundId, pitch);
		} else {
			Logger.error("Already playing sound " + asset.getName());
		}
	}

	public void loop(float volume) {
		if (asset.isLooping()) {
			if (soundId == null) {
				soundId = resource.loop(volume);
				currentVolume = volume;
				pitch = asset.getMinPitch() + (random.nextFloat() * (asset.getMaxPitch() - asset.getMinPitch()));
				resource.setPitch(soundId, pitch);
			}
		} else {
			Logger.error(asset.getName() + " is not loopable");
		}
	}

	public void update(float deltaTime) {
		playedDuration += deltaTime;
	}

	public void pause() {
		if (!paused) {
			resource.pause(soundId);
			paused = true;
		}
	}

	public void resume() {
		if (paused) {
			resource.resume(soundId);
			paused = false;
		}
	}

	public void stop() {
		resource.stop(soundId);
		isStopped = true;
	}

	public boolean completed() {
		return (playedDuration > resource.duration()) || (isStopped);
	}

	public long getParentEntityId() {
		return parentEntityId;
	}

	public Vector2 getWorldPosition() {
		return worldPosition;
	}

	public void setVolume(float volume) {
		currentVolume = volume;
		resource.setVolume(soundId, volume * asset.getVolumeModifier());
	}

	public float getVolume() {
		return currentVolume;
	}

	@Override
	public void dispose() {
		resource.dispose();
	}

	public float getPitch() {
		return pitch;
	}

	public SoundAsset getAsset() {
		return asset;
	}

	public void incrementElapsedTime(float elapsed) {
		this.totalElapsedTime += elapsed;
	}

	public float getTotalElapsedTime() {
		return totalElapsedTime;
	}
}
