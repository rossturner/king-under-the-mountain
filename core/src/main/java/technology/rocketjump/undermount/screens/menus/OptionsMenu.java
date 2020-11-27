package technology.rocketjump.undermount.screens.menus;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RequestSoundMessage;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.rendering.camera.DisplaySettings;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;
import technology.rocketjump.undermount.screens.ScreenManager;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.*;

import static technology.rocketjump.undermount.audio.AmbientSoundManager.DEFAULT_AMBIENT_AUDIO_VOLUME_AS_STRING;
import static technology.rocketjump.undermount.audio.MusicJukebox.DEFAULT_VOLUME_AS_STRING;
import static technology.rocketjump.undermount.audio.SoundEffectManager.DEFAULT_SOUND_VOLUME_AS_STRING;
import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.*;
import static technology.rocketjump.undermount.rendering.camera.DisplaySettings.DEFAULT_UI_SCALE;

@Singleton
public class OptionsMenu implements Menu, Telegraph {

	private final IconButton backButton;
	private final Label musicLabel;
	private final Slider musicSlider;
	private final Label soundEffectLabel;
	private final Slider soundEffectSlider;
	private final Label ambientEffectLabel;
	private final Slider ambientEffectSlider;
	private final Label uiScaleLabel;
	private final Slider uiScaleSlider;
	private final CheckBox stressTestCheckbox;
	private final Skin uiSkin;
	private final I18nLabel graphicsTitle;
	private final I18nLabel audioTitle;
	private final I18nLabel gameplayTitle;
	private final I18nLabel miscTitle;
	private final I18nLabel resolutionLabel;
	private final SelectBox<Resolution> resolutionSelect;
	private final I18nCheckbox fullscreenCheckbox;
	private final I18nCheckbox edgeScrollingCheckbox;
	private final I18nCheckbox treeTransparencyCheckbox;
	private final I18nCheckbox pauseOnNotificationCheckbox;
	private final I18nCheckbox crashReportingCheckbox;
	private final I18nCheckbox enableHintsCheckbox;
	private final I18nCheckbox mainMenuScrollingCheckbox;
	private final UserPreferences userPreferences;
	private Table menuTable;
	private boolean restartRequiredNotified;

	@Inject
	public OptionsMenu(UserPreferences userPreferences, GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
					   IconButtonFactory iconButtonFactory, I18nWidgetFactory i18NWidgetFactory, SoundAssetDictionary soundAssetDictionary) {
		this.uiSkin = guiSkinRepository.getDefault();
		this.userPreferences = userPreferences;

		final SoundAsset sliderSoundAsset = soundAssetDictionary.getByName("Slider");
		final SoundAsset clickSoundAsset = soundAssetDictionary.getByName("MenuClick");

		menuTable = new Table(uiSkin);
		menuTable.setFillParent(false);
		menuTable.center();
		menuTable.background("default-rect");
//		menuTable.setDebug(true);

		graphicsTitle = i18NWidgetFactory.createLabel("GUI.OPTIONS.GRAPHICS.TITLE");
		audioTitle = i18NWidgetFactory.createLabel("GUI.OPTIONS.AUDIO.TITLE");
		gameplayTitle = i18NWidgetFactory.createLabel("GUI.OPTIONS.GAMEPLAY.TITLE");
		miscTitle = i18NWidgetFactory.createLabel("GUI.OPTIONS.MISC.TITLE");

		resolutionLabel = i18NWidgetFactory.createLabel("GUI.GRAPHICS.RESOLUTION");
		resolutionSelect = new SelectBox<>(uiSkin);
		Array<Resolution> resolutionList = Resolution.defaultResolutions;
		if (!resolutionList.contains(DisplaySettings.currentResolution, false)) {
			resolutionList.insert(0, DisplaySettings.currentResolution);
		}
		resolutionSelect.setItems(resolutionList);
		resolutionSelect.setSelected(DisplaySettings.currentResolution);
		resolutionSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				Resolution selectedResolution = resolutionSelect.getSelected();
				userPreferences.setPreference(UserPreferences.PreferenceKey.DISPLAY_RESOLUTION, selectedResolution.toString());
				if (!restartRequiredNotified) {
					messageDispatcher.dispatchMessage(MessageType.NOTIFY_RESTART_REQUIRED);
					restartRequiredNotified = true;
				}
			}
		});

		fullscreenCheckbox = i18NWidgetFactory.createCheckbox("GUI.GRAPHICS.FULLSCREEN");
		Boolean currentlyFullscreen = Boolean.valueOf(userPreferences.getPreference(DISPLAY_FULLSCREEN, "true"));
		fullscreenCheckbox.setChecked(currentlyFullscreen);
		fullscreenCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				Boolean fullscreenSelected = fullscreenCheckbox.isChecked();
				userPreferences.setPreference(UserPreferences.PreferenceKey.DISPLAY_FULLSCREEN, fullscreenSelected.toString());
				if (!restartRequiredNotified) {
					messageDispatcher.dispatchMessage(MessageType.NOTIFY_RESTART_REQUIRED);
					restartRequiredNotified = true;
				}
			}
			return true;
		});

		musicLabel = i18NWidgetFactory.createLabel("GUI.MUSIC_VOLUME");
		musicSlider = new Slider(0, 0.8f, 0.08f, false, uiSkin);
		String savedVolume = userPreferences.getPreference(MUSIC_VOLUME, DEFAULT_VOLUME_AS_STRING);
		musicSlider.setValue(Float.valueOf(savedVolume));
		musicSlider.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(sliderSoundAsset));
				Float musicSliderValue = musicSlider.getValue();
				messageDispatcher.dispatchMessage(MessageType.GUI_CHANGE_MUSIC_VOLUME, musicSliderValue);
			}
			return true;
		});

		soundEffectLabel = i18NWidgetFactory.createLabel("GUI.SOUND_EFFECT_VOLUME");
		soundEffectSlider = new Slider(0, 1, 0.1f, false, uiSkin);
		String savedSoundEffectVolume = userPreferences.getPreference(SOUND_EFFECT_VOLUME, DEFAULT_SOUND_VOLUME_AS_STRING);
		soundEffectSlider.setValue(Float.valueOf(savedSoundEffectVolume));
		soundEffectSlider.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(sliderSoundAsset));
				Float sliderValue = soundEffectSlider.getValue();
				messageDispatcher.dispatchMessage(MessageType.GUI_CHANGE_SOUND_EFFECT_VOLUME, sliderValue);
			}
			return true;
		});

		ambientEffectLabel = i18NWidgetFactory.createLabel("GUI.AMBIENT_EFFECT_VOLUME");
		ambientEffectSlider = new Slider(0, 1, 0.1f, false, uiSkin);
		String savedAmbientEffectVolume = userPreferences.getPreference(AMBIENT_EFFECT_VOLUME, DEFAULT_AMBIENT_AUDIO_VOLUME_AS_STRING);
		ambientEffectSlider.setValue(Float.valueOf(savedAmbientEffectVolume));
		ambientEffectSlider.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(sliderSoundAsset));
				Float sliderValue = ambientEffectSlider.getValue();
				messageDispatcher.dispatchMessage(MessageType.GUI_CHANGE_AMBIENT_EFFECT_VOLUME, sliderValue);
			}
			return true;
		});

		uiScaleLabel = i18NWidgetFactory.createLabel("GUI.UI_SCALE");
		uiScaleSlider = new Slider(0.5f, 3, 0.25f, false, uiSkin);

		String savedScale = userPreferences.getPreference(UI_SCALE, DEFAULT_UI_SCALE);
		uiScaleSlider.setValue(Float.valueOf(savedScale));
		uiScaleSlider.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(sliderSoundAsset));
				Float scaleSliderValue = uiScaleSlider.getValue();
				userPreferences.setPreference(UI_SCALE, String.valueOf(scaleSliderValue));
				messageDispatcher.dispatchMessage(MessageType.GUI_SET_SCALE, scaleSliderValue);
			}
			return true;
		});


		edgeScrollingCheckbox = i18NWidgetFactory.createCheckbox("GUI.OPTIONS.GAMEPLAY.USE_EDGE_SCROLLING");
		GlobalSettings.USE_EDGE_SCROLLING = Boolean.valueOf(userPreferences.getPreference(UserPreferences.PreferenceKey.EDGE_SCROLLING, "true"));;
		edgeScrollingCheckbox.setChecked(GlobalSettings.USE_EDGE_SCROLLING);
		edgeScrollingCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				GlobalSettings.USE_EDGE_SCROLLING = edgeScrollingCheckbox.isChecked();
				userPreferences.setPreference(UserPreferences.PreferenceKey.EDGE_SCROLLING, String.valueOf(GlobalSettings.USE_EDGE_SCROLLING));
			}
			return true;
		});

		treeTransparencyCheckbox = i18NWidgetFactory.createCheckbox("GUI.OPTIONS.GAMEPLAY.HIDE_TREES_OBSCURING_SETTLERS");
		GlobalSettings.TREE_TRANSPARENCY_ENABLED = Boolean.valueOf(userPreferences.getPreference(UserPreferences.PreferenceKey.TREE_TRANSPARENCY, "true"));;
		treeTransparencyCheckbox.setChecked(GlobalSettings.TREE_TRANSPARENCY_ENABLED);
		treeTransparencyCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				GlobalSettings.TREE_TRANSPARENCY_ENABLED = treeTransparencyCheckbox.isChecked();
				userPreferences.setPreference(UserPreferences.PreferenceKey.TREE_TRANSPARENCY, String.valueOf(GlobalSettings.TREE_TRANSPARENCY_ENABLED));
			}
			return true;
		});

		pauseOnNotificationCheckbox = i18NWidgetFactory.createCheckbox("GUI.OPTIONS.GAMEPLAY.PAUSE_ON_NOTIFICATION");
		GlobalSettings.PAUSE_FOR_NOTIFICATIONS = Boolean.valueOf(userPreferences.getPreference(UserPreferences.PreferenceKey.PAUSE_FOR_NOTIFICATIONS, "false"));;
		pauseOnNotificationCheckbox.setChecked(GlobalSettings.PAUSE_FOR_NOTIFICATIONS);
		pauseOnNotificationCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				GlobalSettings.PAUSE_FOR_NOTIFICATIONS = pauseOnNotificationCheckbox.isChecked();
				userPreferences.setPreference(UserPreferences.PreferenceKey.PAUSE_FOR_NOTIFICATIONS, String.valueOf(GlobalSettings.PAUSE_FOR_NOTIFICATIONS));
			}
			return true;
		});

		stressTestCheckbox = new CheckBox("Stress test (1000 settlers)", uiSkin);
		stressTestCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				ScreenManager.STRESS_TEST = stressTestCheckbox.isChecked();
			}
			return true;
		});

		crashReportingCheckbox = i18NWidgetFactory.createCheckbox("GUI.OPTIONS.MISC.CRASH_REPORTING_ENABLED");
		crashReportingCheckbox.setProgrammaticChangeEvents(false); // Used so that message triggered below does not loop endlessly
		crashReportingCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(CRASH_REPORTING, "true")));
		crashReportingCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				messageDispatcher.dispatchMessage(MessageType.CRASH_REPORTING_OPT_IN_MODIFIED, crashReportingCheckbox.isChecked());
			}
			return true;
		});

		enableHintsCheckbox = i18NWidgetFactory.createCheckbox("GUI.OPTIONS.MISC.HINTS_ENABLED");
		enableHintsCheckbox.setProgrammaticChangeEvents(false); // Used so that message triggered below does not loop endlessly
		enableHintsCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(ALLOW_HINTS, "true")));
		enableHintsCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				userPreferences.setPreference(DISABLE_TUTORIAL, String.valueOf(!enableHintsCheckbox.isChecked()));
				userPreferences.setPreference(ALLOW_HINTS, String.valueOf(enableHintsCheckbox.isChecked()));
			}
			return true;
		});


		mainMenuScrollingCheckbox = i18NWidgetFactory.createCheckbox("GUI.OPTIONS.MISC.MAIN_MENU_BACKGROUND_SCROLLING");
		mainMenuScrollingCheckbox.setProgrammaticChangeEvents(false); // Used so that message triggered below does not loop endlessly
		mainMenuScrollingCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(MAIN_MENU_BACKGROUND_SCROLLING, "true")));
		mainMenuScrollingCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				userPreferences.setPreference(MAIN_MENU_BACKGROUND_SCROLLING, String.valueOf(mainMenuScrollingCheckbox.isChecked()));
				messageDispatcher.dispatchMessage(MessageType.SET_MAIN_MENU_BACKGROUND_SCROLLING, mainMenuScrollingCheckbox.isChecked());
			}
			return true;
		});

		backButton = iconButtonFactory.create("GUI.BACK_LABEL", null, Color.LIGHT_GRAY, ButtonStyle.SMALL);
		backButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
		});

		messageDispatcher.addListener(this, MessageType.CRASH_REPORTING_OPT_IN_MODIFIED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.CRASH_REPORTING_OPT_IN_MODIFIED: {
				Boolean reportingEnabled = (Boolean) msg.extraInfo;
				crashReportingCheckbox.setChecked(reportingEnabled);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	@Override
	public void show() {
		enableHintsCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(ALLOW_HINTS, "true")));
	}

	@Override
	public void hide() {

	}

	@Override
	public void populate(Table containerTable) {
		containerTable.add(menuTable).center();
	}

	@Override
	public void reset() {
		menuTable.clearChildren();

		// GRAPHICS
		menuTable.add(graphicsTitle).colspan(3).left().pad(10).row();

		menuTable.add(new Container<>()); // pad out 1 cell
		menuTable.add(fullscreenCheckbox).colspan(2).left().pad(10).row();

		menuTable.add(resolutionLabel).pad(10).right();
		menuTable.add(resolutionSelect).pad(10).left();
		menuTable.add(new Container<>()).row(); // pad out 1 cell

		menuTable.add(uiScaleLabel).pad(10).right();
		menuTable.add(uiScaleSlider).pad(10);
		menuTable.add(new Container<>()).row(); // pad out 1 cell

		// AUDIO
		menuTable.add(audioTitle).colspan(3).left().pad(10).row();

		menuTable.add(musicLabel).pad(10).right();
		menuTable.add(musicSlider).pad(10);
		menuTable.add(new Container<>()).row(); // pad out 1 cell

		menuTable.add(soundEffectLabel).pad(10).right();
		menuTable.add(soundEffectSlider).pad(10);
		menuTable.add(new Container<>()).row(); // pad out 1 cell

		menuTable.add(ambientEffectLabel).pad(10).right();
		menuTable.add(ambientEffectSlider).pad(10);
		menuTable.add(new Container<>()).row(); // pad out 1 cell

		// GAMEPLAY
		menuTable.add(gameplayTitle).colspan(3).left().pad(10).row();

		menuTable.add(new Container<>()); // pad out 1 cell
		menuTable.add(edgeScrollingCheckbox).colspan(2).left().pad(10).row();
		menuTable.add(new Container<>()); // pad out 1 cell
		menuTable.add(treeTransparencyCheckbox).colspan(2).left().pad(10).row();
		menuTable.add(new Container<>()); // pad out 1 cell
		menuTable.add(pauseOnNotificationCheckbox).colspan(2).left().pad(10).row();
		menuTable.add(new Container<>()); // pad out 1 cell
		menuTable.add(enableHintsCheckbox).colspan(2).left().pad(10).row();

		// MISC
		menuTable.add(miscTitle).colspan(3).left().pad(10).row();

		menuTable.add(new Container<>()); // pad out 1 cell
		menuTable.add(crashReportingCheckbox).colspan(2).left().pad(10).row();
		menuTable.add(new Container<>()); // pad out 1 cell
		menuTable.add(mainMenuScrollingCheckbox).colspan(2).left().pad(10).row();

		if (GlobalSettings.DEV_MODE) {
			menuTable.add(new Container<>()); // pad out 1 cell
			menuTable.add(stressTestCheckbox).colspan(2).left().pad(10).row();
		}

		menuTable.add(backButton).colspan(3).pad(10).left().row();

	}
}
