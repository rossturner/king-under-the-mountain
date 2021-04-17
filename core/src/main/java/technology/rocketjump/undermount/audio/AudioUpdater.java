package technology.rocketjump.undermount.audio;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.logging.CrashHandler;

@Singleton
public class AudioUpdater {

	private final MusicJukebox musicJukebox;
	private final SoundEffectManager soundEffectManager;
	private final AudioMessageHandler audioMessageHandler; // Needs instantiating

	@Inject
	public AudioUpdater(MusicJukebox musicJukebox, SoundEffectManager soundEffectManager, AudioMessageHandler audioMessageHandler) {
		this.musicJukebox = musicJukebox;
		this.soundEffectManager = soundEffectManager;
		this.audioMessageHandler = audioMessageHandler;
	}

	public void update() {
		try {
			musicJukebox.update();
			soundEffectManager.update();
		} catch (Exception e) {
			CrashHandler.logCrash(e);
			Logger.error(e);
		}
	}

}
