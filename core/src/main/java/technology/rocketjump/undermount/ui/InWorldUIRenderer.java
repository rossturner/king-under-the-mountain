package technology.rocketjump.undermount.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.entities.behaviour.humanoids.SettlerBehaviour;
import technology.rocketjump.undermount.entities.components.humanoid.SteeringComponent;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.designation.TileDesignation;
import technology.rocketjump.undermount.mapping.tile.floor.BridgeTile;
import technology.rocketjump.undermount.messaging.types.DoorwayPlacementMessage;
import technology.rocketjump.undermount.rendering.RenderMode;
import technology.rocketjump.undermount.rendering.RenderingOptions;
import technology.rocketjump.undermount.rendering.RoomRenderer;
import technology.rocketjump.undermount.rendering.TerrainRenderer;
import technology.rocketjump.undermount.rendering.entities.EntityRenderer;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.rooms.constructions.BridgeConstruction;
import technology.rocketjump.undermount.sprites.TerrainSpriteCache;
import technology.rocketjump.undermount.zones.Zone;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.rendering.WorldRenderer.*;
import static technology.rocketjump.undermount.rooms.RoomTypeDictionary.VIRTUAL_PLACING_ROOM;
import static technology.rocketjump.undermount.ui.GameInteractionMode.*;

public class InWorldUIRenderer {

	// MODDING expose furniture placement colors
	public static final String VALID_PLACEMENT_COLOR = "#00ee00aa";
	public static final String INVALID_PLACEMENT_COLOR = "#ee0000aa";
	private static final long BLINK_DURATION_MILLIS = 800L;

	private final GameInteractionStateContainer interactionStateContainer;
	private final EntityRenderer entityRenderer;
	private final TerrainRenderer terrainRenderer;
	private final SelectableOutlineRenderer selectableOutlineRenderer;

	private final ShapeRenderer shapeRenderer = new ShapeRenderer();
	private final SpriteBatch spriteBatch = new SpriteBatch();
	private final RoomRenderer roomRenderer;
	private final RenderingOptions renderingOptions;
	private final JobStore jobStore;
	private final Sprite doorIconSprite;
	private boolean blinkState = true;

	@Inject
	public InWorldUIRenderer(GameInteractionStateContainer interactionStateContainer, EntityRenderer entityRenderer,
							 TerrainRenderer terrainRenderer, RoomRenderer roomRenderer, RenderingOptions renderingOptions, JobStore jobStore,
							 FurnitureTypeDictionary furnitureTypeDictionary, TextureAtlasRepository textureAtlasRepository,
							 SelectableOutlineRenderer selectableOutlineRenderer) {
		this.interactionStateContainer = interactionStateContainer;
		this.entityRenderer = entityRenderer;
		this.terrainRenderer = terrainRenderer;
		this.roomRenderer = roomRenderer;
		this.renderingOptions = renderingOptions;
		this.jobStore = jobStore;
		this.selectableOutlineRenderer = selectableOutlineRenderer;

		FurnitureType singleDoorType = furnitureTypeDictionary.getByName("SINGLE_DOOR");
		this.doorIconSprite = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS).createSprite(singleDoorType.getIconName());
	}

	public void render(TiledMap map, OrthographicCamera camera, TerrainSpriteCache diffuseSpriteCache) {
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		shapeRenderer.setProjectionMatrix(camera.combined);
		spriteBatch.setProjectionMatrix(camera.combined);

		blinkState = (System.currentTimeMillis() % BLINK_DURATION_MILLIS) > BLINK_DURATION_MILLIS / 2;

		int minX = getMinX(camera);
		int maxX = getMaxX(camera, map);
		int minY = getMinY(camera);
		int maxY = getMaxY(camera, map);

		interactionStateContainer.update();

		Vector2 minDraggingPoint = interactionStateContainer.getMinPoint();
		Vector2 maxDraggingPoint = interactionStateContainer.getMaxPoint();
		GridPoint2 minDraggingTile = new GridPoint2(MathUtils.floor(minDraggingPoint.x), MathUtils.floor(minDraggingPoint.y));
		GridPoint2 maxDraggingTile = new GridPoint2(MathUtils.floor(maxDraggingPoint.x), MathUtils.floor(maxDraggingPoint.y));

		if (interactionStateContainer.isDragging() && interactionStateContainer.getInteractionMode().isDesignation()) {
			drawDragAreaOutline(minDraggingPoint, maxDraggingPoint);
		}

		if (interactionStateContainer.getInteractionMode().equals(DEFAULT)) {
			Selectable selectable = interactionStateContainer.getSelectable();
			if (selectable != null) {
				selectableOutlineRenderer.render(selectable, shapeRenderer);
			}
		} else if (interactionStateContainer.getInteractionMode().equals(PLACE_FURNITURE)) {
			Color furnitureColor = HexColors.get(VALID_PLACEMENT_COLOR);
			if (!interactionStateContainer.isValidFurniturePlacement()) {
				furnitureColor = HexColors.get(INVALID_PLACEMENT_COLOR);
			}

			Entity furnitureEntity = interactionStateContainer.getFurnitureEntityToPlace();
			spriteBatch.begin();
			entityRenderer.render(furnitureEntity, spriteBatch, RenderMode.DIFFUSE, null, furnitureColor, null);
			spriteBatch.end();

			shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
			shapeRenderer.setColor(furnitureColor);

			GridPoint2 furnitureGridPoint = toGridPoint(furnitureEntity.getLocationComponent().getWorldPosition());

			FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();
			for (FurnitureLayout.Workspace workspace : attributes.getCurrentLayout().getWorkspaces()) {
				GridPoint2 workspaceAccessedFrom = furnitureGridPoint.cpy().add(workspace.getAccessedFrom());
				shapeRenderer.circle(workspaceAccessedFrom.x + 0.5f, workspaceAccessedFrom.y + 0.5f, 0.35f, 50);
			}

			shapeRenderer.end();
		} else if (interactionStateContainer.getInteractionMode().equals(PLACE_DOOR)) {
			DoorwayPlacementMessage virtualDoorPlacement = interactionStateContainer.getVirtualDoorPlacement();
			if (virtualDoorPlacement != null) {
				Color doorPlacementColor = HexColors.get(VALID_PLACEMENT_COLOR);
				if (!interactionStateContainer.isValidDoorPlacement()) {
					doorPlacementColor = HexColors.get(INVALID_PLACEMENT_COLOR);
				}

				spriteBatch.begin();
				spriteBatch.setColor(doorPlacementColor);
				spriteBatch.draw(doorIconSprite, virtualDoorPlacement.getTilePosition().x, virtualDoorPlacement.getTilePosition().y, 1, 1);
				spriteBatch.end();
			}
		} else if (interactionStateContainer.getInteractionMode().equals(PLACE_BRIDGE)) {
			BridgeConstruction virtualBridgeConstruction = interactionStateContainer.getVirtualBridgeConstruction();
			if (virtualBridgeConstruction != null) {
				Color bridgePlacementColor = HexColors.get(VALID_PLACEMENT_COLOR);
				if (!interactionStateContainer.isValidBridgePlacement()) {
					bridgePlacementColor = HexColors.get(INVALID_PLACEMENT_COLOR);
				}

				spriteBatch.begin();
				spriteBatch.setColor(bridgePlacementColor);
				for (Map.Entry<GridPoint2, BridgeTile> bridgeEntry : virtualBridgeConstruction.getBridge().entrySet()) {
					terrainRenderer.renderBridgeTile(bridgeEntry.getKey().x, bridgeEntry.getKey().y, virtualBridgeConstruction.getBridge(),
							bridgeEntry.getValue().getBridgeTileLayout(), diffuseSpriteCache, spriteBatch);
				}
				spriteBatch.end();
			}
		}

		spriteBatch.begin();
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		for (int x = minX; x <= maxX; x++) {
			for (int y = maxY; y >= minY; y--) {
				MapTile mapTile = map.getTile(x, y);
				if (mapTile != null) {

					if (insideSelectionArea(minDraggingTile, maxDraggingTile, x, y)) {
						if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.REMOVE_DESIGNATIONS)) {
							// Don't show designations
						} else if (interactionStateContainer.getInteractionMode().designationName != null) { // Is a designation
							// This is within dragging area
							if (shouldHighlight(mapTile)) {
								TileDesignation designationToApply = interactionStateContainer.getInteractionMode().getDesignationToApply();
								spriteBatch.setColor(designationToApply.getSelectionColor());
								spriteBatch.draw(designationToApply.getIconSprite(), x, y, 1, 1);
							} else {
								renderExistingDesignation(x, y, mapTile);
							}
						} else if (interactionStateContainer.getInteractionMode().equals(PLACE_ROOM)) {
							// Do something for placing room
						}
					} else {
						// Outside selection area
						renderExistingDesignation(x, y, mapTile);
					}

					if (mapTile.hasRoom() && mapTile.getRoomTile().getRoom().getRoomType().getRoomName().equals(VIRTUAL_PLACING_ROOM.getRoomName())) {
						roomRenderer.render(mapTile, spriteBatch, diffuseSpriteCache);
					}

					if (renderingOptions.debug().showJobStatus()) {
						List<Job> jobsAtLocation = jobStore.getJobsAtLocation(mapTile.getTilePosition());
						for (Job jobAtLocation : jobsAtLocation) {

							switch (jobAtLocation.getJobState()) {
								case ASSIGNABLE:
									spriteBatch.setColor(Color.GREEN);
									shapeRenderer.setColor(Color.GREEN);
									break;
								case ASSIGNED:
									spriteBatch.setColor(Color.BLUE);
									shapeRenderer.setColor(Color.BLUE);
									break;
								case POTENTIALLY_ACCESSIBLE:
									spriteBatch.setColor(Color.ORANGE);
									shapeRenderer.setColor(Color.ORANGE);
									break;
								case INACCESSIBLE:
									spriteBatch.setColor(Color.RED);
									shapeRenderer.setColor(Color.RED);
									break;
							}

							if (jobAtLocation.getType().getName().equals("HAULING")) {
								shapeRenderer.line(jobAtLocation.getJobLocation().x + 0.5f, jobAtLocation.getJobLocation().y + 0.5f,
										jobAtLocation.getHaulingAllocation().getTargetPosition().x + 0.5f, jobAtLocation.getHaulingAllocation().getTargetPosition().y + 0.5f);
							} else if (mapTile.getDesignation() != null) {
								spriteBatch.draw(mapTile.getDesignation().getIconSprite(), x, y, 1, 1);
							}


						}
					}

					if (renderingOptions.debug().isShowPathfindingSlowdown()) {
						for (Entity entity : mapTile.getEntities()) {
							if (entity.getType().equals(EntityType.HUMANOID)) {
								Vector2 location = entity.getLocationComponent().getWorldPosition();
								Vector2 velocity = entity.getLocationComponent().getLinearVelocity();

								SteeringComponent steeringComponent = ((SettlerBehaviour) entity.getBehaviourComponent()).getSteeringComponent();

								if (steeringComponent.getPauseTime() > 0) {
									shapeRenderer.setColor(Color.BLUE);
								} else if (steeringComponent.isSlowed()) {
									shapeRenderer.setColor(Color.RED);
								} else {
									shapeRenderer.setColor(Color.WHITE);
								}

								shapeRenderer.line(location.x, location.y, location.x + velocity.x, location.y + velocity.y);
							}
						}
					}


					if (renderingOptions.debug().isShowZones()) {
						if (!mapTile.getZones().isEmpty()) {
							Zone zone = mapTile.getZones().iterator().next();
							Random random = new RandomXS128(zone.getZoneId());
							Color color = new Color(
									random.nextFloat(),
									random.nextFloat(),
									random.nextFloat(),
									1
							);
							shapeRenderer.rect(x, y, 1, 1, color, color, color, color);
						}
					}

				}

			}
		}
		spriteBatch.end();
		shapeRenderer.end();
	}

	private void renderExistingDesignation(int x, int y, MapTile mapTile) {
		if (mapTile.getDesignation() != null) {
			TileDesignation designation = mapTile.getDesignation();
			for (Job job : jobStore.getJobsAtLocation(mapTile.getTilePosition())) {
				if (job.getAssignedToEntityId() != null) {
					// There is an assigned job at the location of this designation, so lets skip rendering it if blink is off
					if (!blinkState) {
						return;
					}
				}
			}

			spriteBatch.setColor(designation.getDesignationColor());
			spriteBatch.draw(designation.getIconSprite(), x, y, 1, 1);
		}
	}

	private boolean insideSelectionArea(GridPoint2 minDraggingTile, GridPoint2 maxDraggingTile, int x, int y) {
		return interactionStateContainer.isDragging() &&
				minDraggingTile.x <= x && x <= maxDraggingTile.x &&
				minDraggingTile.y <= y && y <= maxDraggingTile.y;
	}

	private boolean shouldHighlight(MapTile mapTile) {
		if (interactionStateContainer.getInteractionMode().designationCheck != null) {
			return interactionStateContainer.getInteractionMode().designationCheck.shouldDesignationApply(mapTile);
		}
		return false;
	}

	private void drawDragAreaOutline(Vector2 minDraggingPoint, Vector2 maxDraggingPoint) {
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(Color.WHITE);
		shapeRenderer.rect(minDraggingPoint.x, minDraggingPoint.y,
				(maxDraggingPoint.x - minDraggingPoint.x), (maxDraggingPoint.y - minDraggingPoint.y));
		shapeRenderer.end();
	}

}
