package technology.rocketjump.undermount.assets.viewer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.google.inject.Guice;
import com.google.inject.Injector;
import technology.rocketjump.undermount.entities.factories.PlantEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.guice.UndermountGuiceModule;
import technology.rocketjump.undermount.rendering.RenderMode;
import technology.rocketjump.undermount.rendering.ScreenWriter;
import technology.rocketjump.undermount.rendering.camera.PrimaryCameraWrapper;
import technology.rocketjump.undermount.rendering.entities.EntityRenderer;

import java.util.Random;

import static technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation.DOWN;

/**
 * This class is to be used from a separate desktop launcher for checking (and reloading) plant asset definitions
 */
public class PlantViewApplication extends ApplicationAdapter {

	private SpriteBatch batch;
	private ShapeRenderer shapeRenderer;

	private EntityRenderer entityRenderer;
	private PlantEntityFactory entityFactory;
	private PrimaryCameraWrapper cameraManager;

	private PlantSpeciesDictionary plantSpeciesDictionary;

	private PlantViewUI ui;

	private Entity currentEntity;
	private PlantEntityAttributes attributes;
	private ScreenWriter screenWriter;
	private GridPoint2 tilePostion;

	@Override
	public void create () {
		Injector injector = Guice.createInjector(new UndermountGuiceModule());
		this.entityRenderer = injector.getInstance(EntityRenderer.class);
		this.entityFactory = injector.getInstance(PlantEntityFactory.class);
		this.cameraManager = injector.getInstance(PrimaryCameraWrapper.class);
		this.screenWriter = injector.getInstance(ScreenWriter.class);
		plantSpeciesDictionary = injector.getInstance(PlantSpeciesDictionary.class);

		screenWriter.offsetPosition.x = 250f;

		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();

		Random random = new Random();

		PlantSpecies species = plantSpeciesDictionary.getByName("Sunglow");
		attributes = new PlantEntityAttributes(random.nextLong(), species);

		Vector2 position = new Vector2(cameraManager.getCamera().viewportWidth * 0.75f, cameraManager.getCamera().viewportHeight * 0.2f);
		tilePostion = new GridPoint2((int)Math.floor(position.x), (int)Math.floor(position.y));

		currentEntity = entityFactory.create(attributes, tilePostion, new GameContext());

		ui = injector.getInstance(PlantViewUI.class);
		ui.init(currentEntity);

		Gdx.input.setInputProcessor(ui.getStage());
	}

	@Override
	public void render () {
		renderBackground();

		batch.begin();
		batch.setProjectionMatrix(cameraManager.getCamera().combined);
		Vector2 originalPosition = currentEntity.getLocationComponent().getWorldPosition().cpy();

		renderEntityWithOrientation(originalPosition, DOWN.toVector2(), 0, 0, RenderMode.DIFFUSE);

//		renderEntityWithOrientation(originalPosition, DOWN.toVector2(), 0, -2, RenderMode.NORMALS);

		batch.end();


		// Shape renderer to show where plant is offset from
		shapeRenderer.setProjectionMatrix(cameraManager.getCamera().combined);
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
		currentEntity.getLocationComponent().setWorldPosition(originalPosition.cpy().add(orientation), true, false);
		// Set position
		currentEntity.getLocationComponent().setWorldPosition(originalPosition.cpy().add(offsetX, offsetY), false, false);
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
	public void resize (int width, int height) {
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
	public void dispose () {
		ui.dispose();
		batch.dispose();
	}
}