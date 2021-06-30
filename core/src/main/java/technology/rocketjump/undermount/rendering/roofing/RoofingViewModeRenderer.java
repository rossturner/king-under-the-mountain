package technology.rocketjump.undermount.rendering.roofing;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.designation.TileDesignation;
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

	@Inject
	public RoofingViewModeRenderer(GameInteractionStateContainer interactionStateContainer, JobStore jobStore) {
		this.interactionStateContainer = interactionStateContainer;
		this.jobStore = jobStore;
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
					shapeRenderer.setColor(mapTile.getRoof().viewColor);
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
						if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.REMOVE_DESIGNATIONS)) {
							// Don't show designations
						} else if (interactionStateContainer.getInteractionMode().designationName != null) { // Is a designation
							// This is within dragging area
							if (shouldHighlight(mapTile)) {
								TileDesignation designationToApply = interactionStateContainer.getInteractionMode().getDesignationToApply();
								spriteBatch.setColor(designationToApply.getSelectionColor());
								spriteBatch.draw(designationToApply.getIconSprite(), x, y, 1, 1);
							} else {
								renderExistingDesignation(x, y, mapTile, spriteBatch, blinkState);
							}
						} else {
							// Not a designation-type drag
							renderExistingDesignation(x, y, mapTile, spriteBatch, blinkState);
						}
					} else {
						// Outside selection area
						renderExistingDesignation(x, y, mapTile, spriteBatch, blinkState);
					}
				}
			}
		}
		spriteBatch.end();
	}

	private void renderExistingDesignation(int x, int y, MapTile mapTile, SpriteBatch spriteBatch, boolean blinkState) {
		if (mapTile.getDesignation() != null) {
			TileDesignation designation = mapTile.getDesignation();
			if (!designation.isDisplayInRoofingView()) {
				return;
			}
			for (Job job : jobStore.getJobsAtLocation(mapTile.getTilePosition())) {
				if (job.getAssignedToEntityId() != null) {
					// There is an assigned job at the location of this designation, so lets skip rendering it if blink is off
					if (!blinkState) {
						return;
					}
				}
			}

			spriteBatch.setColor(designation.getDesignationColor()); // TODO change to roofing accessibility color - green for accessible, yellow for in range of wall, orange for outside range of support
			spriteBatch.draw(designation.getIconSprite(), x, y, 1, 1);
		}
	}

	private boolean shouldHighlight(MapTile mapTile) {
		if (interactionStateContainer.getInteractionMode().designationCheck != null) {
			return interactionStateContainer.getInteractionMode().designationCheck.shouldDesignationApply(mapTile);
		}
		return false;
	}

}
