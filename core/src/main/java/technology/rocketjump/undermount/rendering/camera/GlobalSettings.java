package technology.rocketjump.undermount.rendering.camera;

import com.google.common.io.Resources;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.misc.versioning.Version;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

public class GlobalSettings {
	public static final boolean MAP_REVEALED = false;
	public static final boolean DEV_MODE = false;

	public static boolean USE_EDGE_SCROLLING = true;
	public static boolean PAUSE_FOR_NOTIFICATIONS = true;
	public static boolean TREE_TRANSPARENCY_ENABLED = true;
	public static final Version VERSION;

	static {
		String loadedVersion = "UNKNOWN 0";
		try {
			URL resourceUrl = Resources.getResource("version.txt");
			loadedVersion = Resources.toString(resourceUrl, Charset.defaultCharset());
		} catch (IOException e) {
			Logger.error(e);
		} finally {
			VERSION = new Version(loadedVersion);
		}
	}
}
