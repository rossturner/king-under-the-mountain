package technology.rocketjump.undermount.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import technology.rocketjump.undermount.assets.AssetDisposable;
import technology.rocketjump.undermount.assets.ChannelTypeDictionary;
import technology.rocketjump.undermount.assets.model.ChannelType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.underground.ChannelLayout;
import technology.rocketjump.undermount.rendering.custom_libgdx.FlowingWaterSpriteBatch;
import technology.rocketjump.undermount.sprites.DiffuseTerrainSpriteCacheProvider;
import technology.rocketjump.undermount.sprites.TerrainSpriteCache;
import technology.rocketjump.undermount.sprites.model.QuadrantSprites;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
public class WaterRenderer implements GameContextAware, AssetDisposable {

	private final ChannelTypeDictionary channelTypeDictionary;
	private final TerrainSpriteCache diffuseTerrainSpriteCache;
	private final FlowingWaterSpriteBatch flowingWaterSpriteBatch = new FlowingWaterSpriteBatch();
	private final Sprite waterTexture;
	private final Sprite waterNormalTexture;
	private final Sprite waveTexture;
	private final QuadrantSprites noMaskSprites;
	private final ChannelType channelMask;

	private float elapsedSeconds = 0f;
	private GameContext gameContext;
	private boolean elapsedTimeUpdatedThisFrame;

	private static final Map<Integer, Color> depthMap = Map.of(
			1, new Color(1, 1, 1, 1f / 7f),
			2, new Color(1, 1, 1, 2f / 7f),
			3, new Color(1, 1, 1, 3f / 7f),
			4, new Color(1, 1, 1, 4f / 7f),
			5, new Color(1, 1, 1, 5f / 7f),
			6, new Color(1, 1, 1, 6f / 7f),
			7, Color.WHITE
	);

	@Inject
	public WaterRenderer(ChannelTypeDictionary channelTypeDictionary, DiffuseTerrainSpriteCacheProvider diffuseTerrainSpriteCacheProvider) {
		this.channelTypeDictionary = channelTypeDictionary;
		this.diffuseTerrainSpriteCache = diffuseTerrainSpriteCacheProvider.get();
		channelMask = channelTypeDictionary.getByName("channel_mask");
		noMaskSprites = diffuseTerrainSpriteCache.getSpritesForChannel(channelMask, new ChannelLayout(255), 0L);


		waterTexture = new Sprite(new Texture("assets/water/sprite/water.png")); // TODO move and make these modable
		waterNormalTexture = new Sprite(new Texture("assets/water/normal_sprite/water_NORMALS.png")); // TODO move and make these modable
		waveTexture = new Sprite(new Texture("assets/water/mask/wave_mask.png")); // TODO move and make these moddable
	}

	public void render(TiledMap map, List<MapTile> riverTiles, Camera camera, RenderMode renderMode) {
		Sprite waterTexture = this.waterTexture;
		if (renderMode == RenderMode.NORMALS) {
			waterTexture = this.waterNormalTexture;
		}
		flowingWaterSpriteBatch.setProjectionMatrix(camera.combined);
		flowingWaterSpriteBatch.setElapsedTime(elapsedSeconds);
		flowingWaterSpriteBatch.begin();

		for (MapTile waterTile : riverTiles) {
			flowingWaterSpriteBatch.draw(waterTexture, waveTexture, waterTile.getTileX(), waterTile.getTileY(), 1f, 1f, map.getVertices(waterTile.getTileX(), waterTile.getTileY()));
		}

		flowingWaterSpriteBatch.end();
	}

	public void updateElapsedTime() {
		if (gameContext != null) {
			if (!gameContext.getGameClock().isPaused()) {
				elapsedSeconds += Gdx.graphics.getDeltaTime() * gameContext.getGameClock().getSpeedMultiplier();
			}
		}
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
		flowingWaterSpriteBatch.dispose();
		waterTexture.getTexture().dispose();
		waterNormalTexture.getTexture().dispose();
		waveTexture.getTexture().dispose();
	}
}
