package technology.rocketjump.undermount.launcher.assetviewer;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.google.inject.Guice;
import technology.rocketjump.undermount.AssetsPackager;
import technology.rocketjump.undermount.assets.viewer.MechanismViewerApplication;

import java.io.IOException;

public class MechanismViewerLauncher {

	public static void main(String[] arg) throws IOException {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "Mechanism Viewer";
        config.width = 1400;
        config.height = 900;

		// On launch repackage assets into relevant folders
		AssetsPackager.main();

		MechanismViewerApplication gameInstance = Guice.createInjector().getInstance(MechanismViewerApplication.class);
        new LwjglApplication(gameInstance, config);
	}

}
