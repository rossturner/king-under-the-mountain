package technology.rocketjump.undermount.assets.viewer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.google.inject.Guice;
import com.google.inject.Injector;
import technology.rocketjump.undermount.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.entities.EntityAssetUpdater;
import technology.rocketjump.undermount.entities.behaviour.furniture.FurnitureBehaviour;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.LiquidContainerComponent;
import technology.rocketjump.undermount.entities.components.furniture.DecorationInventoryComponent;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.factories.*;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.guice.UndermountGuiceModule;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.misc.VectorUtils;
import technology.rocketjump.undermount.rendering.RenderMode;
import technology.rocketjump.undermount.rendering.ScreenWriter;
import technology.rocketjump.undermount.rendering.camera.PrimaryCameraWrapper;
import technology.rocketjump.undermount.rendering.entities.EntityRenderer;

import java.util.Random;

import static technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation.DOWN;
import static technology.rocketjump.undermount.assets.viewer.CharacterViewApplication.createItemEntity;

/**
 * This class is to be used from a separate desktop launcher for checking (and reloading) plant asset definitions
 */
public class FurnitureViewerApplication extends ApplicationAdapter {

	private SpriteBatch batch;
	private ShapeRenderer shapeRenderer;

	private EntityRenderer entityRenderer;
	private FurnitureEntityFactory entityFactory;
	private PrimaryCameraWrapper cameraManager;

	private FurnitureTypeDictionary furnitureTypeDictionary;
	private GameMaterialDictionary gameMaterialDictionary;
	private PlantSpeciesDictionary plantSpeciesDictionary;

	private FurnitureViewerUI ui;

	private Entity currentEntity;
	private FurnitureEntityAttributes attributes;
	private GameMaterial backgroundMaterial, mainMaterial, highlightMaterial;
	private ScreenWriter screenWriter;
	private GridPoint2 tilePostion;
	private EntityAssetUpdater assetUpdater;
	private MessageDispatcher messageDispatcher;
	private GameClock gameClock;

	@Override
	public void create() {
		Injector injector = Guice.createInjector(new UndermountGuiceModule());
		this.entityRenderer = injector.getInstance(EntityRenderer.class);
		this.entityFactory = injector.getInstance(FurnitureEntityFactory.class);
		this.cameraManager = injector.getInstance(PrimaryCameraWrapper.class);
		this.screenWriter = injector.getInstance(ScreenWriter.class);
		furnitureTypeDictionary = injector.getInstance(FurnitureTypeDictionary.class);
		gameMaterialDictionary = injector.getInstance(GameMaterialDictionary.class);
		plantSpeciesDictionary = injector.getInstance(PlantSpeciesDictionary.class);
		assetUpdater = injector.getInstance(EntityAssetUpdater.class);
		this.messageDispatcher = injector.getInstance(MessageDispatcher.class);
		gameClock = injector.getInstance(GameClock.class);

		messageDispatcher.addListener(msg -> {
					assetUpdater.updateEntityAssets((Entity) msg.extraInfo);
					return true;
				}, MessageType.ENTITY_ASSET_UPDATE_REQUIRED);
		messageDispatcher.addListener(msg -> true, MessageType.ENTITY_POSITION_CHANGED);


		screenWriter.offsetPosition.x = 250f;

		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();

		Random random = new Random();

		attributes = new FurnitureEntityAttributes(0); // or 1 or 2
		attributes.setFurnitureType(furnitureTypeDictionary.getByName("WATERWHEEL"));
		attributes.getMaterials().put(GameMaterialType.WOOD, gameMaterialDictionary.getByName("Pine"));
		attributes.getMaterials().put(GameMaterialType.STONE, gameMaterialDictionary.getByName("Dolostone"));
		attributes.getMaterials().put(GameMaterialType.ORE, gameMaterialDictionary.getByName("Hematite"));
		attributes.getMaterials().put(GameMaterialType.GEM, gameMaterialDictionary.getByName("Ruby"));
		attributes.getMaterials().put(GameMaterialType.SEED, gameMaterialDictionary.getByName("Purple Helmet Spawn"));
		attributes.getMaterials().put(GameMaterialType.EARTH, gameMaterialDictionary.getByName("Dirt"));
		attributes.getMaterials().put(GameMaterialType.CLOTH, gameMaterialDictionary.getByName("Tobacco cloth"));
		attributes.setColor(ColoringLayer.MISC_COLOR_1, gameMaterialDictionary.getByName("Tin").getColor());

		GameMaterial testMetal = new GameMaterial("Test metal material", -1L, GameMaterialType.METAL, "#DDDDDE",
				null, null, null, false, false, false, false, false, null);
		attributes.getMaterials().put(GameMaterialType.METAL, testMetal);

		attributes.setPrimaryMaterialType(GameMaterialType.WOOD);

//		attributes.setColor(ColoringLayer.METAL_COLOR, new Color(0.7f, 0.7f, 0.8f, 1f));
//		PlantSpecies plantSpecies = plantSpeciesDictionary.getByName("Oak");


		Vector2 position = new Vector2(cameraManager.getCamera().viewportWidth * 0.6f, cameraManager.getCamera().viewportHeight * 0.6f);
		tilePostion = new GridPoint2((int) Math.floor(position.x), (int) Math.floor(position.y));

		currentEntity = entityFactory.create(attributes, tilePostion, new FurnitureBehaviour(), new GameContext());

		LiquidContainerComponent liquidContainerComponent = currentEntity.getOrCreateComponent(LiquidContainerComponent.class);
		liquidContainerComponent.init(currentEntity, messageDispatcher, new GameContext());
		liquidContainerComponent.setTargetLiquidMaterial(gameMaterialDictionary.getByName("Water"));
		liquidContainerComponent.setLiquidQuantity(1);

		DecorationInventoryComponent decorationInventoryComponent = new DecorationInventoryComponent();
		decorationInventoryComponent.init(currentEntity, null, null);
//		decorationInventoryComponent.add(createItemEntity("Product-Anvil", injector, ItemPlacement.ON_GROUND));
//		decorationInventoryComponent.add(createItemEntity("Tool-Small-Hammer", injector, ItemPlacement.ON_GROUND));
//		decorationInventoryComponent.add(createItemEntity("Tool-Tongs", injector, ItemPlacement.ON_GROUND));
		currentEntity.addComponent(decorationInventoryComponent);

		InventoryComponent inventoryComponent = new InventoryComponent();
		inventoryComponent.setItemsUnallocated(true);
//			Entity humanoidEntity = createHumanoidEntity(injector);
//			inventoryComponent.add(humanoidEntity, currentEntity, messageDispatcher, gameClock);
//			assetUpdater.updateEntityAssets(humanoidEntity);
//			inventoryComponent.add(createItemEntity("Ingredient-Vegetable-Crate", injector), currentEntity, messageDispatcher, gameClock);
//			inventoryComponent.add(createItemEntity("Ingredient-Vegetable-Sack", injector, ItemPlacement.ON_GROUND), currentEntity, messageDispatcher, gameClock);

			inventoryComponent.add(createItemEntity("Product-Barrel", injector, ItemPlacement.ON_GROUND), currentEntity, messageDispatcher, gameClock);
//		inventoryComponent.add(createItemEntity("Resource-Metal-Bloom", injector, ItemPlacement.ON_GROUND), currentEntity, messageDispatcher, gameClock);
//			inventoryComponent.add(createItemEntity("Product-Bread-Loaf", injector), currentEntity, messageDispatcher, gameClock);
//			inventoryComponent.add(createItemEntity("Tool-Plane", injector), currentEntity, messageDispatcher, gameClock);
//			inventoryComponent.add(createItemEntity("Tool-Axe", injector), currentEntity, messageDispatcher, gameClock);
//			inventoryComponent.add(createItemEntity("Tool-Chisel", injector), currentEntity, messageDispatcher, gameClock);
//			inventoryComponent.add(createItemEntity("Tool-Kitchen-Knife", injector), currentEntity, messageDispatcher, gameClock);
//			inventoryComponent.add(createItemEntity("Tool-Saw", injector), currentEntity, messageDispatcher, gameClock);
		currentEntity.addComponent(inventoryComponent);

		assetUpdater.updateEntityAssets(currentEntity);

		ui = injector.getInstance(FurnitureViewerUI.class);
		ui.init(currentEntity);

		Gdx.input.setInputProcessor(ui.getStage());
	}

	private Entity createHumanoidEntity(Injector injector) {
		Random random = new RandomXS128();

		Color skinColor = new SkinColorFactory().randomSkinColor(random);
		Color hairColor = new HairColorFactory().randomHairColor(random);
		Color accessoryColor = new AccessoryColorFactory(gameMaterialDictionary).randomAccessoryColor(random);

		HumanoidEntityAttributes attributes = new HumanoidEntityAttributes(random.nextLong(), hairColor, skinColor, accessoryColor, GameMaterial.NULL_MATERIAL);
		Vector2 facing = new Vector2(0, 0f);
		HumanoidEntityFactory humanoidEntityFactory = injector.getInstance(HumanoidEntityFactory.class);
		GameContext gameContext = new GameContext();
		gameContext.setRandom(random);
		return humanoidEntityFactory.create(attributes, null, facing, null, null, gameContext);
	}

	private static final Color MAIN_TILE_COLOR = new Color(0f, 0f, 1f, 0.3f);
	private static final Color EXTRA_TILE_COLOR = new Color(0f, 1f, 1f, 0.3f);
	private static final Color WORKSPACE_TILE_COLOR = new Color(1f, 0f, 1f, 0.2f);
	private static final Color WORKSPACE_OFFSET_TILE_COLOR = new Color(1f, 0f, 0f, 0.2f);

	@Override
	public void render() {
		renderBackground();

		shapeRenderer.setProjectionMatrix(cameraManager.getCamera().combined);
		shapeRenderer.setColor(EXTRA_TILE_COLOR);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

		Vector2 originalPosition = currentEntity.getLocationComponent().getWorldPosition().cpy();

		GridPoint2 tile = VectorUtils.toGridPoint(originalPosition);
		FurnitureLayout currentLayout = attributes.getCurrentLayout();
		for (GridPoint2 extraTileOffset : currentLayout.getExtraTiles()) {
			GridPoint2 offsetTile = tile.cpy().add(extraTileOffset);
			shapeRenderer.rect(offsetTile.x, offsetTile.y, 1, 1);
		}
		shapeRenderer.setColor(MAIN_TILE_COLOR);
		shapeRenderer.rect(tile.x, tile.y, 1, 1);

		for (FurnitureLayout.Workspace workspace : currentLayout.getWorkspaces()) {
			shapeRenderer.setColor(WORKSPACE_TILE_COLOR);
			GridPoint2 offsetTile = tile.cpy().add(workspace.getLocation());
			shapeRenderer.rect(offsetTile.x, offsetTile.y, 1, 1);
			shapeRenderer.setColor(WORKSPACE_OFFSET_TILE_COLOR);
			offsetTile = tile.cpy().add(workspace.getAccessedFrom());
			shapeRenderer.rect(offsetTile.x, offsetTile.y, 1, 1);
		}

		shapeRenderer.end();


		batch.begin();
		batch.setProjectionMatrix(cameraManager.getCamera().combined);

		renderEntityWithOrientation(originalPosition, DOWN.toVector2(), 0, 0, RenderMode.DIFFUSE);

		renderEntityWithOrientation(originalPosition, DOWN.toVector2(), -5, 0, RenderMode.NORMALS);

		batch.end();


		shapeRenderer.setColor(Color.BLUE);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

		shapeRenderer.line(originalPosition.x - 0.5f, originalPosition.y - 0.5f, originalPosition.x + 0.5f, originalPosition.y + 0.5f);
		shapeRenderer.line(originalPosition.x - 0.5f, originalPosition.y + 0.5f, originalPosition.x + 0.5f, originalPosition.y - 0.5f);

//		shapeRenderer.rect(originalPosition.x, originalPosition.y, 1, 1);

		shapeRenderer.end();

		ui.render();
		screenWriter.render();
	}

	private void renderEntityWithOrientation(Vector2 originalPosition, Vector2 orientation, float offsetX, float offsetY, RenderMode renderMode) {
		// Set orientation
		currentEntity.getLocationComponent().setWorldPosition(originalPosition.cpy().add(orientation), true);
		// Set position
		currentEntity.getLocationComponent().setWorldPosition(originalPosition.cpy().add(offsetX, offsetY), false);
		// Render
		entityRenderer.render(currentEntity, batch, renderMode, null, null, null);
		// Reset position
		currentEntity.getLocationComponent().setWorldPosition(originalPosition, false);
	}

	private void renderBackground() {
		Gdx.gl.glClearColor(0.6f, 0.6f, 0.6f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(0.7f, 0.7f, 0.7f, 1f);
		shapeRenderer.setProjectionMatrix(cameraManager.getCamera().combined);
		for (float x = 0; x <= cameraManager.getCamera().viewportWidth + 1f; x += 0.5f) {
			for (float y = 0; y <= cameraManager.getCamera().viewportHeight + 1f; y += 0.5f) {
				boolean xEven = Math.round(x) - x < 0.001f;
				boolean yEven = Math.round(y) - y < 0.001f;

				if ((xEven && !yEven) || (!xEven && yEven)) {
					shapeRenderer.rect(x, y, 0.5f, 0.5f);
				}
			}
		}
		shapeRenderer.end();

	}

	@Override
	public void resize(int width, int height) {
		ui.onResize(width, height);
		cameraManager.onResize(width, height);
		cameraManager.getCamera().zoom = 0.8f;
		cameraManager.getCamera().update();
		screenWriter.onResize(width, height);

		Vector3 newPosition = new Vector3(width, height, 0);
		newPosition.x = newPosition.x * 0.55f;
		newPosition.y = newPosition.y * 0.6f;
		cameraManager.getCamera().unproject(newPosition);
		// Round to nearest tile boundary
		newPosition.x = Math.round(newPosition.x) + 0.5f;
		newPosition.y = Math.round(newPosition.y) + 0.5f;
		currentEntity.getLocationComponent().setWorldPosition(new Vector2(newPosition.x, newPosition.y), false);
	}

	@Override
	public void dispose() {
		ui.dispose();
		batch.dispose();
	}
}