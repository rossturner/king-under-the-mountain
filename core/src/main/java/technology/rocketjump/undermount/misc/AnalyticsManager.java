package technology.rocketjump.undermount.misc;

import com.badlogic.gdx.Gdx;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;

import java.util.concurrent.atomic.AtomicBoolean;

public class AnalyticsManager {

	private static final long PERIOD_IN_SECONDS = 60;

	private static final GoogleAnalytics ga;
	private static String clientId = "Unknown";
	public static String languageCode = "en-gb";
	private static AnalyticsThread thread;

	static {
		ga = GoogleAnalytics.builder()
				.withTrackingId("UA-82195631-5")
				.build();
	}

	public static void startAnalytics(String clientId) {
		AnalyticsManager.clientId = clientId;
		thread = new AnalyticsThread();
		thread.start();
	}

	public static void stopAnalytics() {
		thread.requestStop();
		thread.interrupt();
	}

	public static class AnalyticsThread extends Thread {

		private AtomicBoolean running = new AtomicBoolean(true);

		@Override
		public void run() {
			while (running.get()) {
				postAnalyticsInfo();
				try {
					Thread.sleep(1000L * PERIOD_IN_SECONDS);
				} catch (InterruptedException e) {
					break;
				}
			}
		}

		public void requestStop() {
			running.set(false);
		}
	}


	private static void postAnalyticsInfo() {
		try {
			ga.pageView("http://client.kingunderthemounta.in", "Main")
					.applicationName("King under the Mountain")
					.applicationVersion(GlobalSettings.VERSION.toString())
					.clientId(clientId)
					.screenResolution(Gdx.graphics.getDisplayMode(Gdx.graphics.getMonitor()).toString())
					.userLanguage(languageCode)
					.send();
		} catch (Exception e) {
			// Suppress any tracking-related exceptions outside of dev mode
			if (GlobalSettings.DEV_MODE) {
				Logger.error(e);
			}
		}
	}

}
