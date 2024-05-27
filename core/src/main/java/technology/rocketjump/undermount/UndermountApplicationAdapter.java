package technology.rocketjump.undermount;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.spi.Message;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import technology.rocketjump.undermount.assets.AssetDisposable;
import technology.rocketjump.undermount.assets.AssetDisposableRegister;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.audio.AudioUpdater;
import technology.rocketjump.undermount.constants.ConstantsRepo;
import technology.rocketjump.undermount.entities.tags.TagProcessor;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.gamecontext.GameContextRegister;
import technology.rocketjump.undermount.gamecontext.GameUpdateRegister;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.guice.UndermountGuiceModule;
import technology.rocketjump.undermount.logging.CrashHandler;
import technology.rocketjump.undermount.messaging.InfoType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.async.BackgroundTaskManager;
import technology.rocketjump.undermount.messaging.types.GameSaveMessage;
import technology.rocketjump.undermount.misc.twitch.TwitchMessageHandler;
import technology.rocketjump.undermount.misc.twitch.TwitchTaskRunner;
import technology.rocketjump.undermount.modding.LocalModRepository;
import technology.rocketjump.undermount.modding.model.ParsedMod;
import technology.rocketjump.undermount.persistence.UserFileManager;
import technology.rocketjump.undermount.rendering.GameRenderer;
import technology.rocketjump.undermount.rendering.ScreenWriter;
import technology.rocketjump.undermount.rendering.camera.PrimaryCameraWrapper;
import technology.rocketjump.undermount.screens.GameScreenDictionary;
import technology.rocketjump.undermount.screens.ScreenManager;
import technology.rocketjump.undermount.screens.menus.OptionsMenu;
import technology.rocketjump.undermount.screens.menus.options.OptionsTab;
import technology.rocketjump.undermount.ui.GuiContainer;
import technology.rocketjump.undermount.ui.I18nUpdatableRegister;
import technology.rocketjump.undermount.ui.cursor.CursorManager;
import technology.rocketjump.undermount.ui.i18n.I18nRepo;
import technology.rocketjump.undermount.ui.i18n.I18nUpdatable;
import technology.rocketjump.undermount.ui.views.TimeDateGuiView;
import technology.rocketjump.undermount.ui.widgets.ImageButtonFactory;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class UndermountApplicationAdapter extends ApplicationAdapter implements LifecycleListener {

	private GameRenderer gameRenderer;
	private PrimaryCameraWrapper primaryCameraWrapper;
	private ScreenWriter screenWriter;
	private MessageDispatcher messageDispatcher;
	private BackgroundTaskManager backgroundTaskManager; // Unused directly but needs creating for dispatched messages
	private CursorManager cursorManager; // Also unused directly
	private ImageButtonFactory imageButtonFactory; // Unused, to init profession image buttons
	private I18nUpdatableRegister i18nUpdatableRegister;
	private AssetDisposableRegister assetDisposableRegister;
	private GuiContainer guiContainer;
	private GameContextRegister gameContextRegister;
	private GameUpdateRegister gameUpdateRegister;
	private AudioUpdater audioUpdater;
	private ScreenManager screenManager;
	private ConstantsRepo constantsRepo;
	private TwitchTaskRunner twitchTaskRunner;

	@Override
	public void create() {
		try {
			Injector injector = Guice.createInjector(new UndermountGuiceModule());

			injector.getInstance(I18nRepo.class).init(injector.getInstance(TextureAtlasRepository.class));

			injector.getInstance(TwitchMessageHandler.class);
			screenWriter = injector.getInstance(ScreenWriter.class);
			gameRenderer = injector.getInstance(GameRenderer.class);
			screenWriter = injector.getInstance(ScreenWriter.class);
			primaryCameraWrapper = injector.getInstance(PrimaryCameraWrapper.class);
			messageDispatcher = injector.getInstance(MessageDispatcher.class);
			backgroundTaskManager = injector.getInstance(BackgroundTaskManager.class);
			twitchTaskRunner = injector.getInstance(TwitchTaskRunner.class);

			guiContainer = injector.getInstance(GuiContainer.class);
			cursorManager = injector.getInstance(CursorManager.class);
			imageButtonFactory = injector.getInstance(ImageButtonFactory.class);

			screenManager = injector.getInstance(ScreenManager.class);
			audioUpdater = injector.getInstance(AudioUpdater.class);
			constantsRepo = injector.getInstance(ConstantsRepo.class);

			gameContextRegister = injector.getInstance(GameContextRegister.class);
			gameUpdateRegister = injector.getInstance(GameUpdateRegister.class);
			i18nUpdatableRegister = injector.getInstance(I18nUpdatableRegister.class);
			assetDisposableRegister = injector.getInstance(AssetDisposableRegister.class);
			UserFileManager userFileManager = injector.getInstance(UserFileManager.class);

			Reflections reflections = new Reflections("technology.rocketjump.undermount", new SubTypesScanner());
			Set<Class<? extends Updatable>> updateableClasses = reflections.getSubTypesOf(Updatable.class);
			updateableClasses.forEach(this::checkForSingleton);
			gameUpdateRegister.registerClasses(updateableClasses, injector);

			// Get all implementations of GameContextAware and instantiate them
			Set<Class<? extends GameContextAware>> gameContextAwareClasses = reflections.getSubTypesOf(GameContextAware.class);
			gameContextAwareClasses.forEach(this::checkForSingleton);
			gameContextRegister.registerClasses(gameContextAwareClasses, injector);

			Set<Class<? extends I18nUpdatable>> i18nUpdateableClasses = reflections.getSubTypesOf(I18nUpdatable.class);
			i18nUpdateableClasses.forEach(this::checkForSingleton);
			i18nUpdatableRegister.registerClasses(i18nUpdateableClasses, injector);

			Set<Class<? extends AssetDisposable>> assetUpdatableClasses = reflections.getSubTypesOf(AssetDisposable.class);
			assetUpdatableClasses.forEach(this::checkForSingleton);
			assetDisposableRegister.registerClasses(assetUpdatableClasses, injector);

			Set<Class<? extends OptionsTab>> optionsTabClasses = reflections.getSubTypesOf(OptionsTab.class);
			List<OptionsTab> optionsTabInstances = new ArrayList<>();
			for (Class<? extends OptionsTab> optionsTabClass : optionsTabClasses) {
				if (!optionsTabClass.isInterface()) {
					optionsTabInstances.add(injector.getInstance(optionsTabClass));
				}
			}
			injector.getInstance(OptionsMenu.class).setTabImplementations(optionsTabInstances);

			injector.getInstance(TagProcessor.class).init();
			injector.getInstance(TimeDateGuiView.class).init(injector.getInstance(GameScreenDictionary.class));

			messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_MENU");

			messageDispatcher.dispatchMessage(MessageType.LANGUAGE_CHANGED);

			LocalModRepository localModRepository = injector.getInstance(LocalModRepository.class);
			List<ParsedMod> incompatibleMods = localModRepository.getIncompatibleMods();
			if (!incompatibleMods.isEmpty()) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_INFO, InfoType.MOD_INCOMPATIBLE);
				// Update preference string
				localModRepository.setActiveMods(localModRepository.getActiveMods());
			}

		} catch (Throwable e) {
			CrashHandler.logCrash(e);
			Gdx.app.exit();
		}
	}

	@Override
	public void render() {
		try {
			Color bgColor = constantsRepo.getWorldConstants().getBackgroundColorInstance();
			Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, bgColor.a);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			float deltaTime = Math.min(Gdx.graphics.getDeltaTime(), 1f / 15f); // Force longer duration frames to behave as though they're at 15fps

			messageDispatcher.update();
			screenManager.getCurrentScreen().render(deltaTime);
			audioUpdater.update();
			twitchTaskRunner.update(deltaTime);
			backgroundTaskManager.update(deltaTime);
		} catch (Throwable e) {
			CrashHandler.logCrash(e);
			onExit();
		}
	}

	@Override
	public void pause() {
		audioUpdater.pause();
	}

	@Override
	public void resume() {
		audioUpdater.resume();
	}

	public void onExit() {
		// TODO perhaps this can be moved to the dispose() override?
		messageDispatcher.dispatchMessage(MessageType.PERFORM_SAVE, new GameSaveMessage(false));
		messageDispatcher.dispatchMessage(MessageType.SHUTDOWN_IN_PROGRESS);
		Gdx.app.exit();
	}

	private void checkForSingleton(Class aClass) {
		if (!aClass.isInterface() && !Modifier.isAbstract(aClass.getModifiers()) && !(
				aClass.isAnnotationPresent(javax.inject.Singleton.class) || aClass.isAnnotationPresent(com.google.inject.Singleton.class)
		)) {
			throw new ConfigurationException(Arrays.asList(new Message(aClass.getName() + " must be annotated with Singleton")));
		}
	}

	@Override
	public void resize(int width, int height) {
		if (width == 0 || height == 0) {
			return;
		}
		try {
			primaryCameraWrapper.onResize(width, height);
			screenWriter.onResize(width, height);
			gameRenderer.onResize(width, height);
			guiContainer.onResize(width, height);
			screenManager.onResize(width, height);
		} catch (Exception e) {
			CrashHandler.logCrash(e);
			throw e;
		}
	}

}
