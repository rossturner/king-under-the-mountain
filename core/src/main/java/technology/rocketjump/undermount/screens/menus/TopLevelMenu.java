package technology.rocketjump.undermount.screens.menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.GameSaveMessage;
import technology.rocketjump.undermount.messaging.types.RequestSoundMessage;
import technology.rocketjump.undermount.persistence.PersistenceCallback;
import technology.rocketjump.undermount.persistence.SavedGameStore;
import technology.rocketjump.undermount.persistence.UserFileManager;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.ui.fonts.FontRepository;
import technology.rocketjump.undermount.ui.i18n.I18nRepo;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nUpdatable;
import technology.rocketjump.undermount.ui.i18n.LanguageType;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.ButtonStyle;
import technology.rocketjump.undermount.ui.widgets.I18nTextWidget;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import java.util.List;

@Singleton
public class TopLevelMenu implements Menu, I18nUpdatable {

	private final I18nTranslator i18nTranslator;
	private final UserPreferences userPreferences;
	private final I18nRepo i18nRepo;
	private final I18nTextWidget i18nTextWidget;
	private final FontRepository fontRepository;
	private final GuiSkinRepository guiSkinRepository;
	private final SavedGameStore savedGameStore;

	private Skin uiSkin;
	private Table menuTable;
	private Table leftColumn;
	private Table rightColumn;

	private final IconButton newGameButton;
	private final IconButton resumeGameButton;
	private final IconButton loadLatestGameButton;
	private final IconButton loadAnyGameButton;
	private final IconButton optionsButton;
	private final IconButton modsButton;
	private final IconButton quitButton;
	private SelectBox<LanguageType> languageSelect;
	private boolean gameStarted = false;
	private Texture logo;
	private boolean displayed = false;

	@Inject
	public TopLevelMenu(GuiSkinRepository guiSkinRepository, IconButtonFactory iconButtonFactory, MessageDispatcher messageDispatcher,
						I18nTranslator i18nTranslator, I18nRepo i18nRepo, UserFileManager userFileManager, UserPreferences userPreferences,
						TextureAtlasRepository textureAtlasRepository, SoundAssetDictionary soundAssetDictionary,
						FontRepository fontRepository, SavedGameStore savedGameStore) {
		this.guiSkinRepository = guiSkinRepository;
		this.uiSkin = guiSkinRepository.getDefault();
		this.i18nTranslator = i18nTranslator;
		this.i18nRepo = i18nRepo;
		this.userPreferences = userPreferences;
		this.fontRepository = fontRepository;
		this.savedGameStore = savedGameStore;


		menuTable = new Table(uiSkin);
		menuTable.setFillParent(false);
		menuTable.center();

		leftColumn = new Table(uiSkin);
		leftColumn.setFillParent(false);
		leftColumn.top();

		rightColumn = new Table(uiSkin);
		rightColumn.setFillParent(false);
		rightColumn.top();

		final SoundAsset startGameSound = soundAssetDictionary.getByName("GameStart");


		newGameButton = iconButtonFactory.create("MENU.NEW_GAME", null, Color.LIGHT_GRAY, ButtonStyle.EXTRA_WIDE);
		newGameButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.EMBARK_MENU);
		});

		resumeGameButton = iconButtonFactory.create("MENU.CONTINUE_GAME", null, Color.LIGHT_GRAY, ButtonStyle.EXTRA_WIDE);
		resumeGameButton.setAction(() -> {
			gameStarted = true;
			messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
			reset();
		});


		loadLatestGameButton = iconButtonFactory.create("MENU.CONTINUE_GAME", null, Color.LIGHT_GRAY, ButtonStyle.EXTRA_WIDE);
		loadLatestGameButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.TRIGGER_QUICKLOAD, (PersistenceCallback) wasSuccessful -> {
				if (wasSuccessful) {
					gameStarted = true;
				}
			});
			reset();
		});
		loadLatestGameButton.setOnClickSoundAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(startGameSound));
		});

		loadAnyGameButton = iconButtonFactory.create("MENU.LOAD_GAME", null, Color.LIGHT_GRAY, ButtonStyle.EXTRA_WIDE);
		loadAnyGameButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.LOAD_GAME_MENU);
		});

		optionsButton = iconButtonFactory.create("MENU.OPTIONS", null, Color.LIGHT_GRAY, ButtonStyle.EXTRA_WIDE);
		optionsButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.OPTIONS_MENU);
		});


		modsButton = iconButtonFactory.create("MENU.MODS", null, Color.LIGHT_GRAY, ButtonStyle.EXTRA_WIDE);
		modsButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.MODS_MENU);
		});

		quitButton = iconButtonFactory.create("MENU.QUIT", null, Color.LIGHT_GRAY, ButtonStyle.EXTRA_WIDE);
		quitButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.PERFORM_SAVE, new GameSaveMessage(false));
			Gdx.app.exit();
		});

		this.languageSelect = buildLanguageSelect(messageDispatcher, i18nRepo, userPreferences, uiSkin, this, textureAtlasRepository, fontRepository, guiSkinRepository);

		i18nTextWidget = null;
	}

	static SelectBox<LanguageType> buildLanguageSelect(MessageDispatcher messageDispatcher, I18nRepo i18nRepo,
													   UserPreferences userPreferences, Skin uiSkin, Menu parent,
													   TextureAtlasRepository textureAtlasRepository, FontRepository fontRepository,
													   GuiSkinRepository guiSkinRepository) {
		i18nRepo.init(textureAtlasRepository);
		String languageCode = userPreferences.getPreference(UserPreferences.PreferenceKey.LANGUAGE, "en-gb");
		List<LanguageType> allLanguages = i18nRepo.getAllLanguages();

		LanguageType selectedLanguage = null;
		for (LanguageType languageType : allLanguages) {
			if (languageType.getCode().equals(languageCode)) {
				selectedLanguage = languageType;
				break;
			}
		}
		if (selectedLanguage == null) {
			selectedLanguage = allLanguages.get(0);
		}

		SelectBox<LanguageType> languageSelect = new SelectBox<>(uiSkin);
		// Override font with unicode-guaranteed font to show east asian characters
		SelectBox.SelectBoxStyle style = new SelectBox.SelectBoxStyle(languageSelect.getStyle());
		style.font = fontRepository.getUnicodeFont().getBitmapFont();
		com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle listStyle = new com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle(style.listStyle);
		listStyle.font = fontRepository.getUnicodeFont().getBitmapFont();
		style.listStyle = listStyle;
		languageSelect.setStyle(style);

		Array<LanguageType> languageEntries = new Array<>();
		for (LanguageType language : allLanguages) {
			languageEntries.add(language);
		}

		languageSelect.setItems(languageEntries);
		languageSelect.setSelected(selectedLanguage);
		languageSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				LanguageType selectedLanguage = languageSelect.getSelected();
				changeLanguage(selectedLanguage, userPreferences, fontRepository, i18nRepo, messageDispatcher, guiSkinRepository);
				parent.reset();
			}
		});

		return languageSelect;
	}

	private static void changeLanguage(LanguageType selectedLanguage, UserPreferences userPreferences,
									   FontRepository fontRepository, I18nRepo i18nRepo, MessageDispatcher messageDispatcher,
									   GuiSkinRepository guiSkinRepository) {
		userPreferences.setPreference(UserPreferences.PreferenceKey.LANGUAGE, selectedLanguage.getCode());
		fontRepository.changeFontName(selectedLanguage.getFontName());
		guiSkinRepository.fontChanged();
		i18nRepo.setCurrentLanguage(selectedLanguage);
		messageDispatcher.dispatchMessage(MessageType.LANGUAGE_CHANGED);
	}

	@Override
	public void show() {
		logo = new Texture("assets/main_menu/Logo.png");
		displayed = true;
	}

	@Override
	public void hide() {
		logo.dispose();
		logo = null;
		displayed = false;
	}

	@Override
	public void populate(Table containerTable) {
		reset();
		containerTable.add(menuTable).center();
	}

	@Override
	public void reset() {
		menuTable.clearChildren();
		leftColumn.clearChildren();
		rightColumn.clearChildren();

		if (logo != null) {
			Image logoImage = new Image(logo);
//			logoImage.setScaling(Scaling.fit);
			menuTable.add(logoImage).width(logo.getWidth()).height(logo.getHeight()).colspan(2).center();
			menuTable.row();
		}

		Table languageRow = new Table(uiSkin);
		LanguageType selectedLanguage = i18nRepo.getCurrentLanguageType();
		languageRow.add(new Image(selectedLanguage.getIconSprite()));
		languageRow.add(languageSelect).padLeft(5);
		menuTable.add(languageRow).colspan(2).row();


		if (gameStarted) {
			leftColumn.add(resumeGameButton).pad(10).row();
		} else if (savedGameStore.hasSaveOrIsRefreshing()) {
			leftColumn.add(loadLatestGameButton).pad(10).row();
		}

		if (savedGameStore.hasSaveOrIsRefreshing()) {
			leftColumn.add(loadAnyGameButton).pad(10).row();
		}

		leftColumn.add(newGameButton).pad(10).row();

		rightColumn.add(optionsButton).pad(10).row();
		rightColumn.add(modsButton).pad(10).row();
		rightColumn.add(quitButton).pad(10).row();

		menuTable.add(leftColumn).top();
		menuTable.add(rightColumn).top().row();

//
//		menuTable.add(i18nTextWidget).pad(10).row();
	}

	@Override
	public void onLanguageUpdated() {
		languageSelect.setSelected(i18nRepo.getCurrentLanguageType());
	}

	public void savedGamesUpdated() {
		if (displayed) {
			this.reset();
		}
	}

	public void gameStarted() {
		this.gameStarted = true;
	}
}
