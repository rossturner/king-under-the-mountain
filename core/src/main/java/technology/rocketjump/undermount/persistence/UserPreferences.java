package technology.rocketjump.undermount.persistence;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.ProvidedBy;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Singleton
@ProvidedBy(UserPreferencesProvider.class)
public class UserPreferences {

	private final File propertiesFile;
	private final Properties properties = new Properties();

	public static String preferencesJson; // for CrashHandler to use statically

	@Inject
	public UserPreferences(UserFileManager userFileManager) {
		propertiesFile = userFileManager.getOrCreateFile("preferences.properties");
		FileInputStream inputStream = null;
		try {
			inputStream = FileUtils.openInputStream(propertiesFile);
			properties.load(inputStream);
			preferencesJson = JSONObject.toJSONString(properties);
		} catch (IOException e) {
			Logger.error(e, "Failed to load " + propertiesFile.getAbsolutePath());
		} finally {
			if (inputStream != null) {
				IOUtils.closeQuietly(inputStream);
			}
		}
	}



	/**
	 * Best not to rename these as any existing saved preferences will be lost
	 */
	public enum PreferenceKey {

		MUSIC_VOLUME,
		AMBIENT_EFFECT_VOLUME,
		SOUND_EFFECT_VOLUME,
		UI_SCALE,
		SAVE_LOCATION,
		CRASH_REPORTING,
		LANGUAGE,
		DISPLAY_RESOLUTION,
		DISPLAY_FULLSCREEN,
		EDGE_SCROLLING,
		TREE_TRANSPARENCY,
		PAUSE_FOR_NOTIFICATIONS,
		ACTIVE_MODS,
		ALLOW_HINTS,
		DISABLE_TUTORIAL,
		MAIN_MENU_BACKGROUND_SCROLLING,

		TWITCH_TOKEN,
		TWITCH_INTEGRATION_ENABLED,
		TWITCH_VIEWERS_AS_SETTLER_NAMES,
		TWITCH_PRIORITISE_SUBSCRIBERS;

	}
	private static final List<PreferenceKey> ALWAYS_PERSIST_KEYS = Arrays.asList(PreferenceKey.SAVE_LOCATION,
			PreferenceKey.DISPLAY_FULLSCREEN);

	public String getPreference(PreferenceKey key, String defaultValue) {
		String property = properties.getProperty(key.name());
		if (property == null) {
			// Force saving of some preferences to always expose them for modification
			if (ALWAYS_PERSIST_KEYS.contains(key)) {
				setPreference(key, defaultValue);
			}
			return defaultValue;
		} else {
			return property;
		}
	}

	public void setPreference(PreferenceKey preferenceKey, String value) {
		properties.setProperty(preferenceKey.name(), value);
		persist();
	}

	public void removePreference(PreferenceKey preferenceKey) {
		properties.remove(preferenceKey.name());
		persist();
	}

	private void persist() {
		FileOutputStream outputStream = null;
		try {
			outputStream = FileUtils.openOutputStream(propertiesFile);
			properties.store(outputStream, "This is a list of user preferences, which is managed by the King under the Mountain game client");
		} catch (IOException e) {
			Logger.error(e, "Failed to load " + propertiesFile.getAbsolutePath());
		} finally {
			if (outputStream != null) {
				IOUtils.closeQuietly(outputStream);
			}
		}

		preferencesJson = JSONObject.toJSONString(properties);
	}

}
