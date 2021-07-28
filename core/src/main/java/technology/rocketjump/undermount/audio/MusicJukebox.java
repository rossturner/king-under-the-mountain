package technology.rocketjump.undermount.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.AssetDisposable;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.persistence.UserPreferences;

import java.util.*;

import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.MUSIC_VOLUME;

@Singleton
public class MusicJukebox implements Telegraph, AssetDisposable {

	public static final String DEFAULT_VOLUME_AS_STRING = "0.24";
	private final UserPreferences userPreferences;
	private float volume;
	private boolean stopped;
	private Deque<FileHandle> playlist = new ArrayDeque<>();
	private List<FileHandle> musicFileList = new ArrayList<>();
	private Music currentTrack;
	private boolean shutdown;

	@Inject
	public MusicJukebox(MessageDispatcher messageDispatcher, UserPreferences userPreferences) {
		this.userPreferences = userPreferences;

		String volumeString = userPreferences.getPreference(UserPreferences.PreferenceKey.MUSIC_VOLUME, DEFAULT_VOLUME_AS_STRING);
		this.volume = SoundEffectManager.GLOBAL_VOLUME_MULTIPLIER * Float.valueOf(volumeString);
		if (this.volume < 0.01f) {
			this.stopped = true;
		}

		FileHandle musicDir = new FileHandle("assets/music");

		FileHandle mainMusic = null;
		for (FileHandle fileHandle : musicDir.list()) {
			if (fileHandle.extension().equals("ogg")) {
				musicFileList.add(fileHandle);
				if (fileHandle.name().contains("King under the Mountain")) {
					mainMusic = fileHandle;
				}
			}
		}


		if (mainMusic != null) {
			musicFileList.remove(mainMusic);
		}

		Collections.shuffle(musicFileList);
		if (mainMusic != null) {
			playlist.add(mainMusic);
		}
		playlist.addAll(musicFileList);

		messageDispatcher.addListener(this, MessageType.GUI_CHANGE_MUSIC_VOLUME);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.GUI_CHANGE_MUSIC_VOLUME: {
				Float newVolume = (Float)msg.extraInfo;
				this.volume = SoundEffectManager.GLOBAL_VOLUME_MULTIPLIER * newVolume;
				if (currentTrack != null) {
					currentTrack.setVolume(volume);
				}
				userPreferences.setPreference(MUSIC_VOLUME, String.valueOf(newVolume));

				if (this.stopped) {
					if (this.volume > 0.01f) {
						this.stopped = false;
					}
				} else {
					if (this.volume < 0.01f) {
						this.stopped = true;
					}
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	// Note: Not using Updatable interface as this should run on all screens
	public void update() {
		if (shutdown || stopped) {
			return;
		}

		if (currentTrack == null || !currentTrack.isPlaying()) {
			startNewTrack();
		}
	}

	private void startNewTrack() {
		if (currentTrack != null) {
			currentTrack.dispose();
		}
		if (playlist.isEmpty()) {
			playlist.addAll(musicFileList);
		}
		currentTrack = Gdx.audio.newMusic(playlist.pop());
		currentTrack.setVolume(volume);
		currentTrack.play();
	}

	@Override
	public void dispose() {
		if (currentTrack != null) {
			currentTrack.stop();
			currentTrack.dispose();
			currentTrack = null;
		}
		this.shutdown = true;
	}
}
