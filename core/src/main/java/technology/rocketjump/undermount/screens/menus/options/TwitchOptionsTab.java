package technology.rocketjump.undermount.screens.menus.options;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.misc.twitch.TwitchDataStore;
import technology.rocketjump.undermount.misc.twitch.model.TwitchAccountInfo;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.I18nCheckbox;
import technology.rocketjump.undermount.ui.widgets.I18nLabel;
import technology.rocketjump.undermount.ui.widgets.I18nTextButton;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;

import static technology.rocketjump.undermount.messaging.MessageType.*;
import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.*;

@Singleton
public class TwitchOptionsTab implements OptionsTab, Telegraph {

	private static final String INTEGRATION_URL = "https://id.twitch.tv/oauth2/authorize?client_id=6gk8asspwcrt787lxge71kc418a3ng&redirect_uri=http://kingunderthemounta.in/twitch/&response_type=code&scope=channel:read:subscriptions&force_verify=true";
	private final I18nLabel pageTitle;
	private final I18nLabel accountLabel;
	private final I18nCheckbox viewersAsSettersCheckbox;
	private final I18nCheckbox prioritiseSubsCheckbox;
	private final I18nTextButton disconnectAccountButton;
	private TwitchAccountInfo accountInfo;
	private final Skin uiSkin;

	private boolean twitchEnabled;
	private final I18nCheckbox twitchEnabledCheckbox;

	private final I18nTextButton linkAccountButton;
	private final I18nTextButton codeSubmitButton;
	private final I18nLabel authCodeFailureLabel;
	private I18nLabel codeLabel;

	private boolean authCodeFailure = false;
	private final TextField codeInput;
	private Table menuTable;

	@Inject
	public TwitchOptionsTab(I18nWidgetFactory i18nWidgetFactory, GuiSkinRepository guiSkinRepository,
							UserPreferences userPreferences, MessageDispatcher messageDispatcher, TwitchDataStore twitchDataStore) {
		uiSkin = guiSkinRepository.getDefault();

		pageTitle = i18nWidgetFactory.createLabel("GUI.OPTIONS.TWITCH.TITLE");

		twitchEnabledCheckbox = i18nWidgetFactory.createCheckbox("GUI.OPTIONS.TWITCH.ENABLED");
		twitchEnabledCheckbox.setProgrammaticChangeEvents(false);
		twitchEnabled = Boolean.parseBoolean(userPreferences.getPreference(TWITCH_INTEGRATION_ENABLED, "false"));
		twitchEnabledCheckbox.setChecked(twitchEnabled);
		twitchEnabledCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				twitchEnabled = twitchEnabledCheckbox.isChecked();
				userPreferences.setPreference(TWITCH_INTEGRATION_ENABLED, String.valueOf(twitchEnabledCheckbox.isChecked()));
				messageDispatcher.dispatchMessage(PREFERENCE_CHANGED, TWITCH_INTEGRATION_ENABLED);
				reset();
			}
			return true;
		});

		this.accountInfo = twitchDataStore.getAccountInfo();

		linkAccountButton = i18nWidgetFactory.createTextButton("GUI.OPTIONS.TWITCH.LINK_ACCOUNT_BUTTON");
		linkAccountButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.net.openURI(INTEGRATION_URL);
			}
		});

		disconnectAccountButton = i18nWidgetFactory.createTextButton("GUI.OPTIONS.TWITCH.DISCONNECT");
		disconnectAccountButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				twitchDataStore.setCurrentToken(null);
				twitchDataStore.setAccountInfo(null);
			}
		});

		codeLabel = i18nWidgetFactory.createLabel("GUI.OPTIONS.TWITCH.CODE_LABEL");
		codeInput = new TextField("", uiSkin);

		codeSubmitButton = i18nWidgetFactory.createTextButton("GUI.OPTIONS.TWITCH.SUBMIT_BUTTON");
		codeSubmitButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				String code = codeInput.getText();
				if (!code.isEmpty()) {
					messageDispatcher.dispatchMessage(TWITCH_AUTH_CODE_SUPPLIED, code);
					reset();
				}
			}
		});

		authCodeFailureLabel = i18nWidgetFactory.createLabel("GUI.OPTIONS.TWITCH.GENERAL_ERROR");
		authCodeFailureLabel.setColor(HexColors.NEGATIVE_COLOR);

		accountLabel = i18nWidgetFactory.createLabel("GUI.OPTIONS.TWITCH.ACCOUNT_LABEL");

		viewersAsSettersCheckbox = i18nWidgetFactory.createCheckbox("GUI.OPTIONS.TWITCH.VIEWERS_AS_SETTLERS");
		viewersAsSettersCheckbox.setProgrammaticChangeEvents(false);
		viewersAsSettersCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(TWITCH_VIEWERS_AS_SETTLER_NAMES, "false")));
		viewersAsSettersCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				userPreferences.setPreference(TWITCH_VIEWERS_AS_SETTLER_NAMES, String.valueOf(viewersAsSettersCheckbox.isChecked()));
			}
			return true;
		});

		prioritiseSubsCheckbox = i18nWidgetFactory.createCheckbox("GUI.OPTIONS.TWITCH.PRIORITISE_SUBSCRIBERS");
		prioritiseSubsCheckbox.setProgrammaticChangeEvents(false);
		prioritiseSubsCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(TWITCH_PRIORITISE_SUBSCRIBERS, "false")));
		prioritiseSubsCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				userPreferences.setPreference(TWITCH_PRIORITISE_SUBSCRIBERS, String.valueOf(prioritiseSubsCheckbox.isChecked()));
			}
			return true;
		});

		messageDispatcher.addListener(this, MessageType.TWITCH_AUTH_CODE_FAILURE);
		messageDispatcher.addListener(this, MessageType.TWITCH_ACCOUNT_INFO_UPDATED);
	}

	@Override
	public void populate(Table menuTable) {
		this.menuTable = menuTable;
		reset();
	}

	private void reset() {
		if (menuTable == null) {
			return;
		}

		menuTable.clearChildren();
		menuTable.add(pageTitle).width(250).left().pad(10);
		menuTable.add(new Container<>()).colspan(2).row();


		menuTable.add(new Container<>()); // pad out 1 cell
		menuTable.add(twitchEnabledCheckbox).colspan(2).left().pad(10).row();

		if (twitchEnabled) {
			if (accountInfo != null) {

				menuTable.add(accountLabel).pad(10).right();
				menuTable.add(new Label(accountInfo.getLogin(), uiSkin)).left().pad(10);
				menuTable.add(disconnectAccountButton).center().row();

				menuTable.add(new Container<>()); // pad out 1 cell
				menuTable.add(viewersAsSettersCheckbox).colspan(2).left().pad(10).row();

				menuTable.add(new Container<>()); // pad out 1 cell
				menuTable.add(prioritiseSubsCheckbox).colspan(2).left().pad(10).row();

			} else {
				menuTable.add(new Container<>()); // pad out 1 cell
				menuTable.add(linkAccountButton).colspan(2).left().pad(10).row();

				menuTable.add(codeLabel).pad(10).right();
				menuTable.add(codeInput).colspan(2).fillX().pad(10).row();

				menuTable.add(new Container<>()); // pad out 1 cell
				menuTable.add(codeSubmitButton).colspan(2).left().pad(10).row();

				if (authCodeFailure) {
					menuTable.add(new Container<>()); // pad out 1 cell
					menuTable.add(authCodeFailureLabel).left().pad(10).row();
				}
			}
		}
	}

	@Override
	public OptionsTabName getTabName() {
		return OptionsTabName.TWITCH;
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.TWITCH_AUTH_CODE_FAILURE: {
				this.authCodeFailure = true;
				reset();
				return true;
			}
			case TWITCH_ACCOUNT_INFO_UPDATED: {
				this.accountInfo = (TwitchAccountInfo) msg.extraInfo;
				reset();
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}
}
