package technology.rocketjump.undermount.audio;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.audio.Music;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import technology.rocketjump.undermount.persistence.UserPreferences;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MusicJukeboxTest {
	@Mock
	private MessageDispatcher messageDispatcher;

	@Mock
	private UserPreferences userPreferences;

	@Mock
	private Audio audio;

	@Mock
	private Music music;

	private MusicJukebox musicJukebox;

	@Before
	public void setUp() throws Exception {
		// Returns whatever the provided default value is
		when(userPreferences.getPreference(any(), any())).then((Answer<String>) invocationOnMock -> invocationOnMock.getArgument(1));
		when(audio.newMusic(any())).thenReturn(music);
		Gdx.audio = audio;

		musicJukebox = new MusicJukebox(messageDispatcher, userPreferences);
	}

	@After
	public void tearDown() throws Exception {
		musicJukebox.dispose();
		Gdx.audio = null;
	}

	@Test
	public void pauseMusic_PausesIfCurrentTrackAvailable() {
		// Initialize and start the track we need to pause.
		musicJukebox.update();

		musicJukebox.pauseMusic();

		verify(music).pause();
	}

	@Test
	public void resumeMusic_PausesIfCurrentTrackAvailable() {
		// Initialize and start the track we need to resume.
		musicJukebox.update();

		musicJukebox.resumeMusic();

		// The update() method calls play too actually, so we verify play is called *twice*.
		verify(music, times(2)).play();
	}
}