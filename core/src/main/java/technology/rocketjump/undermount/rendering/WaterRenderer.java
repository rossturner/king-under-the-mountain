package technology.rocketjump.undermount.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import technology.rocketjump.undermount.assets.AssetDisposable;
import technology.rocketjump.undermount.assets.ChannelTypeDictionary;
import technology.rocketjump.undermount.assets.model.ChannelType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.MapVertex;
import technology.rocketjump.undermount.mapping.tile.underground.ChannelLayout;
import technology.rocketjump.undermount.mapping.tile.underground.TileLiquidFlow;
import technology.rocketjump.undermount.rendering.custom_libgdx.ChanneledWaterSpriteBatch;
import technology.rocketjump.undermount.rendering.custom_libgdx.FlowingWaterSpriteBatch;
import technology.rocketjump.undermount.sprites.DiffuseTerrainSpriteCacheProvider;
import technology.rocketjump.undermount.sprites.TerrainSpriteCache;
import technology.rocketjump.undermount.sprites.model.QuadrantSprites;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class WaterRenderer implements GameContextAware, AssetDisposable {

	private final ChannelTypeDictionary channelTypeDictionary;
	private final TerrainSpriteCache diffuseTerrainSpriteCache;
	private final FlowingWaterSpriteBatch flowingWaterSpriteBatch = new FlowingWaterSpriteBatch();
	private final ChanneledWaterSpriteBatch channeledWaterSpriteBatch = new ChanneledWaterSpriteBatch();
	private final Sprite waterTexture;
	private final Sprite waterNormalTexture;
	private final Sprite waveTexture;
	private final QuadrantSprites noMaskSprites;
	private final ChannelType channelMask;

	private float elapsedSeconds = 0f;
	private GameContext gameContext;
	private boolean elapsedTimeUpdatedThisFrame;

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

	public void renderChannels(List<MapTile> terrainTiles, Camera camera, RenderMode renderMode) {
		channeledWaterSpriteBatch.setProjectionMatrix(camera.combined);
		channeledWaterSpriteBatch.setElapsedTime(elapsedSeconds);
		channeledWaterSpriteBatch.begin();
		for (MapTile terrainTile : terrainTiles) {
			if (terrainTile.hasChannel()) {
				TileLiquidFlow liquidFlow = terrainTile.getUnderTile().getLiquidFlow();
				if (liquidFlow != null && liquidFlow.getLiquidAmount() > 0) {
					renderChannelLiquid(terrainTile, renderMode);
				}
			}
		}
		channeledWaterSpriteBatch.end();
	}

	private void renderChannelLiquid(MapTile terrainTile, RenderMode renderMode) {
		Sprite waterTexture = this.waterTexture;
		if (renderMode == RenderMode.NORMALS) {
			waterTexture = this.waterNormalTexture;
		}

		ChannelLayout channelLayout = terrainTile.getUnderTile().getChannelLayout();
		QuadrantSprites quadrantMasks = diffuseTerrainSpriteCache.getSpritesForChannel(channelMask, channelLayout, terrainTile.getSeed());

		MapVertex[] tileVertices = gameContext.getAreaMap().getVertices(terrainTile.getTileX(), terrainTile.getTileY());

		float offsetX, offsetY;
/*
			Drawing 4 channel quadrants a, b, c, d where

			A | B
			-----
			C | D
			*/

		// Quadrant A
		offsetX = 0f;
		offsetY = 0.5f;
		channeledWaterSpriteBatch.draw(waterTexture, waveTexture, quadrantMasks.getA(),
				terrainTile.getTileX(), terrainTile.getTileY(),
				offsetX, offsetY,
				0.5f, 0.5f,
				tileVertices);

		// Quadrant B
		offsetX = 0.5f;
		offsetY = 0.5f;
		channeledWaterSpriteBatch.draw(waterTexture, waveTexture, quadrantMasks.getA(),
				terrainTile.getTileX(), terrainTile.getTileY(),
				offsetX, offsetY,
				0.5f, 0.5f,
				tileVertices);

		// Quadrant C
		offsetX = 0f;
		offsetY = 0f;
		channeledWaterSpriteBatch.draw(waterTexture, waveTexture, quadrantMasks.getA(),
				terrainTile.getTileX(), terrainTile.getTileY(),
				offsetX, offsetY,
				0.5f, 0.5f,
				tileVertices);

		// Quadrant D
		offsetX = 0.5f;
		offsetY = 0f;
		channeledWaterSpriteBatch.draw(waterTexture, waveTexture, quadrantMasks.getA(),
				terrainTile.getTileX(), terrainTile.getTileY(),
				offsetX, offsetY,
				0.5f, 0.5f,
				tileVertices);
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
