package technology.rocketjump.undermount.audio;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AudioUpdaterTest {
	@Mock
	private MusicJukebox musicJukebox;

	@Mock
	private SoundEffectManager soundEffectManager;

	@Mock
	private AudioMessageHandler audioMessageHandler;

	private AudioUpdater audioUpdater;

	@Before
	public void setUp() throws Exception {
		audioUpdater = new AudioUpdater(musicJukebox, soundEffectManager, audioMessageHandler);
	}

	@Test
	public void pause_pausesMusicJukebox() {
		audioUpdater.pause();

		verify(musicJukebox, only()).pauseMusic();
	}

	@Test
	public void resume_resumesMusicJukebox() {
		audioUpdater.resume();

		verify(musicJukebox, only()).resumeMusic();
	}
}