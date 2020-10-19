package technology.rocketjump.undermount.launcher;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.UndermountLwjglApplication;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.UndermountApplicationAdapter;
import technology.rocketjump.undermount.guice.UndermountGuiceModule;
import technology.rocketjump.undermount.logging.CrashHandler;
import technology.rocketjump.undermount.modding.LocalModRepository;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.rendering.camera.DisplaySettings;
import technology.rocketjump.undermount.screens.menus.Resolution;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.DISPLAY_FULLSCREEN;
import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.DISPLAY_RESOLUTION;

public class DesktopLauncher {

    public static void main(String[] args) throws Exception {
        try {
            forceUtf8();
            launchMainWindow();
        } catch (Exception e) {
            Logger.error(e);
            CrashHandler.logCrash(e);
            throw e;
        }
    }

    private static void launchMainWindow() {
        // config for main window


        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

        config.title = "King under the Mountain";

        Injector preInjector = Guice.createInjector(new UndermountGuiceModule());
        UserPreferences userPreferences = preInjector.getInstance(UserPreferences.class);

        LocalModRepository localModRepository = preInjector.getInstance(LocalModRepository.class);
        localModRepository.packageActiveMods();

        config.fullscreen = Boolean.valueOf(userPreferences.getPreference(DISPLAY_FULLSCREEN, "true"));

        Resolution displayResolution = getDisplayResolution(userPreferences);
        config.width = displayResolution.width;
        config.height = displayResolution.height;

        config.addIcon("assets/icon/undermount-icon-128x128.png", Files.FileType.Internal);
        config.addIcon("assets/icon/undermount-icon-32x32.png", Files.FileType.Internal);
        config.addIcon("assets/icon/undermount-icon-16x16.png", Files.FileType.Internal);

        UndermountApplicationAdapter gameInstance = new UndermountApplicationAdapter();
        new UndermountLwjglApplication(gameInstance, config);
    }

    private static Resolution getDisplayResolution(UserPreferences userPreferences) {
        Graphics.DisplayMode desktopMode = LwjglApplicationConfiguration.getDesktopDisplayMode();
        Resolution desktopResolution = new Resolution(desktopMode.width, desktopMode.height);
        String preferredResolution = userPreferences.getPreference(DISPLAY_RESOLUTION, null);
        Resolution resolutionToUse;
        if (preferredResolution == null) {
            userPreferences.setPreference(DISPLAY_RESOLUTION, desktopResolution.toString());
            resolutionToUse = desktopResolution;
        } else {
            try {
                resolutionToUse = Resolution.byString(preferredResolution);
            } catch (NumberFormatException e) {
                Logger.error("Could not parse " + DISPLAY_RESOLUTION.name() + " preference: " + preferredResolution);
                resolutionToUse = desktopResolution;
            }
        }
        DisplaySettings.currentResolution = resolutionToUse;
        return resolutionToUse;
    }

	private static void forceUtf8() {
		try {
			disableAccessWarnings();
			System.setProperty("file.encoding", "UTF-8");
			Field charset = Charset.class.getDeclaredField("defaultCharset");
			charset.setAccessible(true);
			charset.set(null, null);
			Charset.defaultCharset();
		} catch (Exception e) {
			Logger.error("Exception while attempting to force to UTF-8", e);
		}
	}

    @SuppressWarnings("unchecked")
    public static void disableAccessWarnings() {
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);

            Method putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
            Method staticFieldOffset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);

            Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field loggerField = loggerClass.getDeclaredField("logger");
            Long offset = (Long) staticFieldOffset.invoke(unsafe, loggerField);
            putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
        } catch (Exception ignored) {
        	// Do nothing
        }
    }
}
