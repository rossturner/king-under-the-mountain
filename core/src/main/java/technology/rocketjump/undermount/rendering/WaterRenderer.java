package technology.rocketjump.undermount.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import technology.rocketjump.undermount.assets.AssetDisposable;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.rendering.custom_libgdx.FlowingWaterSpriteBatch;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class WaterRenderer implements GameContextAware, AssetDisposable {

	private final FlowingWaterSpriteBatch batch = new FlowingWaterSpriteBatch();
	private final Sprite waterTexture;
	private final Sprite waterNormalTexture;
	private final Sprite waveTexture;

	private float elapsedSeconds = 0f;
	private GameContext gameContext;

	@Inject
	public WaterRenderer() {
		waterTexture = new Sprite(new Texture("assets/water/water.png")); // TODO move and make these modable
		waterNormalTexture = new Sprite(new Texture("assets/water/water_NORMALS.png")); // TODO move and make these modable
		waveTexture = new Sprite(new Texture("assets/water/wave_mask.png")); // TODO move and make these moddable
	}

	public void render(TiledMap map, List<MapTile> riverTiles, Camera camera, RenderMode renderMode) {
		Sprite waterTexture = this.waterTexture;
		if (renderMode == RenderMode.NORMALS) {
			waterTexture = this.waterNormalTexture;
		}
		if (gameContext != null) {
			if (!gameContext.getGameClock().isPaused()) {
				elapsedSeconds += Gdx.graphics.getDeltaTime() * gameContext.getGameClock().getSpeedMultiplier();
			}
		}
		batch.setProjectionMatrix(camera.combined);
		batch.setElapsedTime(elapsedSeconds);
		batch.begin();

		for (MapTile waterTile : riverTiles) {
			batch.draw(waterTexture, waveTexture, waterTile.getTileX(), waterTile.getTileY(), 1f, 1f, map.getVertices(waterTile.getTileX(), waterTile.getTileY()));
		}

		batch.end();
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		elapsedSeconds = 0;
	}

	@Override
	public void dispose() {
		batch.dispose();
		waterTexture.getTexture().dispose();
		waterNormalTexture.getTexture().dispose();
		waveTexture.getTexture().dispose();
	}
}
