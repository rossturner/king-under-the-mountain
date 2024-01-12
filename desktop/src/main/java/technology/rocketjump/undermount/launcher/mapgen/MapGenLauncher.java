package technology.rocketjump.undermount.launcher.mapgen;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import technology.rocketjump.undermount.mapgen.MapGenApplicationAdapter;

public class MapGenLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 1280;
		config.height = 720;

		new LwjglApplication(new MapGenApplicationAdapter(), config);
	}
}
