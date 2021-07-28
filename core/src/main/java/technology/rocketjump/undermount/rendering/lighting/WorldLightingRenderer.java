package technology.rocketjump.undermount.rendering.lighting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.AssetDisposable;
import technology.rocketjump.undermount.environment.SunlightCalculator;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.model.MapEnvironment;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.MapVertex;
import technology.rocketjump.undermount.rendering.ScreenWriter;
import technology.rocketjump.undermount.rendering.custom_libgdx.ShaderLoader;

import java.util.List;

import static technology.rocketjump.undermount.mapping.tile.TileExploration.UNEXPLORED;
import static technology.rocketjump.undermount.rendering.WorldRenderer.ONE_UNIT;
import static technology.rocketjump.undermount.rendering.camera.TileBoundingBox.*;

@Singleton
public class WorldLightingRenderer implements GameContextAware, AssetDisposable {

	private final LightRenderer lightRenderer;
	private final ScreenWriter screenWriter;
	private final SunlightCalculator sunlightCalculator;
	private final AmbientLightingBatch outdoorLightingBatch;
	private final ShaderProgram outdoorShader;
	private GameContext gameContext;

	@Inject
	public WorldLightingRenderer(LightRenderer lightRenderer, ScreenWriter screenWriter, SunlightCalculator sunlightCalculator) {
		this.lightRenderer = lightRenderer;
		this.screenWriter = screenWriter;
		this.sunlightCalculator = sunlightCalculator;

		FileHandle vertexShaderFile = Gdx.files.classpath("shaders/ambient_lighting_vertex_shader.glsl");
		FileHandle fragmentShaderFile = Gdx.files.classpath("shaders/ambient_lighting_fragment_shader.glsl");
		outdoorShader = ShaderLoader.createShader(vertexShaderFile, fragmentShaderFile);

		this.outdoorLightingBatch = new AmbientLightingBatch(2000, outdoorShader);

	}

	public void renderWorldLighting(GameContext gameContext, List<PointLight> lightList, OrthographicCamera camera, TextureRegion bumpMapTextureRegion) {
		Gdx.gl.glClearColor(0.25f, 0.25f, 0.32f, 1); // Global ambient lighting - dark blue // MODDING expose this
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		Gdx.gl.glEnable(GL20.GL_BLEND);

		renderOutdoorLighting(gameContext.getAreaMap(), camera, bumpMapTextureRegion, gameContext.getMapEnvironment());

		lightRenderer.begin(bumpMapTextureRegion);

		for (PointLight pointLight : lightList) {
			lightRenderer.render(pointLight, camera);
		}
		lightRenderer.end();



		// Reset blend equation
		Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD);
	}

	private void renderOutdoorLighting(TiledMap tiledMap, OrthographicCamera camera,
									   TextureRegion /* TODO use this with directional outdoor lighting */ bumpMapTextureRegion, MapEnvironment mapEnvironment) {

		int minX = getMinX(camera);
		int maxX = getMaxX(camera, tiledMap);
		int minY = getMinY(camera);
		int maxY = getMaxY(camera, tiledMap);

		Color sunlightColor = sunlightCalculator.getSunlightColor(gameContext.getGameClock().getGameTimeInHours());
		sunlightColor.r = Math.min(sunlightColor.r, gameContext.getMapEnvironment().getWeatherColor().r);
		sunlightColor.g = Math.min(sunlightColor.g, gameContext.getMapEnvironment().getWeatherColor().g);
		sunlightColor.b = Math.min(sunlightColor.b, gameContext.getMapEnvironment().getWeatherColor().b);

//		currentSunlightColor.lerp(sunlightColor, Gdx.graphics.getDeltaTime());

		outdoorLightingBatch.setColor(sunlightColor);
		outdoorLightingBatch.setProjectionMatrix(camera.combined);
		outdoorLightingBatch.begin();

		for (int worldY = maxY; worldY >= minY; worldY--) {
			for (int worldX = minX; worldX <= maxX; worldX++) {
				MapTile mapTile = tiledMap.getTile(worldX, worldY);
				if (mapTile == null || mapTile.getExploration().equals(UNEXPLORED)) {
					continue;
				}

				MapVertex vertexNE = tiledMap.getVertex(mapTile, CompassDirection.NORTH_EAST);
				if (vertexNE != null) {
					MapVertex vertexSW = tiledMap.getVertex(mapTile, CompassDirection.SOUTH_WEST);
					MapVertex vertexSE = tiledMap.getVertex(mapTile, CompassDirection.SOUTH_EAST);
					MapVertex vertexNW = tiledMap.getVertex(mapTile, CompassDirection.NORTH_WEST);

					outdoorLightingBatch.draw(mapTile.getTileX(), mapTile.getTileY(), ONE_UNIT, ONE_UNIT,
							vertexSW.getOutsideLightAmount(),
							vertexNW.getOutsideLightAmount(),
							vertexNE.getOutsideLightAmount(),
							vertexSE.getOutsideLightAmount());
				}

			}
		}
		outdoorLightingBatch.end();
		outdoorLightingBatch.setColor(Color.WHITE);
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

	@Override
	public void dispose() {
		outdoorLightingBatch.dispose();
		lightRenderer.dispose();
		outdoorShader.dispose();
	}
}
