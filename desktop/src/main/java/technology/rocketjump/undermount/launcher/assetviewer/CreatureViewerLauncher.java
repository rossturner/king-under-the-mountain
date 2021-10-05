package technology.rocketjump.undermount.launcher.assetviewer;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.google.inject.Guice;
import technology.rocketjump.undermount.AssetsPackager;
import technology.rocketjump.undermount.assets.viewer.CreatureViewApplication;

import java.io.IOException;

public class CreatureViewerLauncher {

	public static void main(String[] arg) throws IOException {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "Creature Viewer";
        config.width = 1280;
        config.height = 900;

		// On launch repackage assets into relevant folders
		AssetsPackager.main();


		CreatureViewApplication gameInstance = Guice.createInjector().getInstance(CreatureViewApplication.class);
        new LwjglApplication(gameInstance, config);
	}

}
