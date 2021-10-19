package technology.rocketjump.undermount.rendering.mechanisms;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.constants.ConstantsRepo;
import technology.rocketjump.undermount.entities.EntityAssetUpdater;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.LocationComponent;
import technology.rocketjump.undermount.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.jobs.JobTypeDictionary;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobType;
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
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.rendering.WorldRenderer.CONSTRUCTION_COLOR;
import static technology.rocketjump.undermount.rendering.camera.TileBoundingBox.*;
import static technology.rocketjump.undermount.ui.InWorldUIRenderer.*;

@Singleton
public class MechanismsViewModeRenderer {

	private final GameInteractionStateContainer interactionStateContainer;
	private final JobStore jobStore;
	private final EntityRenderer entityRenderer;
	private final EntityAssetUpdater entityAssetUpdater;

	// MODDING expose this
	private final Sprite powerSourceSprite;
	private final Color powerSourceColor = HexColors.POSITIVE_COLOR;
	private final Sprite powerConsumerSprite;
	private final Color powerConsumerColor = HexColors.NEGATIVE_COLOR;
	private final Color viewMaskColor = HexColors.get("#999999BB");
	private final List<Entity> entitiesToRender = new ArrayList<>();
	private final List<MapTile> tilesWithSourceOrConsumer = new ArrayList<>();
	private final Sprite deconstructSprite;

	private final MechanismEntityAttributes mechanismConstructionAttributes;
	private final Entity mechanismConstructionEntity;
	private JobType deconstructMechanismJobType;

	@Inject
	public MechanismsViewModeRenderer(GameInteractionStateContainer interactionStateContainer, JobStore jobStore,
									  TextureAtlasRepository textureAtlasRepository, EntityRenderer entityRenderer,
									  EntityAssetUpdater entityAssetUpdater, ConstantsRepo constantsRepo,
									  JobTypeDictionary jobTypeDictionary) {
		this.interactionStateContainer = interactionStateContainer;
		this.jobStore = jobStore;
		this.entityRenderer = entityRenderer;
		this.entityAssetUpdater = entityAssetUpdater;

		TextureAtlas guiAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS);
		powerSourceSprite = guiAtlas.createSprite("input");
		powerConsumerSprite = guiAtlas.createSprite("output");
		deconstructSprite = guiAtlas.createSprite("demolish");
		deconstructMechanismJobType = jobTypeDictionary.getByName(constantsRepo.getSettlementConstants().getDeconstructMechanismJobType());

		mechanismConstructionAttributes = new MechanismEntityAttributes(1L);
		PhysicalEntityComponent physicalEntityComponent = new PhysicalEntityComponent();
		physicalEntityComponent.setAttributes(mechanismConstructionAttributes);
		LocationComponent locationComponent = new LocationComponent();
		locationComponent.setOrientation(EntityAssetOrientation.DOWN);
		mechanismConstructionEntity = new Entity(EntityType.MECHANISM, physicalEntityComponent, null,
				locationComponent, null, new GameContext());
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
		Vector3 worldPosition = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
		Vector2 worldPosition2 = new Vector2(worldPosition.x, worldPosition.y);
		GridPoint2 cursorTilePosition = toGridPoint(worldPosition2);
		entitiesToRender.clear();
		tilesWithSourceOrConsumer.clear();

		shapeRenderer.setColor(viewMaskColor);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		for (int x = minX; x <= maxX; x++) {
			for (int y = maxY; y >= minY; y--) {
				MapTile mapTile = map.getTile(x, y);
				if (mapTile != null) {
					if (mapTile.getExploration().equals(UNEXPLORED)) {
						continue;
					}

					shapeRenderer.rect(x, y, 1, 1);

					UnderTile underTile = mapTile.getUnderTile();

					if (underTile != null) {
						if (mapTile.hasPowerMechanism()) {
							entitiesToRender.add(underTile.getPowerMechanismEntity());
						}

						if (underTile.isPowerSource() || underTile.isPowerConsumer()) {
							tilesWithSourceOrConsumer.add(mapTile);
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
		for (int x = minX; x <= maxX; x++) {
			for (int y = maxY; y >= minY; y--) {
				MapTile mapTile = map.getTile(x, y);
				if (mapTile != null) {
					if (mapTile.getExploration().equals(UNEXPLORED)) {
						continue;
					}

					if (insideSelectionArea(minDraggingTile, maxDraggingTile, x, y, interactionStateContainer)) {
						if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.CANCEL_MECHANISMS)) {
							// Don't show designations
						} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.DECONSTRUCT_MECHANISMS)) {
							if (shouldHighlight(mapTile)) {
								spriteBatch.setColor(PipeConstructionState.PENDING_DECONSTRUCTION.renderColor);
								spriteBatch.draw(deconstructSprite, x, y, 1, 1);
							} else {
								renderExistingMechanismConstruction(x, y, mapTile, spriteBatch, blinkState);
							}
						} else {
							// Not a designation-type drag
							renderExistingMechanismConstruction(x, y, mapTile, spriteBatch, blinkState);
						}
					} else {
						// Outside selection area
						renderExistingMechanismConstruction(x, y, mapTile, spriteBatch, blinkState);
					}
				}
			}
		}
		if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.DESIGNATE_MECHANISMS)) {
			// TODO Show mechanism at current mouse tile
			MapTile cursorTile = map.getTile(cursorTilePosition);
			if (cursorTile != null) {
				boolean hasOtherMechanismConstruction = cursorTile.getUnderTile() != null && cursorTile.getUnderTile().getQueuedMechanismType() != null;
				if (!hasOtherMechanismConstruction) {
					Color placementColor = GameInteractionMode.DESIGNATE_MECHANISMS.tileDesignationCheck.shouldDesignationApply(cursorTile)
							? VALID_PLACEMENT_COLOR : INVALID_PLACEMENT_COLOR;
					renderPlacingEntity(cursorTilePosition.x, cursorTilePosition.y, interactionStateContainer.getMechanismTypeToPlace(),
							spriteBatch, placementColor);
				}
			}

		}
		for (MapTile tile : tilesWithSourceOrConsumer) {
			if (tile.getUnderTile().isPowerSource()) {
				spriteBatch.setColor(powerSourceColor);
				spriteBatch.draw(powerSourceSprite, tile.getTileX(), tile.getTileY(), 1, 1);
			} else if (tile.getUnderTile().isPowerConsumer()) {
				spriteBatch.setColor(powerConsumerColor);
				spriteBatch.draw(powerConsumerSprite, tile.getTileX(), tile.getTileY(), 1, 1);
			}
		}
		spriteBatch.end();


	}


	private void renderExistingMechanismConstruction(int x, int y, MapTile mapTile, Batch spriteBatch, boolean blinkState) {
		UnderTile underTile = mapTile.getUnderTile();
		if (underTile != null) {
			for (Job job : jobStore.getJobsAtLocation(mapTile.getTilePosition())) {
				if (job.getAssignedToEntityId() != null && !blinkState) {
					// There is an assigned job at the location of this designation, so lets skip rendering it if blink is off
					return;
				} else if (job.getType().equals(deconstructMechanismJobType)) {
					spriteBatch.setColor(PipeConstructionState.PENDING_DECONSTRUCTION.renderColor);
					spriteBatch.draw(deconstructSprite, x, y, 1, 1);
				}
			}

			MechanismType queuedMechanismType = underTile.getQueuedMechanismType();
			if (queuedMechanismType != null) {

				renderPlacingEntity(x, y, queuedMechanismType, spriteBatch, CONSTRUCTION_COLOR);
			}
		}
	}

	private void renderPlacingEntity(int x, int y, MechanismType mechanismType, Batch spriteBatch, Color color) {
		mechanismConstructionAttributes.setMechanismType(mechanismType);
		entityAssetUpdater.updateEntityAssets(mechanismConstructionEntity);
		mechanismConstructionEntity.getLocationComponent().setWorldPosition(new Vector2(x + 0.5f, y + 0.5f), false, false);

		entityRenderer.render(mechanismConstructionEntity, spriteBatch, RenderMode.DIFFUSE, null, color, null);
	}

	private boolean shouldHighlight(MapTile mapTile) {
		if (interactionStateContainer.getInteractionMode().tileDesignationCheck != null) {
			return interactionStateContainer.getInteractionMode().tileDesignationCheck.shouldDesignationApply(mapTile);
		}
		return false;
	}

}
