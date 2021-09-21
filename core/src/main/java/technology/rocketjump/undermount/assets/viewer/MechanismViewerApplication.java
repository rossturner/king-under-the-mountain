package technology.rocketjump.undermount.assets.viewer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.google.inject.Guice;
import com.google.inject.Injector;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.entities.EntityAssetUpdater;
import technology.rocketjump.undermount.entities.behaviour.DoNothingBehaviour;
import technology.rocketjump.undermount.entities.factories.MechanismEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismTypeDictionary;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.guice.UndermountGuiceModule;
import technology.rocketjump.undermount.mapping.tile.underground.PipeLayout;
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

/**
 * This class is to be used from a separate desktop launcher for checking (and reloading) plant asset definitions
 */
public class MechanismViewerApplication extends ApplicationAdapter {

	private SpriteBatch batch;
	private ShapeRenderer shapeRenderer;

	private EntityRenderer entityRenderer;
	private MechanismEntityFactory entityFactory;
	private PrimaryCameraWrapper cameraManager;

	private MechanismTypeDictionary mechanismTypeDictionary;
	private GameMaterialDictionary gameMaterialDictionary;

	private MechanismViewerUI ui;

	private Entity currentEntity;
	private Entity comparisonEntity;
	private MechanismEntityAttributes attributes;
	private GameMaterial mainMaterial;
	private ScreenWriter screenWriter;
	private GridPoint2 tilePostion;
	private EntityAssetUpdater assetUpdater;
	private MessageDispatcher messageDispatcher;
	private GameClock gameClock;

	@Override
	public void create() {
		Injector injector = Guice.createInjector(new UndermountGuiceModule());
		this.entityRenderer = injector.getInstance(EntityRenderer.class);
		this.entityFactory = injector.getInstance(MechanismEntityFactory.class);
		this.cameraManager = injector.getInstance(PrimaryCameraWrapper.class);
		this.screenWriter = injector.getInstance(ScreenWriter.class);
		mechanismTypeDictionary = injector.getInstance(MechanismTypeDictionary.class);
		gameMaterialDictionary = injector.getInstance(GameMaterialDictionary.class);
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

		attributes = new MechanismEntityAttributes(0); // or 1 or 2
		attributes.setMechanismType(mechanismTypeDictionary.getByName("Pipe"));
		if (attributes.getMechanismType().getName().equals("Pipe")) {
			attributes.setPipeLayout(new PipeLayout(90));
		}
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

		Vector2 position = new Vector2(cameraManager.getCamera().viewportWidth * 0.6f, cameraManager.getCamera().viewportHeight * 0.6f);
		tilePostion = new GridPoint2((int) Math.floor(position.x), (int) Math.floor(position.y));

		currentEntity = entityFactory.create(attributes, tilePostion, new DoNothingBehaviour(), new GameContext());

		assetUpdater.updateEntityAssets(currentEntity);

		ui = injector.getInstance(MechanismViewerUI.class);
		ui.init(currentEntity);

		comparisonEntity = entityFactory.create((MechanismEntityAttributes) attributes.clone(), tilePostion, new DoNothingBehaviour(), new GameContext());

		Gdx.input.setInputProcessor(ui.getStage());
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
		shapeRenderer.setColor(MAIN_TILE_COLOR);
		shapeRenderer.rect(tile.x, tile.y, 1, 1);
		shapeRenderer.end();


		batch.begin();
		batch.setProjectionMatrix(cameraManager.getCamera().combined);
		renderEntityWithOrientation(comparisonEntity, originalPosition, DOWN.toVector2(), 0, 1, RenderMode.DIFFUSE);
		renderEntityWithOrientation(comparisonEntity, originalPosition, DOWN.toVector2(), -1, 0, RenderMode.DIFFUSE);
		renderEntityWithOrientation(currentEntity, originalPosition, DOWN.toVector2(), 0, 0, RenderMode.DIFFUSE);
		renderEntityWithOrientation(comparisonEntity, originalPosition, DOWN.toVector2(), 1, 0, RenderMode.DIFFUSE);
		renderEntityWithOrientation(comparisonEntity, originalPosition, DOWN.toVector2(), 0, -1, RenderMode.DIFFUSE);
		renderEntityWithOrientation(currentEntity, originalPosition, DOWN.toVector2(), -5, 0, RenderMode.NORMALS);
		batch.end();


		ui.render();
		screenWriter.render();
	}

	private void renderEntityWithOrientation(Entity entity, Vector2 originalPosition, Vector2 orientation, float offsetX, float offsetY, RenderMode renderMode) {
		// Set orientation
		entity.getLocationComponent().setWorldPosition(originalPosition.cpy().add(orientation), true);
		// Set position
		entity.getLocationComponent().setWorldPosition(originalPosition.cpy().add(offsetX, offsetY), false);
		// Render
		entityRenderer.render(entity, batch, renderMode, null, null, null);
		// Reset position
		entity.getLocationComponent().setWorldPosition(originalPosition, false);
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
		newPosition.y = newPosition.y * 0.9f;
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