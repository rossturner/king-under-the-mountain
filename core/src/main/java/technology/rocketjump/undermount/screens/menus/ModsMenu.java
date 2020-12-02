package technology.rocketjump.undermount.screens.menus;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.messaging.InfoType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RequestSoundMessage;
import technology.rocketjump.undermount.modding.LocalModRepository;
import technology.rocketjump.undermount.modding.ModCompatibilityChecker;
import technology.rocketjump.undermount.modding.model.ParsedMod;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.ui.Scene2DUtils;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static technology.rocketjump.undermount.modding.ModCompatibilityChecker.Compatibility.INCOMPATIBLE;

@Singleton
public class ModsMenu implements Menu {

	private final Table outerTable;

	private final Table modsTable;
	private ScrollPane scrollPane;

	private final IconButton backButton;
	private final Skin uiSkin;
	private final MessageDispatcher messageDispatcher;
	private final I18nWidgetFactory i18NWidgetFactory;
	private final LocalModRepository modRepository;
	private final ModCompatibilityChecker modCompatibilityChecker;
	private final IconButtonFactory iconButtonFactory;

	private List<ParsedMod> modsInOrder = new ArrayList<>();

	private boolean useScrollPane;

	@Inject
	public ModsMenu(UserPreferences userPreferences, GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
					IconButtonFactory iconButtonFactory, I18nWidgetFactory i18NWidgetFactory, SoundAssetDictionary soundAssetDictionary,
					LocalModRepository modRepository, ModCompatibilityChecker modCompatibilityChecker) {
		this.uiSkin = guiSkinRepository.getDefault();
		this.messageDispatcher = messageDispatcher;
		this.i18NWidgetFactory = i18NWidgetFactory;
		this.modRepository = modRepository;
		this.iconButtonFactory = iconButtonFactory;
		this.modCompatibilityChecker = modCompatibilityChecker;

		final SoundAsset clickSoundAsset = soundAssetDictionary.getByName("MenuClick");

		outerTable = new Table(uiSkin);
		outerTable.setFillParent(false);
		outerTable.center();
		outerTable.background("default-rect");
//		outerTable.setDebug(true);

		modsTable = new Table(uiSkin);

		modsInOrder.addAll(modRepository.getActiveMods());

		List<ParsedMod> inactiveMods = new ArrayList<>();
		for (ParsedMod mod : modRepository.getAll()) {
			if (!modsInOrder.contains(mod)) {
				inactiveMods.add(mod);
			}
		}
		inactiveMods.sort(Comparator.comparing(o -> o.getInfo().getName()));
		Collections.reverse(inactiveMods);
		modsInOrder.addAll(inactiveMods);

		Collections.reverse(modsInOrder);

		useScrollPane = modsInOrder.size() > 10;
		if (useScrollPane) {
			scrollPane = Scene2DUtils.wrapWithScrollPane(modsTable, uiSkin);
		} else {
			scrollPane = null;
		}

		backButton = iconButtonFactory.create("GUI.BACK_LABEL", null, Color.LIGHT_GRAY, ButtonStyle.SMALL);
		backButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
			messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
			if (modRepository.hasChangesToApply()) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_INFO, InfoType.MOD_CHANGES_OUTSTANDING);
			}
		});

	}

	@Override
	public void show() {

	}

	@Override
	public void hide() {

	}

	@Override
	public void populate(Table containerTable) {
		containerTable.add(outerTable).center();
	}

	@Override
	public void reset() {
		outerTable.clearChildren();

		modsTable.clearChildren();

		modsTable.add(i18NWidgetFactory.createLabel("MODS.TABLE.ORDERING")).pad(10);
		modsTable.add(i18NWidgetFactory.createLabel("MODS.TABLE.ENABLED")).pad(10);
		modsTable.add(i18NWidgetFactory.createLabel("MODS.TABLE.NAME")).pad(10);
		modsTable.add(i18NWidgetFactory.createLabel("MODS.TABLE.VERSION")).pad(10);
		modsTable.add(i18NWidgetFactory.createLabel("MODS.TABLE.COMPATIBILITY")).pad(10);
		modsTable.row();

		for (int i = 0; i < modsInOrder.size(); i++) {
			final int index = i;
			ParsedMod mod = modsInOrder.get(index);
			final boolean isBaseMod = mod.getInfo().isBaseMod();

			if (isBaseMod) {
				modsTable.add(new Container<>());
			} else {
				Table orderingTable = new Table(uiSkin);
				IconOnlyButton upButton = iconButtonFactory.create("arrow-up");
				upButton.setAction(() -> {
					Collections.swap(modsInOrder, index, index - 1);
					orderChanged();
				});
				IconOnlyButton downButton = iconButtonFactory.create("arrow-down");
				downButton.setAction(() -> {
					Collections.swap(modsInOrder, index, index + 1);
					orderChanged();
				});

				if (index > 0) {
					orderingTable.add(upButton).pad(5);
				}

				if (index < modsInOrder.size() - 2) {
					orderingTable.add(downButton).pad(5);
				}

				modsTable.add(orderingTable);
			}

			ModCompatibilityChecker.Compatibility compatibility = modCompatibilityChecker.checkCompatibility(mod);

			CheckBox activeCheckbox = new CheckBox("", uiSkin);
			activeCheckbox.getLabelCell().padLeft(5f);
			activeCheckbox.setChecked(modRepository.getActiveMods().contains(mod));
			if (isBaseMod) {
				activeCheckbox.setDisabled(true);
			}
			activeCheckbox.addListener((event) -> {
				if (event instanceof ChangeListener.ChangeEvent) {
					boolean checked = activeCheckbox.isChecked();
					if (isBaseMod) {
						return true;
					} else if (checked) {
						modRepository.getActiveMods().add(mod);
						orderChanged();
					} else {
						modRepository.getActiveMods().remove(mod);
						orderChanged();
					}
				}
				return true;
			});
			if (compatibility.equals(INCOMPATIBLE)) {
				// Disabled unchecked box is unclear so just adding an empty cell
				modsTable.add(new Container<>());
			} else {
				modsTable.add(activeCheckbox);
			}

			modsTable.add(new Label(mod.getInfo().getName(), uiSkin));
			modsTable.add(new Label(mod.getInfo().getVersion().toString(), uiSkin));

			I18nLabel i18nLabel = i18NWidgetFactory.createLabel(compatibility.getI18nKey());
			// New label instance so it can be reused in the same stage/table
			modsTable.add(new Label(i18nLabel.getText(), uiSkin));
			modsTable.row();
		}

		if (useScrollPane) {
			outerTable.add(scrollPane).colspan(2).pad(10).left().row();
		} else {
			outerTable.add(modsTable).colspan(2).pad(10).left().row();
		}


		outerTable.add(backButton).colspan(2).pad(10).left();

		outerTable.row();

	}

	private void orderChanged() {
		List<ParsedMod> currentActiveMods = modRepository.getActiveMods();
		List<ParsedMod> newActiveMods = new ArrayList<>();

		for (ParsedMod mod : modsInOrder) {
			if (currentActiveMods.contains(mod)) {
				newActiveMods.add(mod);
			}
		}

		Collections.reverse(newActiveMods);
		modRepository.setActiveMods(newActiveMods);

		this.reset();
	}

}
