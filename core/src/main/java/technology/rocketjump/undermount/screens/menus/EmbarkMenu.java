package technology.rocketjump.undermount.screens.menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.entities.factories.SettlementNameGenerator;
import technology.rocketjump.undermount.messaging.InfoType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RequestSoundMessage;
import technology.rocketjump.undermount.messaging.types.StartNewGameMessage;
import technology.rocketjump.undermount.persistence.SavedGameInfo;
import technology.rocketjump.undermount.persistence.SavedGameStore;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nUpdatable;
import technology.rocketjump.undermount.ui.i18n.I18nWord;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.*;

import java.util.Map;
import java.util.Random;

@Singleton
public class EmbarkMenu implements Menu, I18nUpdatable {

	private final MessageDispatcher messageDispatcher;
	private final Table outerTable;
	private final I18nTranslator i18nTranslator;
	private final SavedGameStore savedGameStore;
	private final GameDialogDictionary gameDialogDictionary;

	private final Skin uiSkin;
	private final SettlementNameGenerator settlementNameGenerator;
	private final IconButton backButton;
	private final IconButton startButton;
	private final I18nLabel title;
	private final Table nameTable;
	private final TextField nameInput;
	private final ImageButton randomiseNameButton;
	private final Table seedTable;
	private final TextField seedInput;
	private final ImageButton randomiseSeedButton;
	private final I18nTextButton discordLink;
	private I18nTextWidget disclaimerText;
	private final Random random = new RandomXS128();

	@Inject
	public EmbarkMenu(GuiSkinRepository guiSkinRepository, IconButtonFactory iconButtonFactory, MessageDispatcher messageDispatcher,
					  I18nTranslator i18nTranslator, SavedGameStore savedGameStore, I18nWidgetFactory i18nWidgetFactory,
					  SettlementNameGenerator settlementNameGenerator, ImageButtonFactory imageButtonFactory,
					  SoundAssetDictionary soundAssetDictionary, GameDialogDictionary gameDialogDictionary) {
		this.uiSkin = guiSkinRepository.getDefault();
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.savedGameStore = savedGameStore;
		this.settlementNameGenerator = settlementNameGenerator;
		this.gameDialogDictionary = gameDialogDictionary;

		this.outerTable = new Table(uiSkin);
		outerTable.background("default-rect");

		title = i18nWidgetFactory.createLabel("GUI.EMBARK.TITLE");

		backButton = iconButtonFactory.create("GUI.BACK_LABEL", null, Color.LIGHT_GRAY, ButtonStyle.SMALL);
		backButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
		});

		startButton = iconButtonFactory.create("GUI.EMBARK.START", "flying-flag", HexColors.POSITIVE_COLOR, ButtonStyle.DEFAULT);
		final SoundAsset startGameSound = soundAssetDictionary.getByName("GameStart");
		startButton.setOnClickSoundAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(startGameSound));
		});

		discordLink = i18nWidgetFactory.createTextButton("GUI.EMBARK.DISCORD_LINK");
		discordLink.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.net.openURI("https://discord.gg/M57GrFp");
			}
		});

		onLanguageUpdated();

		I18nLabel nameLabel = i18nWidgetFactory.createLabel("GUI.EMBARK.SETTLEMENT_NAME");
		nameInput = new TextField("", uiSkin);
		randomiseNameButton = imageButtonFactory.getOrCreate("clockwise-rotation", true);
		randomiseNameButton.setAction(() -> nameInput.setText(settlementNameGenerator.create(random.nextLong())));


		I18nLabel seedLabel = i18nWidgetFactory.createLabel("GUI.EMBARK.MAP_SEED");
		seedInput = new TextField("", uiSkin);
		randomiseSeedButton = imageButtonFactory.getOrCreate("clockwise-rotation", true).clone();
		randomiseSeedButton.setAction(() -> seedInput.setText(String.valueOf(Math.abs(random.nextLong()))));

		nameTable = new Table(uiSkin);
		nameTable.add(nameLabel).right().pad(5);
		nameTable.add(nameInput).width(300).pad(5);
		nameTable.add(randomiseNameButton).left().pad(5);

		seedTable = new Table(uiSkin);
		seedTable.add(seedLabel).right().pad(5);
		seedTable.add(seedInput).width(300).pad(5);
		seedTable.add(randomiseSeedButton).left().pad(5);


		startButton.setAction(() -> {
			String settlementName = getSettlementName();
			SavedGameInfo existingSave = savedGameStore.getByName(settlementName);
			if (settlementName.isBlank()) {
				ModalDialog dialog = gameDialogDictionary.getInfoDialog(InfoType.SETTLEMENT_NAME_NOT_SPECIFIED);
				messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, dialog);
			} else if (existingSave != null) {
				NotificationDialog dialog = new NotificationDialog(
						i18nTranslator.getTranslatedString("GUI.DIALOG.INFO_TITLE"),
						uiSkin,
						messageDispatcher
				);
				dialog.withText(i18nTranslator.getTranslatedWordWithReplacements("GUI.DIALOG.SETTLEMENT_NAME_ALREADY_IN_USE",
						Map.of("name", new I18nWord(settlementName)))
					.breakAfterLength(i18nTranslator.getCurrentLanguageType().getBreakAfterLineLength()));

				dialog.withButton(i18nTranslator.getTranslatedString("GUI.DIALOG.OK_BUTTON"), (Runnable) () -> {
					messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
					messageDispatcher.dispatchMessage(MessageType.START_NEW_GAME, new StartNewGameMessage(getSettlementName(), parseSeed()));
				});
				dialog.withButton(i18nTranslator.getTranslatedString("GUI.DIALOG.CANCEL_BUTTON"));
				messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, dialog);
			} else {
				messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
				messageDispatcher.dispatchMessage(MessageType.START_NEW_GAME, new StartNewGameMessage(getSettlementName(), parseSeed()));
			}
		});
	}

	@Override
	public void show() {
		reset();
	}

	@Override
	public void hide() {

	}

	@Override
	public void populate(Table containerTable) {
		outerTable.clearChildren();

		outerTable.add(title).center().pad(5).colspan(3).row();

		outerTable.add(nameTable).center().pad(5).colspan(3).row();
		outerTable.add(seedTable).center().pad(5).colspan(3).row();

		outerTable.add(disclaimerText).center().pad(30).colspan(3).row();

		outerTable.add(backButton).left().pad(5);
		outerTable.add(discordLink).center().pad(5);
		outerTable.add(startButton).right().pad(5).row();

		containerTable.add(outerTable);
	}

	@Override
	public void reset() {
		this.nameInput.setText(settlementNameGenerator.create(random.nextLong()));
		this.seedInput.setText(String.valueOf(Math.abs(random.nextLong())));
	}

	@Override
	public void onLanguageUpdated() {
		I18nText disclaimerContent = i18nTranslator.getTranslatedString("GUI.EMBARK.DISCLAIMER");
		disclaimerContent.breakAfterLength(i18nTranslator.getCurrentLanguageType().getBreakAfterLineLength());
		disclaimerText = new I18nTextWidget(disclaimerContent, uiSkin, messageDispatcher);
	}

	private long parseSeed() {
		String seedText = seedInput.getText().trim();
		if (StringUtils.isNumeric(seedText)) {
			if (seedText.length() > 18) {
				seedText = seedText.substring(0, 18);
			}
			return Long.parseLong(seedText);
		} else {
			long hash = 0;
			for (char c : seedText.toCharArray()) {
				hash = 31L*hash + c;
			}
			return hash;
		}
	}

	private String getSettlementName() {
		return nameInput.getText().trim();
	}

}
