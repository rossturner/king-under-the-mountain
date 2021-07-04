package technology.rocketjump.undermount.rendering.roofing;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.ui.GameInteractionMode;
import technology.rocketjump.undermount.ui.GameInteractionStateContainer;

import javax.inject.Inject;
import javax.inject.Singleton;

import static technology.rocketjump.undermount.mapping.tile.TileExploration.UNEXPLORED;
import static technology.rocketjump.undermount.rendering.camera.TileBoundingBox.*;
import static technology.rocketjump.undermount.ui.InWorldUIRenderer.insideSelectionArea;

@Singleton
public class RoofingViewModeRenderer {

	private final GameInteractionStateContainer interactionStateContainer;
	private final JobStore jobStore;

	// MODDING expose these
	private final Color selectionColor = HexColors.get("#FFFF9966");
	private final Color renderColor = HexColors.get("#FFFFFF66");
	private final Sprite roofingSprite;

	@Inject
	public RoofingViewModeRenderer(GameInteractionStateContainer interactionStateContainer, JobStore jobStore, TextureAtlasRepository textureAtlasRepository) {
		this.interactionStateContainer = interactionStateContainer;
		this.jobStore = jobStore;

		TextureAtlas guiAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS);
		roofingSprite = guiAtlas.createSprite("triple-gate");
	}

	public void render(TiledMap map, OrthographicCamera camera, SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, boolean blinkState) {
		int minX = getMinX(camera);
		int maxX = getMaxX(camera, map);
		int minY = getMinY(camera);
		int maxY = getMaxY(camera, map);
		Vector2 minDraggingPoint = interactionStateContainer.getMinPoint();
		Vector2 maxDraggingPoint = interactionStateContainer.getMaxPoint();
		GridPoint2 minDraggingTile = new GridPoint2(MathUtils.floor(minDraggingPoint.x), MathUtils.floor(minDraggingPoint.y));
		GridPoint2 maxDraggingTile = new GridPoint2(MathUtils.floor(maxDraggingPoint.x), MathUtils.floor(maxDraggingPoint.y));

		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		for (int x = minX; x <= maxX; x++) {
			for (int y = maxY; y >= minY; y--) {
				MapTile mapTile = map.getTile(x, y);
				if (mapTile != null) {
					if (mapTile.getExploration().equals(UNEXPLORED)) {
						continue;
					}

					// Show roofing tile colour
					shapeRenderer.setColor(mapTile.getRoof().getState().viewColor);
					shapeRenderer.rect(x, y, 1, 1);
				}
			}
		}
		shapeRenderer.end();


		spriteBatch.begin();
		for (int x = minX; x <= maxX; x++) {
			for (int y = maxY; y >= minY; y--) {
				MapTile mapTile = map.getTile(x, y);
				if (mapTile != null) {
					if (mapTile.getExploration().equals(UNEXPLORED)) {
						continue;
					}

					if (insideSelectionArea(minDraggingTile, maxDraggingTile, x, y, interactionStateContainer)) {
						if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.CANCEL_ROOFING)) {
							// Don't show designations
						} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.DESIGNATE_ROOFING)) {
							// This is within dragging area
							if (shouldHighlight(mapTile)) {
								spriteBatch.setColor(selectionColor);
								spriteBatch.draw(roofingSprite, x, y, 1, 1);
							} else {
								renderExistingRoofConstruction(x, y, mapTile, spriteBatch, blinkState);
							}
						} else {
							// Not a designation-type drag
							renderExistingRoofConstruction(x, y, mapTile, spriteBatch, blinkState);
						}
					} else {
						// Outside selection area
						renderExistingRoofConstruction(x, y, mapTile, spriteBatch, blinkState);
					}
				}
			}
		}
		spriteBatch.end();
	}

	private void renderExistingRoofConstruction(int x, int y, MapTile mapTile, SpriteBatch spriteBatch, boolean blinkState) {
		if (mapTile.getRoof().isRoofConstructionQueued()) {
			for (Job job : jobStore.getJobsAtLocation(mapTile.getTilePosition())) {
				if (job.getAssignedToEntityId() != null) {
					// There is an assigned job at the location of this designation, so lets skip rendering it if blink is off
					if (!blinkState) {
						return;
					}
				}
			}

			spriteBatch.setColor(renderColor); // TODO change to roofing accessibility color - green for accessible, yellow for in range of wall, orange for outside range of support
			spriteBatch.draw(roofingSprite, x, y, 1, 1);
		}
	}

	private boolean shouldHighlight(MapTile mapTile) {
		if (interactionStateContainer.getInteractionMode().designationCheck != null) {
			return interactionStateContainer.getInteractionMode().designationCheck.shouldDesignationApply(mapTile);
		}
		return false;
	}

}
