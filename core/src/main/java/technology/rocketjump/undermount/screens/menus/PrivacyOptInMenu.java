package technology.rocketjump.undermount.screens.menus;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.ui.fonts.FontRepository;
import technology.rocketjump.undermount.ui.i18n.I18nRepo;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.LanguageType;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.ButtonStyle;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import java.util.ArrayList;
import java.util.List;

import static technology.rocketjump.undermount.screens.menus.TopLevelMenu.buildLanguageSelect;

@Singleton
public class PrivacyOptInMenu implements Menu {

	public static final int NUM_PRIVACY_OPT_IN_LINES = 10;
	private final List<Label> textLabels = new ArrayList<>();
	private final IconButton acceptButton;
	private final IconButton doNotAcceptButton;

	private final Skin uiSkin;
	private final I18nRepo i18nRepo;
	private Table menuTable;
	private final I18nTranslator i18nTranslator;

	private final SelectBox<LanguageType> languageSelect;

	@Inject
	public PrivacyOptInMenu(UserPreferences userPreferences, GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
							IconButtonFactory iconButtonFactory, I18nTranslator i18nTranslator, I18nRepo i18nRepo,
							I18nWidgetFactory i18NWidgetFactory, TextureAtlasRepository textureAtlasRepository, FontRepository fontRepository) {
		this.i18nTranslator = i18nTranslator;
		this.i18nRepo = i18nRepo;
		this.uiSkin = guiSkinRepository.getDefault();


		menuTable = new Table(uiSkin);
		menuTable.setFillParent(false);
		menuTable.center();
		menuTable.background("default-rect");

		for (int line = 1; line <= NUM_PRIVACY_OPT_IN_LINES; line++) {
			textLabels.add(i18NWidgetFactory.createLabel("PRIVACY.OPT_IN.LINE_"+line));
		}

		acceptButton = iconButtonFactory.create("PRIVACY.OPT_IN.ACCEPT_BUTTON", null, Color.LIGHT_GRAY, ButtonStyle.EXTRA_WIDE);
		acceptButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
			messageDispatcher.dispatchMessage(MessageType.CRASH_REPORTING_OPT_IN_MODIFIED, Boolean.TRUE);
		});

		doNotAcceptButton = iconButtonFactory.create("PRIVACY.OPT_IN.DO_NOT_ACCEPT_BUTTON", null, Color.LIGHT_GRAY, ButtonStyle.SMALL);
		doNotAcceptButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
			messageDispatcher.dispatchMessage(MessageType.CRASH_REPORTING_OPT_IN_MODIFIED, Boolean.FALSE);
		});

		this.languageSelect = buildLanguageSelect(messageDispatcher, i18nRepo, userPreferences, uiSkin, this, textureAtlasRepository, fontRepository, guiSkinRepository);
	}

	@Override
	public void show() {

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

		Table languageRow = new Table(uiSkin);
		languageRow.add(new Image(i18nRepo.getCurrentLanguageType().getIconSprite()));
		languageRow.add(languageSelect).padLeft(5);
		menuTable.add(languageRow).row();

		for (Label label : textLabels) {
			menuTable.add(label).left().pad(0, 10, 0, 10).row();
		}

		menuTable.add(acceptButton).pad(10).row();
		menuTable.add(doNotAcceptButton).pad(10).row();
	}

}
