package technology.rocketjump.undermount.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.undermount.logging.CrashHandler;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.misc.twitch.TwitchDataStore;
import technology.rocketjump.undermount.misc.twitch.model.TwitchAccountInfo;
import technology.rocketjump.undermount.misc.versioning.Version;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.rendering.ScreenWriter;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.screens.menus.*;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nUpdatable;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.GameDialog;
import technology.rocketjump.undermount.ui.widgets.I18nTextButton;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;

import java.util.ArrayList;
import java.util.List;

import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.*;
import static technology.rocketjump.undermount.rendering.camera.DisplaySettings.DEFAULT_UI_SCALE;
import static technology.rocketjump.undermount.rendering.camera.GlobalSettings.VERSION;

/**
 * Just to show some basic start game / options / quit etc.
 *
 * Should slowly pan across a background image
 */
@Singleton
public class MainMenuScreen implements Telegraph, GameScreen, I18nUpdatable {

	private static final float PIXEL_SCROLL_PER_SECOND = 70f;
	private final MessageDispatcher messageDispatcher;
	private final ScreenWriter screenWriter;
	private final TopLevelMenu topLevelMenu;
	private final OptionsMenu optionsMenu;
	private final ModsMenu modsMenu;
	private final EmbarkMenu embarkMenu;
	private final PrivacyOptInMenu privacyOptInMenu;
	private final Skin uiSkin;
	private final I18nTextButton newVersionButton;
	private final I18nTextButton viewRoadmapButton;
	private SpriteBatch basicSpriteBatch = new SpriteBatch();

	private OrthographicCamera camera = new OrthographicCamera();
	private Texture backgroundImage;
	private boolean scrollBackgroundImage;

	private float xCursor = 0f;
	private float backgroundScale = 1f;

	private Stage stage;

	private Table containerTable;
	private Table versionTable;

	private Menu currentMenu;

	private float uiScaleChangeTimer;
	private float uiScale;
	private Version remoteVersion;
	private final UserPreferences userPreferences;
	private final TwitchDataStore twitchDataStore;
	private final I18nTranslator i18nTranslator;

	@Inject
	public MainMenuScreen(MessageDispatcher messageDispatcher, ScreenWriter screenWriter, EmbarkMenu embarkMenu, GuiSkinRepository guiSkinRepository,
						  UserPreferences userPreferences, TopLevelMenu topLevelMenu, OptionsMenu optionsMenu,
						  PrivacyOptInMenu privacyOptInMenu, CrashHandler crashHandler, I18nWidgetFactory i18nWidgetFactory,
						  ModsMenu modsMenu, TwitchDataStore twitchDataStore, I18nTranslator i18nTranslator){
		this.messageDispatcher = messageDispatcher;
		this.screenWriter = screenWriter;
		this.embarkMenu = embarkMenu;
		this.uiSkin = guiSkinRepository.getDefault();
		this.topLevelMenu = topLevelMenu;
		this.optionsMenu = optionsMenu;
		this.modsMenu = modsMenu;
		this.privacyOptInMenu = privacyOptInMenu;
		this.userPreferences = userPreferences;
		this.uiScale = Float.parseFloat(userPreferences.getPreference(UserPreferences.PreferenceKey.UI_SCALE, "1"));
		this.twitchDataStore = twitchDataStore;
		this.i18nTranslator = i18nTranslator;

		containerTable= new Table(uiSkin);
		containerTable.setFillParent(true);
		containerTable.center();
//		containerTable.setDebug(true);

		scrollBackgroundImage = Boolean.parseBoolean(userPreferences.getPreference(MAIN_MENU_BACKGROUND_SCROLLING, "true"));

		String savedScale = userPreferences.getPreference(UI_SCALE, DEFAULT_UI_SCALE);
		ScreenViewport viewport = new ScreenViewport();
		viewport.setUnitsPerPixel(1 / Float.parseFloat(savedScale));
		stage = new Stage(viewport);
		stage.addActor(containerTable);

		versionTable = new Table(uiSkin);
		versionTable.setFillParent(true);
		versionTable.left().bottom();
		String versionText = VERSION.toString();
		if (GlobalSettings.DEV_MODE) {
			versionText += " (DEV MODE ENABLED)";
		}
		versionTable.add(new Label(versionText, uiSkin)).left().pad(5);
		newVersionButton = i18nWidgetFactory.createTextButton("GUI.NEW_VERSION_AVAILABLE");
		newVersionButton.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				Gdx.net.openURI("https://rocketjumptechnology.itch.io/king-under-the-mountain");
			}
		});

		viewRoadmapButton = i18nWidgetFactory.createTextButton("GUI.VIEW_ROADMAP");
		viewRoadmapButton.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				Gdx.net.openURI("http://kingunderthemounta.in/roadmap/");
			}
		});

		stage.addActor(versionTable);

		if (crashHandler.isOptInConfirmationRequired()) {
			currentMenu = privacyOptInMenu;
		} else {
			currentMenu = topLevelMenu;
		}
		currentMenu.show();

		messageDispatcher.addListener(this, MessageType.SWITCH_MENU);
		messageDispatcher.addListener(this, MessageType.GUI_SET_SCALE);
		messageDispatcher.addListener(this, MessageType.GUI_SCALE_CHANGED);
		messageDispatcher.addListener(this, MessageType.REMOTE_VERSION_FOUND);
		messageDispatcher.addListener(this, MessageType.SET_MAIN_MENU_BACKGROUND_SCROLLING);
		messageDispatcher.addListener(this, MessageType.TWITCH_ACCOUNT_INFO_UPDATED);
		messageDispatcher.addListener(this, MessageType.PREFERENCE_CHANGED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.SWITCH_MENU: {
				MenuType targetMenuType = (MenuType) msg.extraInfo;
				MenuType currentMenuType = MenuType.byInstance(currentMenu);
				if (!targetMenuType.equals(currentMenuType)) {
					if (currentMenu != null) {
						currentMenu.hide();
					}
					switch (targetMenuType) {
						case TOP_LEVEL_MENU:
							currentMenu = topLevelMenu;
							break;
						case OPTIONS_MENU:
							currentMenu = optionsMenu;
							break;
						case EMBARK_MENU:
							currentMenu = embarkMenu;
							break;
						case MODS_MENU:
							currentMenu = modsMenu;
							break;
						case PRIVACY_OPT_IN_MENU:
							currentMenu = optionsMenu;
							break;
						default:
							throw new NotImplementedException("not yet implemented:" + targetMenuType.name());
					}
					currentMenu.show();
					reset();
				}
				return true;
			}
			case MessageType.GUI_SET_SCALE: {
				this.uiScaleChangeTimer = 1f;
				this.uiScale = (Float)msg.extraInfo;
				return true;
			}
			case MessageType.GUI_SCALE_CHANGED: {
				resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
				return true;
			}
			case MessageType.REMOTE_VERSION_FOUND: {
				this.remoteVersion = (Version) msg.extraInfo;
				resetVersionTable();
				reset();
				return true;
			}
			case MessageType.SET_MAIN_MENU_BACKGROUND_SCROLLING: {
				this.scrollBackgroundImage = (Boolean) msg.extraInfo;
				return true;
			}
			case MessageType.TWITCH_ACCOUNT_INFO_UPDATED: {
				resetVersionTable();
				return false;
			}
			case MessageType.PREFERENCE_CHANGED: {
				UserPreferences.PreferenceKey changedPreference = (UserPreferences.PreferenceKey) msg.extraInfo;
				if (changedPreference.equals(TWITCH_INTEGRATION_ENABLED)) {
					resetVersionTable();
					return true;
				} else {
					return false;
				}
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void reset() {
		List<Dialog> outstandingDialogs = new ArrayList<>();
		for (Actor child : stage.getRoot().getChildren()) {
			if (child instanceof Dialog) {
				outstandingDialogs.add((Dialog)child);
			}
		}

		stage.clear();

		containerTable.clearChildren();
		currentMenu.reset();
		currentMenu.populate(containerTable);

		stage.addActor(containerTable);
		stage.addActor(versionTable);

		for (Dialog outstandingDialog : outstandingDialogs) {
			stage.addActor(outstandingDialog);
		}
	}

	@Override
	public void show() {
		if (new RandomXS128().nextBoolean()) {
			backgroundImage = new Texture("assets/main_menu/Dwarves.jpg");
		} else {
			backgroundImage = new Texture("assets/main_menu/Dwarf Realm.jpg");
		}

		xCursor = 0f;

		InputMultiplexer inputMultiplexer = new InputMultiplexer();
		inputMultiplexer.addProcessor(stage);
		inputMultiplexer.addProcessor(new MainMenuInputHandler(messageDispatcher));
		Gdx.input.setInputProcessor(inputMultiplexer);

		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}

	@Override
	public void hide() {
		backgroundImage.dispose();
		backgroundImage = null;
	}

	@Override
	public void render(float delta) {

		camera.update();
		stage.act(delta);


		// Delay changing ui scale when dragging
		if (uiScaleChangeTimer > 0) {
			uiScaleChangeTimer -= delta;
			if (uiScaleChangeTimer <= 0) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SCALE_CHANGED, uiScale);
			}
		}

		float minXPosition = -((backgroundImage.getWidth() * backgroundScale) - Gdx.graphics.getWidth())/2f;

		if (scrollBackgroundImage) {
			xCursor -= (delta * PIXEL_SCROLL_PER_SECOND);
			if (xCursor < minXPosition) {
				xCursor = minXPosition;
			}
		}


		float yPosition = calcYPosition();

		// Show middle section of background from xCursor to xCursor + width
		float width = xCursor + backgroundImage.getWidth();
		float height = yPosition + backgroundImage.getHeight();
		basicSpriteBatch.begin();
		basicSpriteBatch.setProjectionMatrix(camera.combined);
		basicSpriteBatch.draw(backgroundImage, xCursor, yPosition, width * backgroundScale, height * backgroundScale);
		basicSpriteBatch.end();

		stage.draw();

		screenWriter.render();
		screenWriter.clearText();
	}

	@Override
	public String getName() {
		return "MAIN_MENU";
	}

	@Override
	public void showDialog(GameDialog dialog) {
		dialog.show(stage);
	}

	@Override
	public void resize(int width, int height) {
		camera.setToOrtho(false, width, height);

		backgroundScale = 1f;
		while (calcYPosition() + (backgroundImage.getHeight() * backgroundScale) < Gdx.graphics.getHeight() + 200) {
			backgroundScale += 0.2f;
		}

		ScreenViewport viewport = new ScreenViewport(new OrthographicCamera(width, height));
		viewport.setUnitsPerPixel(1 / uiScale);
		stage.setViewport(viewport);
		stage.getViewport().update(width, height, true);

		reset();
	}

	private float calcYPosition() {
		return -Math.abs(backgroundImage.getHeight() - Gdx.graphics.getHeight())/4f - 100f;
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose() {

	}

	private void resetVersionTable() {

		versionTable.clearChildren();
		versionTable.left().bottom();

		if (twitchEnabled()) {
			TwitchAccountInfo accountInfo = twitchDataStore.getAccountInfo();
			Label twitchLabel;
			if (accountInfo == null) {
				String twitchLabelText = i18nTranslator.getTranslatedString("GUI.OPTIONS.TWITCH.DISCONNECTED_LABEL").toString();
				twitchLabel = new Label(twitchLabelText, uiSkin);
				twitchLabel.setColor(HexColors.NEGATIVE_COLOR);
			} else {
				String twitchLabelText = i18nTranslator.getTranslatedString("GUI.OPTIONS.TWITCH.CONNECTED_LABEL").toString() + " (" + accountInfo.getLogin() + ")";
				twitchLabel = new Label(twitchLabelText, uiSkin);
			}
			versionTable.add(twitchLabel).colspan(3).left().pad(5).row();
		}

		versionTable.add(viewRoadmapButton).colspan(3).left().pad(5).row();

		String versionText = VERSION.toString();
		if (GlobalSettings.DEV_MODE) {
			versionText += " (DEV MODE ENABLED)";
		}
		versionTable.add(new Label(versionText, uiSkin)).left().pad(5);
		if (remoteVersion != null) {
			if (remoteVersion.toInteger() > VERSION.toInteger()) {
				versionTable.add(newVersionButton).pad(5);
			} else if (remoteVersion.toInteger() < VERSION.toInteger()) {
				versionTable.add(new Label("(Unreleased)", uiSkin));
			}
		}
	}

	private boolean twitchEnabled() {
		return Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.TWITCH_INTEGRATION_ENABLED, "false"));
	}

	@Override
	public void onLanguageUpdated() {
		// Not translated but needs triggering for font change
		resetVersionTable();
	}

	private static class MainMenuInputHandler implements InputProcessor {


		private final MessageDispatcher messageDispatcher;

		public MainMenuInputHandler(MessageDispatcher messageDispatcher) {
			this.messageDispatcher = messageDispatcher;
		}

		@Override
		public boolean keyDown(int keycode) {
			return false;
		}
		@Override
		public boolean keyUp(int keycode) {
			return false;
		}
		@Override
		public boolean keyTyped(char character) {
			return false;
		}
		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			return false;
		}
		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			return false;
		}
		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			return false;
		}
		@Override
		public boolean mouseMoved(int screenX, int screenY) {
			return false;
		}
		@Override
		public boolean scrolled(int amount) {
			return false;
		}
	}
}
