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
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.underground.PipeConstructionState;
import technology.rocketjump.undermount.mapping.tile.underground.UnderTile;
import technology.rocketjump.undermount.rendering.RenderMode;
import technology.rocketjump.undermount.rendering.entities.EntityRenderer;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.ui.GameInteractionMode;
import technology.rocketjump.undermount.ui.GameInteractionStateContainer;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import static technology.rocketjump.undermount.mapping.tile.TileExploration.UNEXPLORED;
import static technology.rocketjump.undermount.mapping.tile.underground.PipeConstructionState.NONE;
import static technology.rocketjump.undermount.rendering.camera.TileBoundingBox.*;
import static technology.rocketjump.undermount.ui.InWorldUIRenderer.insideSelectionArea;

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
	private final Sprite pipesSprite;
	private final Color pendingPipesColor = HexColors.get("#FFFF9966");
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
		pipesSprite = guiAtlas.createSprite("pipes");
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
		for (int x = minX; x <= maxX; x++) {
			for (int y = maxY; y >= minY; y--) {
				MapTile mapTile = map.getTile(x, y);
				if (mapTile != null) {
					if (mapTile.getExploration().equals(UNEXPLORED)) {
						continue;
					}

					if (insideSelectionArea(minDraggingTile, maxDraggingTile, x, y, interactionStateContainer)) {
						if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.CANCEL_PIPING)) {
							// Don't show designations
						} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.DESIGNATE_PIPING)) {
							// This is within dragging area
							if (shouldHighlight(mapTile)) {
								spriteBatch.setColor(pendingPipesColor);
								spriteBatch.draw(pipesSprite, x, y, 1, 1);
							} else {
								renderExistingPipeConstruction(x, y, mapTile, spriteBatch, blinkState);
							}
						} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.DECONSTRUCT_PIPING)) {
							if (shouldHighlight(mapTile)) {
								spriteBatch.setColor(PipeConstructionState.PENDING_DECONSTRUCTION.renderColor);
								spriteBatch.draw(pipesSprite, x, y, 1, 1);
							} else {
								renderExistingPipeConstruction(x, y, mapTile, spriteBatch, blinkState);
							}
						} else {
							// Not a designation-type drag
							renderExistingPipeConstruction(x, y, mapTile, spriteBatch, blinkState);
						}
					} else {
						// Outside selection area
						renderExistingPipeConstruction(x, y, mapTile, spriteBatch, blinkState);
					}
				}
			}
		}
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


	private void renderExistingPipeConstruction(int x, int y, MapTile mapTile, Batch spriteBatch, boolean blinkState) {
		UnderTile underTile = mapTile.getUnderTile();
		if (underTile != null) {
			PipeConstructionState pipeConstructionState = underTile.getPipeConstructionState();
			if (!pipeConstructionState.equals(NONE)) {
				for (Job job : jobStore.getJobsAtLocation(mapTile.getTilePosition())) {
					if (job.getAssignedToEntityId() != null) {
						// There is an assigned job at the location of this designation, so lets skip rendering it if blink is off
						if (!blinkState) {
							return;
						}
					}
				}

				spriteBatch.setColor(pipeConstructionState.renderColor);
				spriteBatch.draw(pipesSprite, x, y, 1, 1);
			}
		}
	}

	private boolean shouldHighlight(MapTile mapTile) {
		if (interactionStateContainer.getInteractionMode().designationCheck != null) {
			return interactionStateContainer.getInteractionMode().designationCheck.shouldDesignationApply(mapTile);
		}
		return false;
	}

}
