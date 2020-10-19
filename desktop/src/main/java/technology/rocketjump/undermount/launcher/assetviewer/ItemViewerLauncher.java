package technology.rocketjump.undermount.launcher.assetviewer;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.google.inject.Guice;
import technology.rocketjump.undermount.AssetsPackager;
import technology.rocketjump.undermount.assets.viewer.ItemViewerApplication;

import java.io.IOException;

public class ItemViewerLauncher {

	public static void main(String[] arg) throws IOException {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "Item Viewer";
		config.width = 1280;
		config.height = 900;

		// On launch repackage assets into relevant folders
		AssetsPackager.main();

		ItemViewerApplication gameInstance = Guice.createInjector().getInstance(ItemViewerApplication.class);
		new LwjglApplication(gameInstance, config);
	}

}
