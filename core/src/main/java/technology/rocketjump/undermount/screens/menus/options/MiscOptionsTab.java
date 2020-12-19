package technology.rocketjump.undermount.screens.menus.options;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
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
import technology.rocketjump.undermount.screens.ScreenManager;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.I18nCheckbox;
import technology.rocketjump.undermount.ui.widgets.I18nLabel;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.CRASH_REPORTING;
import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.MAIN_MENU_BACKGROUND_SCROLLING;

@Singleton
public class MiscOptionsTab implements OptionsTab, Telegraph {

	private final I18nLabel miscTitle;
	private final I18nCheckbox crashReportingCheckbox;
	private final I18nCheckbox mainMenuScrollingCheckbox;
	private final CheckBox stressTestCheckbox;

	@Inject
	public MiscOptionsTab(UserPreferences userPreferences, GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
						  IconButtonFactory iconButtonFactory, I18nWidgetFactory i18NWidgetFactory, SoundAssetDictionary soundAssetDictionary) {
		Skin uiSkin = guiSkinRepository.getDefault();
		final SoundAsset clickSoundAsset = soundAssetDictionary.getByName("MenuClick");

		miscTitle = i18NWidgetFactory.createLabel("GUI.OPTIONS.MISC.TITLE");

		stressTestCheckbox = new CheckBox("Stress test (1000 settlers)", uiSkin);
		stressTestCheckbox.getLabelCell().padLeft(5f);
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
	public void populate(Table menuTable) {
		// MISC
		menuTable.add(miscTitle).width(250).left().pad(10);
		menuTable.add(new Container<>()).colspan(2).row();

		menuTable.add(new Container<>()); // pad out 1 cell
		menuTable.add(crashReportingCheckbox).colspan(2).left().pad(10).row();
		menuTable.add(new Container<>()); // pad out 1 cell
		menuTable.add(mainMenuScrollingCheckbox).colspan(2).left().pad(10).row();

		if (GlobalSettings.DEV_MODE) {
			menuTable.add(new Container<>()); // pad out 1 cell
			menuTable.add(stressTestCheckbox).colspan(2).left().pad(10).row();
		}

	}

	@Override
	public OptionsTabName getTabName() {
		return OptionsTabName.MISC;
	}
}
