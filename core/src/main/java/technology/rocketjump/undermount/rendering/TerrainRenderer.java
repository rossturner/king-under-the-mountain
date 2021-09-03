package technology.rocketjump.undermount.rendering;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Disposable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.ChannelTypeDictionary;
import technology.rocketjump.undermount.assets.model.ChannelType;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.floor.BridgeTile;
import technology.rocketjump.undermount.mapping.tile.floor.TileFloor;
import technology.rocketjump.undermount.mapping.tile.layout.TileLayout;
import technology.rocketjump.undermount.mapping.tile.underground.ChannelLayout;
import technology.rocketjump.undermount.mapping.tile.wall.Wall;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.rendering.custom_libgdx.AlphaMaskSpriteBatch;
import technology.rocketjump.undermount.rendering.custom_libgdx.VertexColorSpriteBatch;
import technology.rocketjump.undermount.rooms.Bridge;
import technology.rocketjump.undermount.rooms.constructions.BridgeConstruction;
import technology.rocketjump.undermount.rooms.constructions.Construction;
import technology.rocketjump.undermount.rooms.constructions.ConstructionType;
import technology.rocketjump.undermount.rooms.constructions.WallConstruction;
import technology.rocketjump.undermount.sprites.DiffuseTerrainSpriteCacheProvider;
import technology.rocketjump.undermount.sprites.TerrainSpriteCache;
import technology.rocketjump.undermount.sprites.model.BridgeTileLayout;
import technology.rocketjump.undermount.sprites.model.QuadrantSprites;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static technology.rocketjump.undermount.rendering.WorldRenderer.CONSTRUCTION_COLOR;

@Singleton
public class TerrainRenderer implements Disposable {

	private final float WALL_QUADRANT_MIDPOINT_X = 0.5f;
	private final float WALL_QUADRANT_MIDPOINT_Y = 50f / 64f;

	private final float TILE_WIDTH_HEIGHT = 1.0f;
	private final float QUADRANT_A_B_HEIGHT = 14f / 64f;
	private final float QUADRANT_C_D_HEIGHT = WALL_QUADRANT_MIDPOINT_Y;

	private final FloorOverlapRenderer floorOverlapRenderer;

	private final VertexColorSpriteBatch vertexColorSpriteBatch = new VertexColorSpriteBatch();
	private final AlphaMaskSpriteBatch alphaMaskSpriteBatch = new AlphaMaskSpriteBatch();
	private final WaterRenderer waterRenderer;
	private final TerrainSpriteCache diffuseTerrainSpriteCache;
	private ChannelType channelFloorType;
	private ChannelType channelEdgeType;
	private ChannelType channelMaskType;
	private GameMaterial dirtMaterial;

	@Inject
	public TerrainRenderer(FloorOverlapRenderer floorOverlapRenderer, WaterRenderer waterRenderer, ChannelTypeDictionary channelTypeDictionary,
						   GameMaterialDictionary gameMaterialDictionary, DiffuseTerrainSpriteCacheProvider diffuseTerrainSpriteCacheProvider) {
		this.floorOverlapRenderer = floorOverlapRenderer;
		this.waterRenderer = waterRenderer;
		channelFloorType = channelTypeDictionary.getByName("channel_floor");
		channelEdgeType = channelTypeDictionary.getByName("channel_edge");
		channelMaskType = channelTypeDictionary.getByName("channel_mask");
		this.dirtMaterial = gameMaterialDictionary.getByName("Dirt");
		this.diffuseTerrainSpriteCache = diffuseTerrainSpriteCacheProvider.get();
	}

	public void renderFloors(List<MapTile> mapTiles, Camera camera, TerrainSpriteCache spriteCache, RenderMode renderMode) {
		vertexColorSpriteBatch.setProjectionMatrix(camera.combined);
		vertexColorSpriteBatch.enableBlending();
		vertexColorSpriteBatch.begin();
		vertexColorSpriteBatch.setColor(Color.WHITE);
		for (MapTile terrainTile : mapTiles) {
			if (terrainTile.hasFloor()) {
				render(terrainTile, vertexColorSpriteBatch, spriteCache, renderMode);
			}
		}
		vertexColorSpriteBatch.end();
	}

	public void renderChannels(TiledMap map, List<MapTile> terrainTiles, OrthographicCamera camera, TerrainSpriteCache spriteCache, RenderMode renderMode) {
		List<MapTile> tilesToShowWater = terrainTiles.stream()
				.filter(t -> t.hasChannel() && renderLiquidFlow(t))
				.collect(Collectors.toList());
		List<MapTile> tilesWithoutWater = terrainTiles.stream()
				.filter(t -> t.hasChannel() && !renderLiquidFlow(t))
				.collect(Collectors.toList());

		waterRenderer.render(map, tilesToShowWater, camera, renderMode);
		// Re-render terrain over water using channel mask
		alphaMaskSpriteBatch.setProjectionMatrix(camera.combined);
		alphaMaskSpriteBatch.begin();
		for (MapTile mapTile : tilesToShowWater) {
			renderFloorWithChannelMasks(mapTile, spriteCache, renderMode);
		}
		alphaMaskSpriteBatch.end();
		floorOverlapRenderer.renderWithChannelMasks(tilesToShowWater, camera, renderMode, spriteCache);


		vertexColorSpriteBatch.setProjectionMatrix(camera.combined);
		vertexColorSpriteBatch.enableBlending();
		if (renderMode.equals(RenderMode.DIFFUSE)) {
			vertexColorSpriteBatch.setColor(dirtMaterial.getColor());
		} else {
			vertexColorSpriteBatch.setColor(Color.WHITE);
		}
		vertexColorSpriteBatch.begin();
		for (MapTile terrainTile : tilesWithoutWater) {
			ChannelLayout channelLayout = terrainTile.getUnderTile().getChannelLayout();
			QuadrantSprites quadrantSprites = spriteCache.getSpritesForChannel(channelFloorType, channelLayout, terrainTile.getSeed());
			renderChannelQuadrants(terrainTile.getTileX(), terrainTile.getTileY(), vertexColorSpriteBatch, quadrantSprites);
		}
		vertexColorSpriteBatch.end();

//		waterRenderer.renderChannels(terrainTiles, camera, renderMode);

		vertexColorSpriteBatch.setColor(Color.WHITE);
		vertexColorSpriteBatch.begin();
		for (MapTile terrainTile : tilesToShowWater) {
			ChannelLayout channelLayout = terrainTile.getUnderTile().getChannelLayout();
			QuadrantSprites quadrantSprites = spriteCache.getSpritesForChannel(channelEdgeType, channelLayout, terrainTile.getSeed());
			renderChannelQuadrants(terrainTile.getTileX(), terrainTile.getTileY(), vertexColorSpriteBatch, quadrantSprites);
		}
		for (MapTile terrainTile : tilesWithoutWater) {
			ChannelLayout channelLayout = terrainTile.getUnderTile().getChannelLayout();
			QuadrantSprites quadrantSprites = spriteCache.getSpritesForChannel(channelEdgeType, channelLayout, terrainTile.getSeed());
			renderChannelQuadrants(terrainTile.getTileX(), terrainTile.getTileY(), vertexColorSpriteBatch, quadrantSprites);
		}
		vertexColorSpriteBatch.end();
	}

	private boolean renderLiquidFlow(MapTile t) {
		return t.getUnderTile() != null && t.getUnderTile().getLiquidFlow() != null && t.getUnderTile().getLiquidFlow().getLiquidAmount() > 0;
	}

	public void renderWalls(List<MapTile> mapTiles, Camera camera, TerrainSpriteCache spriteCache, RenderMode renderMode) {
		vertexColorSpriteBatch.setProjectionMatrix(camera.combined);
		vertexColorSpriteBatch.enableBlending();
		vertexColorSpriteBatch.begin();
		vertexColorSpriteBatch.setColor(Color.WHITE);
		for (MapTile terrainTile : mapTiles) {
			if (terrainTile.hasWall()) {
				render(terrainTile, vertexColorSpriteBatch, spriteCache, renderMode);
			}
		}
		vertexColorSpriteBatch.end();
	}

	public void render(Collection<Construction> terrainConstructionsToRender, OrthographicCamera camera, TerrainSpriteCache spriteCache, RenderMode renderMode) {
		if (!renderMode.equals(RenderMode.DIFFUSE)) {
			return;
		}

		vertexColorSpriteBatch.setProjectionMatrix(camera.combined);
		vertexColorSpriteBatch.enableBlending();
		vertexColorSpriteBatch.begin();
		vertexColorSpriteBatch.setColor(CONSTRUCTION_COLOR);
		for (Construction terrainConstruction : terrainConstructionsToRender) {
			if (terrainConstruction.getConstructionType().equals(ConstructionType.WALL_CONSTRUCTION)) {
				WallConstruction wallConstruction = (WallConstruction) terrainConstruction;
				renderWall(terrainConstruction.getPrimaryLocation().x, terrainConstruction.getPrimaryLocation().y, wallConstruction.getLayout(), wallConstruction.getId(), wallConstruction.getWallTypeToConstruct(),
						CONSTRUCTION_COLOR, vertexColorSpriteBatch, spriteCache, renderMode);
			} else if (terrainConstruction.getConstructionType().equals(ConstructionType.BRIDGE_CONSTRUCTION)) {
				BridgeConstruction bridgeConstruction = (BridgeConstruction) terrainConstruction;
				for (Map.Entry<GridPoint2, BridgeTile> bridgeEntry : bridgeConstruction.getBridge().entrySet()) {
					renderBridgeTile(bridgeEntry.getKey().x, bridgeEntry.getKey().y, bridgeConstruction.getBridge(),
							bridgeEntry.getValue().getBridgeTileLayout(), spriteCache, vertexColorSpriteBatch);
				}
			}
		}
		vertexColorSpriteBatch.end();
	}

	public void render(MapTile mapTile, VertexColorSpriteBatch vertexColorSpriteBatch, TerrainSpriteCache spriteCache, RenderMode renderMode) {

		if (mapTile.hasWall()) {
			Wall wall = mapTile.getWall();
			renderWall(mapTile.getTileX(), mapTile.getTileY(), wall.getTrueLayout(), mapTile.getSeed(), wall.getWallType(), getWallMaterialColor(mapTile),
					vertexColorSpriteBatch, spriteCache, renderMode);
			WallType oreType = wall.getOreType();
			if (oreType != null) {
				renderWall(mapTile.getTileX(), mapTile.getTileY(), wall.getTrueLayout(), mapTile.getSeed(),
						oreType, wall.getOreMaterial().getColor(),
						vertexColorSpriteBatch, spriteCache, renderMode);
				if (oreType.getOverlayWallType() != null) {
					renderWall(mapTile.getTileX(), mapTile.getTileY(), wall.getTrueLayout(), mapTile.getSeed(),
							oreType.getOverlayWallType(), getWallMaterialColor(mapTile),
							vertexColorSpriteBatch, spriteCache, renderMode);
				}
			}
		} else {
			if (renderMode.equals(RenderMode.DIFFUSE)) {
				setColor(vertexColorSpriteBatch, mapTile);
			}

			Sprite spriteForFloor = spriteCache.getFloorSpriteForType(mapTile.getFloor().getFloorType(), mapTile.getSeed());
			if (renderMode.equals(RenderMode.DIFFUSE)) {
				vertexColorSpriteBatch.draw(spriteForFloor, mapTile.getTileX(), mapTile.getTileY(), TILE_WIDTH_HEIGHT, TILE_WIDTH_HEIGHT, mapTile.getFloor().getVertexColors());
			} else {
				vertexColorSpriteBatch.draw(spriteForFloor, mapTile.getTileX(), mapTile.getTileY(), TILE_WIDTH_HEIGHT, TILE_WIDTH_HEIGHT);
			}
		}
	}

	public void renderFloorWithChannelMasks(MapTile mapTile, TerrainSpriteCache spriteCache, RenderMode renderMode) {
		if (!mapTile.hasChannel()) {
			return;
		}
		if (renderMode.equals(RenderMode.DIFFUSE)) {
			TileFloor floor = mapTile.getFloor();
			if (floor.getFloorType().isUseMaterialColor() && floor.getMaterial().getColor() != null) {
				alphaMaskSpriteBatch.setColor(floor.getMaterial().getColor());
			} else {
				Color[] vertexColors = mapTile.getFloor().getVertexColors();
				alphaMaskSpriteBatch.setColors(vertexColors);
			}
		} else {
			alphaMaskSpriteBatch.setColor(Color.WHITE);
		}

		ChannelLayout channelLayout = mapTile.getUnderTile().getChannelLayout();
		QuadrantSprites quadrantSprites = diffuseTerrainSpriteCache.getSpritesForChannel(channelMaskType, channelLayout, mapTile.getSeed());
		Sprite spriteForFloor = spriteCache.getFloorSpriteForType(mapTile.getFloor().getFloorType(), mapTile.getSeed());

		alphaMaskSpriteBatch.draw(spriteForFloor, quadrantSprites.getA(), mapTile.getTileX(), mapTile.getTileY(), 0, 0.5f, 0.5f, 0.5f);
		alphaMaskSpriteBatch.draw(spriteForFloor, quadrantSprites.getB(), mapTile.getTileX(), mapTile.getTileY(), 0.5f, 0.5f, 0.5f, 0.5f);
		alphaMaskSpriteBatch.draw(spriteForFloor, quadrantSprites.getC(), mapTile.getTileX(), mapTile.getTileY(), 0, 0f, 0.5f, 0.5f);
		alphaMaskSpriteBatch.draw(spriteForFloor, quadrantSprites.getD(), mapTile.getTileX(), mapTile.getTileY(), 0.5f, 0f, 0.5f, 0.5f);
	}

	public void renderBridgeTiles(Map<Bridge, List<MapTile>> bridgeTiles, TerrainSpriteCache spriteCache, SpriteBatch basicSpriteBatch, RenderMode renderMode) {
		for (Map.Entry<Bridge, List<MapTile>> bridgeListEntry : bridgeTiles.entrySet()) {
			if (renderMode.equals(RenderMode.DIFFUSE)) {
				basicSpriteBatch.setColor(bridgeListEntry.getKey().getMaterial().getColor());
			}
			for (MapTile bridgeTile : bridgeListEntry.getValue()) {
				renderBridgeTile(bridgeTile.getTileX(), bridgeTile.getTileY(), bridgeTile.getFloor().getBridge(),
						bridgeTile.getFloor().getBridgeTile().getBridgeTileLayout(), spriteCache, basicSpriteBatch);
			}
		}
	}

	public void renderBridgeTile(int worldX, int worldY, Bridge bridge, BridgeTileLayout layout, TerrainSpriteCache spriteCache, VertexColorSpriteBatch spriteBatch) {
		Sprite sprite = spriteCache.getForBridge(bridge, layout);
		spriteBatch.draw(sprite, worldX, worldY, 1, 1);
	}

	public void renderBridgeTile(int worldX, int worldY, Bridge bridge, BridgeTileLayout layout, TerrainSpriteCache spriteCache, Batch spriteBatch) {
		Sprite sprite = spriteCache.getForBridge(bridge, layout);
		spriteBatch.draw(sprite, worldX, worldY, 1, 1);
	}

	private void renderWall(int worldX, int worldY, TileLayout layout, long seed,
							WallType wallType, Color wallMaterialColor,
							VertexColorSpriteBatch vertexColorSpriteBatch, TerrainSpriteCache spriteCache, RenderMode renderMode) {
		if (renderMode.equals(RenderMode.DIFFUSE) && wallMaterialColor != null) {
			vertexColorSpriteBatch.setColor(wallMaterialColor);
		}

		QuadrantSprites quadrantSprites = spriteCache.getSpritesForWall(wallType, layout, seed);
		renderWallQuadrants(worldX, worldY, vertexColorSpriteBatch, quadrantSprites);
	}

	private void setColor(VertexColorSpriteBatch spriteBatch, MapTile mapTile) {
		// TODO Refactor this into only being used for floors
		if (mapTile.hasWall()) {
			spriteBatch.setColor(getWallMaterialColor(mapTile));
		} else {
			TileFloor floor = mapTile.getFloor();
			if (floor.getFloorType().isUseMaterialColor() && floor.getMaterial().getColor() != null) {
				spriteBatch.setColor(floor.getMaterial().getColor());
			} else {
				spriteBatch.setColor(Color.WHITE);
			}
		}
	}

	private Color getWallMaterialColor(MapTile mapTile) {
		Wall wall = mapTile.getWall();
		if (wall.getWallType().isUseMaterialColor() && wall.getMaterial().getColor() != null) {
			return wall.getMaterial().getColor();
		} else {
			return Color.WHITE;
		}
	}

	private void renderWallQuadrants(int worldX, int worldY, VertexColorSpriteBatch vertexColorSpriteBatch, QuadrantSprites spritesForWall) {
		/*
			Drawing 4 wall quadrants a, b, c, d where

			A | B
			-----
			  |
			C | D

			A) (0, 50) -> (32, 64)

			B) (32, 50) -> (64, 64)

			C) (0, 0) -> (32, 50)

			D) (32, 0) -> (64, 50)

		 */

		// FIXME MODDING Replace magic numbers based on 64px tile e.g. 32 and 50

		// Quadrant A
		vertexColorSpriteBatch.draw(
				spritesForWall.getA().getTexture(), // texture
				worldX,
				worldY + WALL_QUADRANT_MIDPOINT_Y,
				WALL_QUADRANT_MIDPOINT_X / 2, // midpointX relative to world position
				QUADRANT_A_B_HEIGHT / 2, // midpointY relative to world position
				WALL_QUADRANT_MIDPOINT_X, // width in world units
				QUADRANT_A_B_HEIGHT, // height in world units
				spritesForWall.getA().getScaleX(),
				spritesForWall.getA().getScaleY(),
				0,
				spritesForWall.getA().getRegionX(),
				spritesForWall.getA().getRegionY(),
				spritesForWall.getA().getRegionWidth() - 32, // src image width
				spritesForWall.getA().getRegionHeight() - 50, // src image height
				false,
				false);

		// Quadrant B
		vertexColorSpriteBatch.draw(
				spritesForWall.getB().getTexture(), // texture
				worldX + WALL_QUADRANT_MIDPOINT_X,
				worldY + WALL_QUADRANT_MIDPOINT_Y,
				WALL_QUADRANT_MIDPOINT_X / 2, // midpointX relative to world position
				QUADRANT_A_B_HEIGHT / 2, // midpointY relative to world position
				WALL_QUADRANT_MIDPOINT_X, // width in world units
				QUADRANT_A_B_HEIGHT, // height in world units
				spritesForWall.getB().getScaleX(),
				spritesForWall.getB().getScaleY(),
				0,
				spritesForWall.getB().getRegionX() + 32,
				spritesForWall.getB().getRegionY(),
				spritesForWall.getB().getRegionWidth() - 32, // src image width
				spritesForWall.getB().getRegionHeight() - 50, // src image height
				false,
				false);

		// Quadrant C
		vertexColorSpriteBatch.draw(
				spritesForWall.getC().getTexture(), // texture
				worldX,
				worldY,
				WALL_QUADRANT_MIDPOINT_X / 2, // midpointX relative to world position
				QUADRANT_C_D_HEIGHT / 2, // midpointY relative to world position
				WALL_QUADRANT_MIDPOINT_X, // width in world units
				QUADRANT_C_D_HEIGHT, // height in world units
				spritesForWall.getC().getScaleX(),
				spritesForWall.getC().getScaleY(),
				0,
				spritesForWall.getC().getRegionX(),
				spritesForWall.getC().getRegionY() + 16,
				spritesForWall.getC().getRegionWidth() - 32, // src image width
				spritesForWall.getC().getRegionHeight() - 16, // src image height
				false,
				false);

		// Quadrant D
		vertexColorSpriteBatch.draw(
				spritesForWall.getD().getTexture(), // texture
				worldX + WALL_QUADRANT_MIDPOINT_X,
				worldY,
				WALL_QUADRANT_MIDPOINT_X / 2, // midpointX relative to world position
				QUADRANT_C_D_HEIGHT / 2, // midpointY relative to world position
				WALL_QUADRANT_MIDPOINT_X, // width in world units
				QUADRANT_C_D_HEIGHT, // height in world units
				spritesForWall.getD().getScaleX(),
				spritesForWall.getD().getScaleY(),
				0,
				spritesForWall.getD().getRegionX() + 32,
				spritesForWall.getD().getRegionY() + 16,
				spritesForWall.getD().getRegionWidth() - 32, // src image width
				spritesForWall.getD().getRegionHeight() - 16, // src image height
				false,
				false);
	}


	private void renderChannelQuadrants(int worldX, int worldY, VertexColorSpriteBatch vertexColorSpriteBatch, QuadrantSprites spritesForChannel) {
		/*
			Drawing 4 channel quadrants a, b, c, d where

			A | B
			-----
			C | D

			A) (0, 32) -> (32, 64)

			B) (32, 23) -> (64, 64)

			C) (0, 0) -> (32, 23)

			D) (32, 0) -> (64, 32)

		 */

		// Quadrant A
		vertexColorSpriteBatch.draw(
				spritesForChannel.getA().getTexture(), // texture
				worldX,
				worldY + 0.5f,
				0.5f / 2f, // midpointX relative to world position
				0.5f / 2f, // midpointY relative to world position
				0.5f, // width in world units
				0.5f, // height in world units
				spritesForChannel.getA().getScaleX(),
				spritesForChannel.getA().getScaleY(),
				0,
				spritesForChannel.getA().getRegionX(),
				spritesForChannel.getA().getRegionY(),
				spritesForChannel.getA().getRegionWidth() - 32, // src image width
				spritesForChannel.getA().getRegionHeight() - 32, // src image height
				false,
				false);

		// Quadrant B
		vertexColorSpriteBatch.draw(
				spritesForChannel.getB().getTexture(), // texture
				worldX + 0.5f,
				worldY + 0.5f,
				0.5f / 2f, // midpointX relative to world position
				0.5f / 2f, // midpointY relative to world position
				0.5f, // width in world units
				0.5f, // height in world units
				spritesForChannel.getB().getScaleX(),
				spritesForChannel.getB().getScaleY(),
				0,
				spritesForChannel.getB().getRegionX() + 32,
				spritesForChannel.getB().getRegionY(),
				spritesForChannel.getB().getRegionWidth() - 32, // src image width
				spritesForChannel.getB().getRegionHeight() - 32, // src image height
				false,
				false);

		// Quadrant C
		vertexColorSpriteBatch.draw(
				spritesForChannel.getC().getTexture(), // texture
				worldX,
				worldY,
				0.5f / 2f, // midpointX relative to world position
				0.5f / 2f, // midpointY relative to world position
				0.5f, // width in world units
				0.5f, // height in world units
				spritesForChannel.getC().getScaleX(),
				spritesForChannel.getC().getScaleY(),
				0,
				spritesForChannel.getC().getRegionX(),
				spritesForChannel.getC().getRegionY() + 32,
				spritesForChannel.getC().getRegionWidth() - 32, // src image width
				spritesForChannel.getC().getRegionHeight() - 32, // src image height
				false,
				false);

		// Quadrant D
		vertexColorSpriteBatch.draw(
				spritesForChannel.getD().getTexture(), // texture
				worldX + 0.5f,
				worldY,
				0.5f / 2f, // midpointX relative to world position
				0.5f / 2f, // midpointY relative to world position
				0.5f, // width in world units
				0.5f, // height in world units
				spritesForChannel.getD().getScaleX(),
				spritesForChannel.getD().getScaleY(),
				0,
				spritesForChannel.getD().getRegionX() + 32,
				spritesForChannel.getD().getRegionY() + 32,
				spritesForChannel.getD().getRegionWidth() - 32, // src image width
				spritesForChannel.getD().getRegionHeight() - 32, // src image height
				false,
				false);
	}

	@Override
	public void dispose() {
		vertexColorSpriteBatch.dispose();
	}
}
