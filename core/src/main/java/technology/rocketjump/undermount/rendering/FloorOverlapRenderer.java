package technology.rocketjump.undermount.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntArray;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.floor.FloorOverlap;
import technology.rocketjump.undermount.mapping.tile.floor.OverlapQuadrantDictionary;
import technology.rocketjump.undermount.rendering.custom_libgdx.AlphaMaskSpriteBatch;
import technology.rocketjump.undermount.sprites.MasksSpriteCache;
import technology.rocketjump.undermount.sprites.TerrainSpriteCache;
import technology.rocketjump.undermount.sprites.model.QuadrantSprites;

import javax.inject.Inject;
import java.util.List;

public class FloorOverlapRenderer implements Disposable {

	private final AlphaMaskSpriteBatch alphaMaskSpriteBatch = new AlphaMaskSpriteBatch();

	private final MasksSpriteCache masksSpriteCache;
	private final OverlapQuadrantDictionary overlapQuadrantDictionary;

	@Inject
	public FloorOverlapRenderer(MasksSpriteCache masksSpriteCache, OverlapQuadrantDictionary overlapQuadrantDictionary) {
		this.masksSpriteCache = masksSpriteCache;
		this.overlapQuadrantDictionary = overlapQuadrantDictionary;
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

	@Override
	public void dispose() {
		alphaMaskSpriteBatch.dispose();
	}
}
