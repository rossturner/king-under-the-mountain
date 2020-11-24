package technology.rocketjump.undermount.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Disposable;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.floor.FloorOverlap;
import technology.rocketjump.undermount.rendering.custom_libgdx.AlphaMaskSpriteBatch;
import technology.rocketjump.undermount.sprites.MasksSpriteCache;
import technology.rocketjump.undermount.sprites.TerrainSpriteCache;

import javax.inject.Inject;
import java.util.List;

import static technology.rocketjump.undermount.rendering.WorldRenderer.ONE_UNIT;

public class FloorOverlapRenderer implements Disposable {

	private final AlphaMaskSpriteBatch alphaMaskSpriteBatch = new AlphaMaskSpriteBatch();

	private final MasksSpriteCache masksSpriteCache;

	@Inject
	public FloorOverlapRenderer(MasksSpriteCache masksSpriteCache) {
		this.masksSpriteCache = masksSpriteCache;
	}


	public void render(List<MapTile> tilesToRender, OrthographicCamera camera, RenderMode renderMode, TerrainSpriteCache spriteCache) {
		alphaMaskSpriteBatch.setProjectionMatrix(camera.combined);
		alphaMaskSpriteBatch.begin();
		alphaMaskSpriteBatch.setColor(Color.WHITE);

		for (MapTile mapTile : tilesToRender) {
			if (!mapTile.getFloor().getOverlaps().isEmpty()) {

				for (FloorOverlap floorOverlap : mapTile.getFloor().getOverlaps()) {
					Sprite overlapAlphaMask = masksSpriteCache.getMaskForOverlap(floorOverlap.getLayout(), mapTile.getSeed());
					Sprite overlapSprite = spriteCache.getFloorSpriteForType(floorOverlap.getFloorType(), mapTile.getSeed());

					if (renderMode.equals(RenderMode.DIFFUSE)) {
						alphaMaskSpriteBatch.draw(overlapSprite, overlapAlphaMask, mapTile.getTileX(), mapTile.getTileY(), ONE_UNIT, ONE_UNIT, floorOverlap.getVertexColors());
					} else {
						alphaMaskSpriteBatch.draw(overlapSprite, overlapAlphaMask, mapTile.getTileX(), mapTile.getTileY(), ONE_UNIT, ONE_UNIT);
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
