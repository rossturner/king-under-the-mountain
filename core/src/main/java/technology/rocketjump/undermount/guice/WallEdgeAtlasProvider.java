package technology.rocketjump.undermount.guice;

import com.badlogic.gdx.Gdx;
import com.google.inject.Provider;
import technology.rocketjump.undermount.mapping.tile.wall.WallEdgeAtlas;

import java.io.IOException;

public class WallEdgeAtlasProvider implements Provider<WallEdgeAtlas> {
	@Override
	public WallEdgeAtlas get() {
		try {
			return new WallEdgeAtlas(
					Gdx.files.internal("assets/terrain/wallEdges.json").file(),
					Gdx.files.internal("assets/terrain/doorwayClosedEdges.json").file(),
					Gdx.files.internal("assets/terrain/doorwayEdges.json").file()
					);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
