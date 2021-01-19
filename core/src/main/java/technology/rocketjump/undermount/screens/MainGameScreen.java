package technology.rocketjump.undermount.screens;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.environment.model.GameSpeed;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.gamecontext.GameUpdateRegister;
import technology.rocketjump.undermount.input.GameWorldInputHandler;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.particles.ParticleEffectUpdater;
import technology.rocketjump.undermount.rendering.GameRenderer;
import technology.rocketjump.undermount.rendering.ScreenWriter;
import technology.rocketjump.undermount.rendering.camera.DisplaySettings;
import technology.rocketjump.undermount.rendering.camera.PrimaryCameraWrapper;
import technology.rocketjump.undermount.rendering.camera.TileBoundingBox;
import technology.rocketjump.undermount.ui.GuiContainer;
import technology.rocketjump.undermount.ui.widgets.GameDialog;
import technology.rocketjump.undermount.ui.widgets.ModalDialog;

@Singleton
public class MainGameScreen implements GameContextAware, GameScreen {

	private final GameRenderer gameRenderer;
	private final PrimaryCameraWrapper primaryCameraWrapper;
	private final GuiContainer guiContainer;
	private final ScreenWriter screenWriter;
	private final GameWorldInputHandler gameWorldInputHandler;
	private final MessageDispatcher messageDispatcher;
	private final ParticleEffectUpdater particleEffectUpdater;

	private GameContext gameContext;
	private GameUpdateRegister gameUpdateRegister;

	@Inject
	public MainGameScreen(GameRenderer gameRenderer, PrimaryCameraWrapper primaryCameraWrapper, GuiContainer guiContainer,
						  ScreenWriter screenWriter, GameWorldInputHandler gameWorldInputHandler,
						  MessageDispatcher messageDispatcher, ParticleEffectUpdater particleEffectUpdater, GameUpdateRegister gameUpdateRegister) {
		this.gameRenderer = gameRenderer;
		this.primaryCameraWrapper = primaryCameraWrapper;
		this.guiContainer = guiContainer;
		this.screenWriter = screenWriter;
		this.gameWorldInputHandler = gameWorldInputHandler;
		this.messageDispatcher = messageDispatcher;
		this.particleEffectUpdater = particleEffectUpdater;
		this.gameUpdateRegister = gameUpdateRegister;
	}

	@Override
	public void render(float deltaTime) {
		updateGameLogic(deltaTime);

		gameRenderer.renderGame(gameContext.getAreaMap(), primaryCameraWrapper.getCamera());
		if (DisplaySettings.showGui) {
			guiContainer.render();
		}
	}

	private void updateGameLogic(float deltaTime) {
		screenWriter.clearText();
		float multipliedDeltaTime = deltaTime * gameContext.getGameClock().getSpeedMultiplier();
		GdxAI.getTimepiece().update(multipliedDeltaTime); // This is used for message delays, not actual AI, so runs when paused
		if (!gameContext.getGameClock().isPaused()) {
			gameContext.getGameClock().update(multipliedDeltaTime, messageDispatcher);
			particleEffectUpdater.update(multipliedDeltaTime, new TileBoundingBox(primaryCameraWrapper.getCamera(), gameContext.getAreaMap()));
		}
		primaryCameraWrapper.update(deltaTime);
		gameUpdateRegister.update(multipliedDeltaTime, gameContext.getGameClock().isPaused());

//		screenWriter.printLine("Day " + gameContext.getGameClock().getDayOfSeason() + " " + gameContext.getAreaMap().getEnvironment().getCurrentSeason().name());
//		screenWriter.printLine(gameContext.getGameClock().getFormattedGameTime());

//		screenWriter.printLine("FPS: " + Gdx.graphics.getFramesPerSecond());
//		screenWriter.printLine("Time: " + gameClock.getFormattedGameTime());
//		screenWriter.printLine("Season: " + i18nTranslator.getTranslatedString(mapManager.getTiledMap().getEnvironment().getCurrentSeason().getTranslationKey()));
//		screenWriter.printLine("Rooms:" + mapManager.getTiledMap().getRooms().size);
//		screenWriter.printLine("Zoom: " + primaryCameraWrapper.getCamera().zoom);

		guiContainer.update(deltaTime);
	}

	@Override
	public void show() {
		InputMultiplexer inputMultiplexer = new InputMultiplexer();
		for (InputProcessor guiInputProcessor : guiContainer.getInputProcessors()) {
			inputMultiplexer.addProcessor(guiInputProcessor);
		}
		inputMultiplexer.addProcessor(gameWorldInputHandler);
		Gdx.input.setInputProcessor(inputMultiplexer);
	}

	@Override
	public String getName() {
		return "MAIN_GAME";
	}

	@Override
	public void showDialog(GameDialog dialog) {
		if (dialog instanceof ModalDialog && !gameContext.getGameClock().isPaused()) {
			messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED);
		}
		guiContainer.showDialog(dialog);
	}

	@Override
	public void resize(int width, int height) {

	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void hide() {

	}

	@Override
	public void dispose() {

	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
