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

import java.nio.charset.Charset;

import static technology.rocketjump.undermount.persistence.UserPreferences.FullscreenMode.BORDERLESS_FULLSCREEN;
import static technology.rocketjump.undermount.persistence.UserPreferences.FullscreenMode.EXCLUSIVE_FULLSCREEN;
import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.DISPLAY_RESOLUTION;
import static technology.rocketjump.undermount.screens.menus.options.GraphicsOptionsTab.getFullscreenMode;

public class DesktopLauncher {

    public static void main(String[] args) {
        try {
            checkDefaultCharset();
            launchMainWindow();
        } catch (Throwable e) {
            Logger.error(e);
            CrashHandler.logCrash(e);
            System.exit(-1);
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

        UserPreferences.FullscreenMode fullscreenMode = getFullscreenMode(userPreferences);

        if (fullscreenMode.equals(BORDERLESS_FULLSCREEN)) {
            System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
        }
        config.fullscreen = fullscreenMode.equals(EXCLUSIVE_FULLSCREEN);


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

	private static void checkDefaultCharset() {
        Logger.info("Default character set is " + Charset.defaultCharset().name());
	}

}
