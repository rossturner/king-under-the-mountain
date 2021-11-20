package technology.rocketjump.undermount.screens.menus.options;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RequestSoundMessage;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;
import technology.rocketjump.undermount.ui.widgets.I18nCheckbox;
import technology.rocketjump.undermount.ui.widgets.I18nLabel;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;

import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.ALLOW_HINTS;
import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.ENABLE_TUTORIAL;

@Singleton
public class GameplayOptionsTab implements OptionsTab, Telegraph {

	private final I18nLabel gameplayTitle;

	private final I18nCheckbox edgeScrollingCheckbox;
	private final I18nCheckbox zoomToCursorCheckbox;
	private final I18nCheckbox treeTransparencyCheckbox;
	private final I18nCheckbox pauseOnNotificationCheckbox;
	private final I18nCheckbox enableHintsCheckbox;
	private final I18nCheckbox enableTutorialCheckbox;
	private final UserPreferences userPreferences;

	@Inject
	public GameplayOptionsTab(UserPreferences userPreferences, MessageDispatcher messageDispatcher,
							  I18nWidgetFactory i18NWidgetFactory, SoundAssetDictionary soundAssetDictionary) {
		this.userPreferences = userPreferences;
		final SoundAsset clickSoundAsset = soundAssetDictionary.getByName("MenuClick");
		gameplayTitle = i18NWidgetFactory.createLabel("GUI.OPTIONS.GAMEPLAY.TITLE");

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

		zoomToCursorCheckbox = i18NWidgetFactory.createCheckbox("GUI.OPTIONS.GAMEPLAY.ZOOM_TO_CURSOR");
		GlobalSettings.ZOOM_TO_CURSOR = Boolean.valueOf(userPreferences.getPreference(UserPreferences.PreferenceKey.ZOOM_TO_CURSOR, "true"));;
		zoomToCursorCheckbox.setChecked(GlobalSettings.ZOOM_TO_CURSOR);
		zoomToCursorCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				GlobalSettings.ZOOM_TO_CURSOR = zoomToCursorCheckbox.isChecked();
				userPreferences.setPreference(UserPreferences.PreferenceKey.ZOOM_TO_CURSOR, String.valueOf(GlobalSettings.ZOOM_TO_CURSOR));
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
		GlobalSettings.PAUSE_FOR_NOTIFICATIONS = Boolean.valueOf(userPreferences.getPreference(UserPreferences.PreferenceKey.PAUSE_FOR_NOTIFICATIONS, "true"));;
		pauseOnNotificationCheckbox.setChecked(GlobalSettings.PAUSE_FOR_NOTIFICATIONS);
		pauseOnNotificationCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				GlobalSettings.PAUSE_FOR_NOTIFICATIONS = pauseOnNotificationCheckbox.isChecked();
				userPreferences.setPreference(UserPreferences.PreferenceKey.PAUSE_FOR_NOTIFICATIONS, String.valueOf(GlobalSettings.PAUSE_FOR_NOTIFICATIONS));
			}
			return true;
		});

		enableHintsCheckbox = i18NWidgetFactory.createCheckbox("GUI.OPTIONS.MISC.HINTS_ENABLED");
		enableHintsCheckbox.setProgrammaticChangeEvents(false); // Used so that message triggered below does not loop endlessly
		enableHintsCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(ALLOW_HINTS, "true")));
		enableHintsCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				userPreferences.setPreference(ALLOW_HINTS, String.valueOf(enableHintsCheckbox.isChecked()));
			}
			return true;
		});


		enableTutorialCheckbox = i18NWidgetFactory.createCheckbox("GUI.OPTIONS.MISC.TUTORIAL_ENABLED");
		enableTutorialCheckbox.setProgrammaticChangeEvents(false); // Used so that message triggered below does not loop endlessly
		enableTutorialCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(ENABLE_TUTORIAL, "true")));
		enableTutorialCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				userPreferences.setPreference(ENABLE_TUTORIAL, String.valueOf(enableTutorialCheckbox.isChecked()));
			}
			return true;
		});

		messageDispatcher.addListener(this, MessageType.PREFERENCE_CHANGED);
	}

	@Override
	public void populate(Table menuTable) {
		// GAMEPLAY
		menuTable.add(gameplayTitle).width(250).left().pad(10);
		menuTable.add(new Container<>()).colspan(2).row();

		menuTable.add(new Container<>()); // pad out 1 cell
		menuTable.add(edgeScrollingCheckbox).colspan(2).left().pad(10).row();
		menuTable.add(new Container<>()); // pad out 1 cell
		menuTable.add(zoomToCursorCheckbox).colspan(2).left().pad(10).row();
		menuTable.add(new Container<>()); // pad out 1 cell
		menuTable.add(treeTransparencyCheckbox).colspan(2).left().pad(10).row();
		menuTable.add(new Container<>()); // pad out 1 cell
		menuTable.add(pauseOnNotificationCheckbox).colspan(2).left().pad(10).row();
		menuTable.add(new Container<>()); // pad out 1 cell
		menuTable.add(enableHintsCheckbox).colspan(2).left().pad(10).row();
		menuTable.add(new Container<>()); // pad out 1 cell
		menuTable.add(enableTutorialCheckbox).colspan(2).left().pad(10).row();

	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.PREFERENCE_CHANGED: {
				UserPreferences.PreferenceKey changedKey = (UserPreferences.PreferenceKey) msg.extraInfo;
				if (changedKey.equals(UserPreferences.PreferenceKey.ALLOW_HINTS)) {
					enableHintsCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(ALLOW_HINTS, "true")));
					return true;
				} else if (changedKey.equals(ENABLE_TUTORIAL)) {
					enableTutorialCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(ENABLE_TUTORIAL, "true")));
					return true;
				} else {
					return false;
				}
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	@Override
	public OptionsTabName getTabName() {
		return OptionsTabName.GAMEPLAY;
	}
}
