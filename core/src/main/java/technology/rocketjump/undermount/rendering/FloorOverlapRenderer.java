package technology.rocketjump.undermount.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntArray;
import technology.rocketjump.undermount.assets.ChannelTypeDictionary;
import technology.rocketjump.undermount.assets.model.ChannelType;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.floor.FloorOverlap;
import technology.rocketjump.undermount.mapping.tile.floor.OverlapQuadrantDictionary;
import technology.rocketjump.undermount.mapping.tile.underground.ChannelLayout;
import technology.rocketjump.undermount.rendering.custom_libgdx.AlphaMaskSpriteBatch;
import technology.rocketjump.undermount.rendering.custom_libgdx.DualAlphaMaskSpriteBatch;
import technology.rocketjump.undermount.sprites.DiffuseTerrainSpriteCacheProvider;
import technology.rocketjump.undermount.sprites.MasksSpriteCache;
import technology.rocketjump.undermount.sprites.TerrainSpriteCache;
import technology.rocketjump.undermount.sprites.model.QuadrantSprites;

import javax.inject.Inject;
import java.util.List;

public class FloorOverlapRenderer implements Disposable {

	public static final Color[] WHITE_ARRAY = {Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE};
	private final AlphaMaskSpriteBatch alphaMaskSpriteBatch = new AlphaMaskSpriteBatch();
	private final DualAlphaMaskSpriteBatch dualAlphaMaskSpriteBatch = new DualAlphaMaskSpriteBatch();

	private final MasksSpriteCache masksSpriteCache;
	private final OverlapQuadrantDictionary overlapQuadrantDictionary;
	private final TerrainSpriteCache diffuseTerrainSpriteCache;
	private final ChannelType channelMaskType;

	@Inject
	public FloorOverlapRenderer(MasksSpriteCache masksSpriteCache, OverlapQuadrantDictionary overlapQuadrantDictionary,
								DiffuseTerrainSpriteCacheProvider diffuseTerrainSpriteCacheProvider, ChannelTypeDictionary channelTypeDictionary) {
		this.masksSpriteCache = masksSpriteCache;
		this.overlapQuadrantDictionary = overlapQuadrantDictionary;
		this.diffuseTerrainSpriteCache = diffuseTerrainSpriteCacheProvider.get();
		channelMaskType = channelTypeDictionary.getByName("channel_mask");
	}


	public void render(List<MapTile> tilesToRender, OrthographicCamera camera, RenderMode renderMode, TerrainSpriteCache spriteCache) {
		alphaMaskSpriteBatch.setProjectionMatrix(camera.combined);
		alphaMaskSpriteBatch.begin();
		alphaMaskSpriteBatch.setColor(Color.WHITE);

		for (MapTile mapTile : tilesToRender) {
			if (!mapTile.getFloor().getOverlaps().isEmpty()) {

				for (FloorOverlap floorOverlap : mapTile.getFloor().getOverlaps()) {
					IntArray overlapQuadrants = overlapQuadrantDictionary.getOverlapQuadrants(floorOverlap.getLayout().getId());
					QuadrantSprites quadrantAlphaSprites = masksSpriteCache.getMasksForOverlap(floorOverlap.getFloorType().getOverlapType(), floorOverlap.getLayout(), mapTile.getSeed());
					Sprite overlapSprite = spriteCache.getFloorSpriteForType(floorOverlap.getFloorType(), mapTile.getSeed());

					if (renderMode.equals(RenderMode.DIFFUSE)) {
						Color[] vertexColors = floorOverlap.getVertexColors();
						alphaMaskSpriteBatch.setColors(vertexColors);
					} else {
						alphaMaskSpriteBatch.setColors(new Color[] {Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE});
					}

					if (overlapQuadrants.get(0) != 0) {
						alphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getA(), mapTile.getTileX(), mapTile.getTileY(), 0, 0.5f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(1) != 0) {
						alphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getB(), mapTile.getTileX(), mapTile.getTileY(), 0.5f, 0.5f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(2) != 0) {
						alphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getC(), mapTile.getTileX(), mapTile.getTileY(), 0, 0f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(3) != 0) {
						alphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getD(), mapTile.getTileX(), mapTile.getTileY(), 0.5f, 0f, 0.5f, 0.5f);
					}
				}

			}
		}

		alphaMaskSpriteBatch.end();
	}


	public void renderWithChannelMasks(List<MapTile> tilesToRender, OrthographicCamera camera, RenderMode renderMode, TerrainSpriteCache spriteCache) {
		if (renderMode.equals(RenderMode.NORMALS)) {
			// TODO FIXME Currently an issue when rendering normals where diffuse render ends up white with wrong texture when camera is in certain position
			// Could look to move channel masks to masks spritesheet rather than diffuse terrain spritesheet
			return;
		}
		dualAlphaMaskSpriteBatch.setProjectionMatrix(camera.combined);
		dualAlphaMaskSpriteBatch.begin();

		for (MapTile mapTile : tilesToRender) {
			if (!mapTile.getFloor().getOverlaps().isEmpty()) {

				if (mapTile.getUnderTile() == null || mapTile.getUnderTile().getChannelLayout() == null) {
					continue;
				}

				ChannelLayout channelLayout = mapTile.getUnderTile().getChannelLayout();
				QuadrantSprites quadrantChannelMaskSprites = diffuseTerrainSpriteCache.getSpritesForChannel(channelMaskType, channelLayout, mapTile.getSeed());

				for (FloorOverlap floorOverlap : mapTile.getFloor().getOverlaps()) {
					IntArray overlapQuadrants = overlapQuadrantDictionary.getOverlapQuadrants(floorOverlap.getLayout().getId());
					QuadrantSprites quadrantAlphaSprites = masksSpriteCache.getMasksForOverlap(floorOverlap.getFloorType().getOverlapType(), floorOverlap.getLayout(), mapTile.getSeed());
					Sprite overlapSprite = spriteCache.getFloorSpriteForType(floorOverlap.getFloorType(), mapTile.getSeed());

					if (renderMode.equals(RenderMode.DIFFUSE)) {
						Color[] vertexColors = floorOverlap.getVertexColors();
						dualAlphaMaskSpriteBatch.setColors(vertexColors);
					} else {
						dualAlphaMaskSpriteBatch.setColors(WHITE_ARRAY);
					}

					if (overlapQuadrants.get(0) != 0) {
						dualAlphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getA(), quadrantChannelMaskSprites.getA(),
								mapTile.getTileX(), mapTile.getTileY(), 0, 0.5f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(1) != 0) {
						dualAlphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getB(), quadrantChannelMaskSprites.getB(),
								mapTile.getTileX(), mapTile.getTileY(), 0.5f, 0.5f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(2) != 0) {
						dualAlphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getC(), quadrantChannelMaskSprites.getC(),
								mapTile.getTileX(), mapTile.getTileY(), 0, 0f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(3) != 0) {
						dualAlphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getD(), quadrantChannelMaskSprites.getD(),
								mapTile.getTileX(), mapTile.getTileY(), 0.5f, 0f, 0.5f, 0.5f);
					}
				}

			}
		}

		dualAlphaMaskSpriteBatch.end();
	}

	@Override
	public void dispose() {
		alphaMaskSpriteBatch.dispose();
	}
}
