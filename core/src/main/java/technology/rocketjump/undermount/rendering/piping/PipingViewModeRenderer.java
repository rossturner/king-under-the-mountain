package technology.rocketjump.undermount.rendering.piping;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.underground.UnderTile;
import technology.rocketjump.undermount.rendering.RenderMode;
import technology.rocketjump.undermount.rendering.entities.EntityRenderer;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.ui.GameInteractionStateContainer;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import static technology.rocketjump.undermount.mapping.tile.TileExploration.UNEXPLORED;
import static technology.rocketjump.undermount.rendering.camera.TileBoundingBox.*;

@Singleton
public class PipingViewModeRenderer {

	private final GameInteractionStateContainer interactionStateContainer;
	private final JobStore jobStore;
	private final EntityRenderer entityRenderer;

	// MODDING expose this
	private final Sprite liquidInputSprite;
	private final Color liquidInputColor = HexColors.get("#26e1ed");
	private final Sprite liquidOutputSprite;
	private final Color liquidOutputColor = HexColors.POSITIVE_COLOR;
//	private final Sprite deconstructSprite;
	private final Color viewMaskColor = HexColors.get("#999999BB");
	private final List<Entity> entitiesToRender = new ArrayList<>();
	private final List<MapTile> tilesWithInputsOrOutputs = new ArrayList<>();

	@Inject
	public PipingViewModeRenderer(GameInteractionStateContainer interactionStateContainer, JobStore jobStore,
								  TextureAtlasRepository textureAtlasRepository, EntityRenderer entityRenderer) {
		this.interactionStateContainer = interactionStateContainer;
		this.jobStore = jobStore;
		this.entityRenderer = entityRenderer;

		TextureAtlas guiAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS);
		liquidInputSprite = guiAtlas.createSprite("input");
		liquidOutputSprite = guiAtlas.createSprite("output");
//		deconstructSprite = guiAtlas.createSprite("demolish");
	}

	public void render(TiledMap map, OrthographicCamera camera, Batch spriteBatch, ShapeRenderer shapeRenderer, boolean blinkState) {
		int minX = getMinX(camera);
		int maxX = getMaxX(camera, map);
		int minY = getMinY(camera);
		int maxY = getMaxY(camera, map);
		Vector2 minDraggingPoint = interactionStateContainer.getMinPoint();
		Vector2 maxDraggingPoint = interactionStateContainer.getMaxPoint();
		GridPoint2 minDraggingTile = new GridPoint2(MathUtils.floor(minDraggingPoint.x), MathUtils.floor(minDraggingPoint.y));
		GridPoint2 maxDraggingTile = new GridPoint2(MathUtils.floor(maxDraggingPoint.x), MathUtils.floor(maxDraggingPoint.y));
		entitiesToRender.clear();
		tilesWithInputsOrOutputs.clear();

		shapeRenderer.setColor(viewMaskColor);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		for (int x = minX; x <= maxX; x++) {
			for (int y = maxY; y >= minY; y--) {
				MapTile mapTile = map.getTile(x, y);
				if (mapTile != null) {
					if (mapTile.getExploration().equals(UNEXPLORED) || mapTile.getFloor().isRiverTile()) {
						continue;
					}

					shapeRenderer.rect(x, y, 1, 1);

					UnderTile underTile = mapTile.getUnderTile();

					if (underTile != null) {
						if (mapTile.hasPipe()) {
							entitiesToRender.add(underTile.getPipeEntity());
						}

						if (underTile.isLiquidInput() || underTile.isLiquidOutput()) {
							tilesWithInputsOrOutputs.add(mapTile);
						}
					}

				}
			}
		}
		shapeRenderer.end();

		spriteBatch.setProjectionMatrix(camera.combined);
		spriteBatch.begin();
		for (Entity entity : entitiesToRender) {
			entityRenderer.render(entity, spriteBatch, RenderMode.DIFFUSE,
					null, null, null);
		}
		for (MapTile tile : tilesWithInputsOrOutputs) {
			if (tile.getUnderTile().isLiquidInput()) {
				spriteBatch.setColor(liquidInputColor);
				spriteBatch.draw(liquidInputSprite, tile.getTileX(), tile.getTileY(), 1, 1);
			} else if (tile.getUnderTile().isLiquidOutput()) {
				spriteBatch.setColor(liquidOutputColor);
				spriteBatch.draw(liquidOutputSprite, tile.getTileX(), tile.getTileY(), 1, 1);
			}
		}
		spriteBatch.end();


	}

}
