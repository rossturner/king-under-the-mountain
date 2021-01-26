package technology.rocketjump.undermount.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Disposable;
import com.google.inject.Inject;
import technology.rocketjump.undermount.entities.components.AttachedLightSourceComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesGrowthStage;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesType;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileRoof;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.AmbienceMessage;
import technology.rocketjump.undermount.particles.ParticleEffectStore;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;
import technology.rocketjump.undermount.rendering.camera.TileBoundingBox;
import technology.rocketjump.undermount.rendering.entities.EntityRenderer;
import technology.rocketjump.undermount.rendering.lighting.LightProcessor;
import technology.rocketjump.undermount.rendering.lighting.PointLight;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.rooms.Bridge;
import technology.rocketjump.undermount.rooms.constructions.Construction;
import technology.rocketjump.undermount.rooms.constructions.ConstructionType;
import technology.rocketjump.undermount.sprites.TerrainSpriteCache;

import java.util.*;

import static technology.rocketjump.undermount.mapping.tile.TileExploration.PARTIAL;
import static technology.rocketjump.undermount.mapping.tile.TileExploration.UNEXPLORED;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.rooms.RoomTypeDictionary.VIRTUAL_PLACING_ROOM;
import static technology.rocketjump.undermount.rooms.constructions.ConstructionType.*;

public class WorldRenderer implements Disposable {

	public static final float ONE_UNIT = 1.0f;
	private static final Color TREE_TRANSPARENCY = new Color(1f, 1f, 1f, 0.4f);
	private final RenderingOptions renderingOptions;
	private final TerrainRenderer terrainRenderer;
	private final EntityRenderer entityRenderer;
	private final WaterRenderer waterRenderer;
	private final FloorOverlapRenderer floorOverlapRenderer;
	private final RoomRenderer roomRenderer;
	private final ExplorationRenderer explorationRenderer;
	private final MessageDispatcher messageDispatcher;
	private final ParticleEffectStore particleEffectStore;

	private final SpriteBatch basicSpriteBatch = new SpriteBatch();

	private final PriorityQueue<Entity> entitiesToRender = new PriorityQueue<>(new Entity.YDepthEntityComparator());
	private final List<MapTile> terrainTiles = new LinkedList<>();
	private final List<MapTile> riverTiles = new LinkedList<>();
	private final Map<Bridge, List<MapTile>> bridgeTiles = new HashMap<>();
	private final List<MapTile> roomTiles = new LinkedList<>();
	private final List<MapTile> unexploredTiles = new LinkedList<>();
	private final Set<Long> entitiesRenderedThisFrame = new HashSet<>();
	private final Set<GridPoint2> settlerLocations = new HashSet<>();
	private final Map<Long, Construction> terrainConstructionsToRender = new TreeMap<>();
	private final Map<Long, Construction> otherConstructionsToRender = new TreeMap<>(); // This needs to behave like a set and have consistent yet unimportant ordering
	private final List<ParticleEffectInstance> particlesInFrontOfEntity = new ArrayList<>();

	private final LightProcessor lightProcessor;
	public static final Color CONSTRUCTION_COLOR = HexColors.get("#EEEEEE99");

	private static final List<ConstructionType> terrainConstructionTypes = Arrays.asList(WALL_CONSTRUCTION, FLOOR_CONSTRUCTION, BRIDGE_CONSTRUCTION);

	@Inject
	public WorldRenderer(RenderingOptions renderingOptions, TerrainRenderer terrainRenderer, EntityRenderer entityRenderer,
						 WaterRenderer waterRenderer, FloorOverlapRenderer floorOverlapRenderer, RoomRenderer roomRenderer,
						 ExplorationRenderer explorationRenderer, MessageDispatcher messageDispatcher,
						 ParticleEffectStore particleEffectStore, LightProcessor lightProcessor) {
		this.renderingOptions = renderingOptions;
		this.terrainRenderer = terrainRenderer;
		this.entityRenderer = entityRenderer;
		this.waterRenderer = waterRenderer;
		this.floorOverlapRenderer = floorOverlapRenderer;
		this.roomRenderer = roomRenderer;
		this.explorationRenderer = explorationRenderer;
		this.messageDispatcher = messageDispatcher;
		this.particleEffectStore = particleEffectStore;
		this.lightProcessor = lightProcessor;
	}

	public void renderWorld(TiledMap tiledMap, OrthographicCamera camera, TerrainSpriteCache spriteCache, RenderMode renderMode,
							List<PointLight> lightsToRenderThisFrame, List<ParticleEffectInstance> particlesToRenderAsUI) {
		Gdx.gl.glClearColor(0.4f, 0.4f, 0.4f, 1); // MODDING expose default background color
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		entitiesToRender.clear();
		entitiesRenderedThisFrame.clear();
		terrainConstructionsToRender.clear();
		otherConstructionsToRender.clear();
		terrainTiles.clear();
		riverTiles.clear();
		bridgeTiles.clear();
		roomTiles.clear();
		unexploredTiles.clear();
		settlerLocations.clear();
		int totalTiles = 0;
		int outdoorTiles = 0;

		TileBoundingBox bounds = new TileBoundingBox(camera, tiledMap);

		for (int worldY = bounds.maxY; worldY >= bounds.minY; worldY--) {
			for (int worldX = bounds.minX; worldX <= bounds.maxX; worldX++) {
				MapTile mapTile = tiledMap.getTile(worldX, worldY);
				if (mapTile == null) {
					continue;
				}
				if (mapTile.getExploration().equals(UNEXPLORED)) {
					unexploredTiles.add(mapTile);
					continue;
				}
				if (mapTile.getExploration().equals(PARTIAL)) {
					unexploredTiles.add(mapTile);
				}
				if (mapTile.getFloor().isRiverTile()) {
					riverTiles.add(mapTile);
				} else {
					terrainTiles.add(mapTile);
				}
				if (mapTile.getFloor().hasBridge()) {
					bridgeTiles.computeIfAbsent(mapTile.getFloor().getBridge(), (a) -> new LinkedList<>()).add(mapTile);
				}
				entitiesToRender.addAll(mapTile.getEntities());
				for (Entity entity : mapTile.getEntities()) {
					if (entity.getType().equals(EntityType.HUMANOID)) {
						settlerLocations.add(toGridPoint(entity.getLocationComponent().getWorldOrParentPosition()));
					}
				}
				if (mapTile.hasDoorway()) {
					entitiesToRender.add(mapTile.getDoorway().getFrameEntity());
					entitiesToRender.add(mapTile.getDoorway().getDoorEntity());
					entitiesToRender.addAll(mapTile.getDoorway().getWallCapEntities());
				}
				if (mapTile.hasRoom()) {
					roomTiles.add(mapTile);
				}
				Construction construction = mapTile.getConstruction();
				if (construction != null) {
					if (terrainConstructionTypes.contains(construction.getConstructionType())) {
						terrainConstructionsToRender.put(construction.getId(), construction);
 					} else {
						otherConstructionsToRender.put(construction.getId(), construction);
					}
				}

				totalTiles++;
				if (mapTile.getRoof().equals(TileRoof.OPEN)) {
					outdoorTiles++;
				}
			}
		}

		if (!riverTiles.isEmpty()) {
			waterRenderer.render(tiledMap, riverTiles, camera, renderMode);
		}

		terrainRenderer.renderFloors(terrainTiles, camera, spriteCache, renderMode);
		if (renderingOptions.isFloorOverlapRenderingEnabled()) {
			floorOverlapRenderer.render(riverTiles, camera, renderMode, spriteCache);
			floorOverlapRenderer.render(terrainTiles, camera, renderMode, spriteCache);
		}
		terrainRenderer.renderWalls(terrainTiles, camera, spriteCache, renderMode);


		// Also need to pick up entities up to X tiles below minX due to tree heights
		for (int worldY = bounds.minY; worldY >= bounds.minY - 4; worldY--) {
			for (int worldX = bounds.minX; worldX <= bounds.maxX; worldX++) {
				MapTile mapTile = tiledMap.getTile(worldX, worldY);
				if (mapTile == null || mapTile.getExploration().equals(UNEXPLORED)) {
					continue;
				}
				entitiesToRender.addAll(mapTile.getEntities());
				Construction construction = mapTile.getConstruction();
				if (construction != null) {
					if (terrainConstructionTypes.contains(construction.getConstructionType())) {
						terrainConstructionsToRender.put(construction.getId(), construction);
					} else {
						otherConstructionsToRender.put(construction.getId(), construction);
					}
				}
			}
		}

		// Render constructions under entities
		if (!terrainConstructionsToRender.isEmpty()) {
			terrainRenderer.render(terrainConstructionsToRender.values(), camera, spriteCache, renderMode);
		}


		basicSpriteBatch.setProjectionMatrix(camera.combined);
		basicSpriteBatch.enableBlending();
		basicSpriteBatch.begin();
		if (renderMode.equals(RenderMode.DIFFUSE)) {

			for (MapTile mapTile : roomTiles) {
				if (!mapTile.getRoomTile().getRoom().getRoomType().getRoomName().equals(VIRTUAL_PLACING_ROOM.getRoomName())) {
					roomRenderer.render(mapTile, basicSpriteBatch, spriteCache);
				}
			}
		}


		basicSpriteBatch.setColor(Color.WHITE);
		if (!bridgeTiles.isEmpty()) {
			terrainRenderer.renderBridgeTiles(bridgeTiles, spriteCache, basicSpriteBatch, renderMode);
		}

		for (Construction construction : otherConstructionsToRender.values()) {
			if (construction.getEntity() != null) {
				entityRenderer.render(construction.getEntity(), basicSpriteBatch, renderMode, null, CONSTRUCTION_COLOR, null);
			}
		}


		while (!entitiesToRender.isEmpty()) {
			Entity entity = entitiesToRender.poll();
			if (!entitiesRenderedThisFrame.contains(entity.getId())) {
				entitiesRenderedThisFrame.add(entity.getId());

				particlesInFrontOfEntity.clear();
				particleEffectStore.getParticlesAttachedToEntity(entity).forEach(p -> {
					if (p.getType().getIsAffectedByLighting()) {
						if (p.getType().isRenderBehindParent()) {
							p.getGdxParticleEffect().draw(basicSpriteBatch, renderMode);
						} else {
							particlesInFrontOfEntity.add(p);
						}
					} else {
						particlesToRenderAsUI.add(p);
					}
				});

				Color multiplyColor = null;
				if (GlobalSettings.TREE_TRANSPARENCY_ENABLED) {
					if (entity.getType().equals(EntityType.PLANT) && isPlantOccludingHumanoid(entity)) {
						multiplyColor = TREE_TRANSPARENCY;
					}
				}

				entityRenderer.render(entity, basicSpriteBatch, renderMode, null, null, multiplyColor);
				AttachedLightSourceComponent attachedLightSourceComponent = entity.getComponent(AttachedLightSourceComponent.class);
				if (lightsToRenderThisFrame != null && attachedLightSourceComponent != null && attachedLightSourceComponent.isEnabled()) {
					lightsToRenderThisFrame.add(attachedLightSourceComponent.getLightForRendering(tiledMap, lightProcessor));
				}

				particlesInFrontOfEntity.forEach(p -> p.getGdxParticleEffect().draw(basicSpriteBatch, renderMode));

			}
		}

		basicSpriteBatch.end();
		explorationRenderer.render(unexploredTiles, camera, tiledMap, renderMode);

		if (renderMode.equals(RenderMode.DIFFUSE)) { // So this only happens once per frame
			messageDispatcher.dispatchMessage(MessageType.AMBIENCE_UPDATE, new AmbienceMessage(outdoorTiles, riverTiles.size(), totalTiles));
		}
	}

	private boolean isPlantOccludingHumanoid(Entity entity) {
		PlantEntityAttributes attributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		if (attributes.getSpecies().getPlantType().equals(PlantSpeciesType.TREE)) {
			GridPoint2 treePosition = toGridPoint(entity.getLocationComponent().getWorldOrParentPosition());

			PlantSpeciesGrowthStage growthStage = attributes.getSpecies().getGrowthStages().get(attributes.getGrowthStageCursor());
			for (int checkX = treePosition.x - 1; checkX <= treePosition.x + 1; checkX++) {
				for (int checkY = treePosition.y + 1; checkY <= treePosition.y + growthStage.getTileHeight(); checkY++) {
					if (settlerLocations.contains(new GridPoint2(checkX, checkY))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public void dispose() {
		basicSpriteBatch.dispose();
		floorOverlapRenderer.dispose();
		explorationRenderer.dispose();
		terrainRenderer.dispose();
	}
}
