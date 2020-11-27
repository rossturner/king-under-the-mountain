package technology.rocketjump.undermount.screens;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.google.common.collect.ImmutableMap;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent;
import technology.rocketjump.undermount.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidEntityAttributes;
import technology.rocketjump.undermount.jobs.model.Profession;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RequestSoundMessage;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.settlement.SettlerTracker;
import technology.rocketjump.undermount.ui.Scene2DUtils;
import technology.rocketjump.undermount.ui.Selectable;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

import static technology.rocketjump.undermount.jobs.ProfessionDictionary.NULL_PROFESSION;
import static technology.rocketjump.undermount.ui.views.EntitySelectedGuiView.*;

@Singleton
public class SettlerManagementScreen extends ManagementScreen {

	public static final int UI_WIDTH_REQUIRED_PER_SETTLER = 600;
	private final ClickableTableFactory clickableTableFactory;
	private int numSettlerTablesPerRow = 3;
	private final MessageDispatcher messageDispatcher;
	private final SettlerTracker settlerTracker;
	private final EntityDrawableFactory entityDrawableFactory;
	private final Table sortingOptionsTable;
	private SettlerSorting selectedSortOption = SettlerSorting.BY_NAME;
	Map<SettlerSorting, I18nCheckbox> sortingCheckboxes = new LinkedHashMap<>();

	private final Table professionsTable;
	private final Table settlerTable;
	private final ScrollPane settlerScrollPane;

	private Set<Profession> selectedProfessions = new HashSet<>();

	@Inject
	public SettlerManagementScreen(UserPreferences userPreferences, MessageDispatcher messageDispatcher,
								   GuiSkinRepository guiSkinRepository, SettlerTracker settlerTracker,
								   I18nWidgetFactory i18nWidgetFactory,
								   EntityDrawableFactory entityDrawableFactory, I18nTranslator i18nTranslator,
								   ClickableTableFactory clickableTableFactory, IconButtonFactory iconButtonFactory, SoundAssetDictionary soundAssetDictionary) {
		super(userPreferences, messageDispatcher, guiSkinRepository, i18nWidgetFactory, i18nTranslator, iconButtonFactory);
		this.settlerTracker = settlerTracker;
		this.messageDispatcher = messageDispatcher;
		this.entityDrawableFactory = entityDrawableFactory;
		this.clickableTableFactory = clickableTableFactory;

		professionsTable = new Table(uiSkin);
		settlerTable = new Table(uiSkin);
		settlerScrollPane = Scene2DUtils.wrapWithScrollPane(settlerTable, uiSkin);

		final SoundAsset clickSoundAsset = soundAssetDictionary.getByName("MenuClick");

		sortingOptionsTable = new Table(uiSkin);
		for (SettlerSorting settlerSorting : SettlerSorting.values()) {
			I18nCheckbox checkbox = i18nWidgetFactory.createCheckbox("GUI.SETTLER_MANAGEMENT.SORT." + settlerSorting.name());
			sortingCheckboxes.put(settlerSorting, checkbox);
			checkbox.setChecked(this.selectedSortOption.equals(settlerSorting));
			checkbox.setProgrammaticChangeEvents(false);
			checkbox.addListener((event) -> {
				if (event instanceof ChangeListener.ChangeEvent) {
					messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
					if (checkbox.isChecked()) {
						this.selectedSortOption = settlerSorting;
						for (Map.Entry<SettlerSorting, I18nCheckbox> entry : sortingCheckboxes.entrySet()) {
							if (!entry.getKey().equals(settlerSorting)) {
								entry.getValue().setChecked(false);
							}
						}
						reset();
					} else {
						// unchecked, put it back to checked
						checkbox.setChecked(true);
					}
				}
				return true;
			});

			sortingOptionsTable.add(checkbox).pad(5).center();
		}


	}

	@Override
	public void reset() {
		Map<Profession, List<Entity>> byProfession = new TreeMap<>(Comparator.comparing(Profession::getName));
		for (Entity settler : settlerTracker.getLiving()) {
			ProfessionsComponent professionsComponent = settler.getComponent(ProfessionsComponent.class);
			if (professionsComponent != null) {
				for (ProfessionsComponent.QuantifiedProfession activeProfession : professionsComponent.getActiveProfessions()) {
					if (!activeProfession.getProfession().equals(NULL_PROFESSION)) {
						byProfession.computeIfAbsent(activeProfession.getProfession(), p -> new ArrayList<>()).add(settler);
					}
				}
			}
		}

		int living = settlerTracker.getLiving().size();
		int dead = settlerTracker.getDead().size();

		containerTable.clearChildren();
		containerTable.add(titleLabel).center().pad(5).row();

		I18nTextWidget livingLabel = new I18nTextWidget(i18nTranslator.getTranslatedWordWithReplacements("GUI.SETTLER_MANAGEMENT.LIVING_COUNTER",
				ImmutableMap.of("count", new I18nText(String.valueOf(living)))), uiSkin, messageDispatcher);
		I18nTextWidget deadLabel = new I18nTextWidget(i18nTranslator.getTranslatedWordWithReplacements("GUI.SETTLER_MANAGEMENT.DEAD_COUNTER",
				ImmutableMap.of("count", new I18nText(String.valueOf(dead)))), uiSkin, messageDispatcher);
		deadLabel.setColor(Color.RED);

		containerTable.add(livingLabel).left().row();
		if (dead > 0) {
			containerTable.add(deadLabel).left().row();
		}

		resetProfessionsTable(byProfession);
		resetSettlerTable();

		containerTable.add(professionsTable).height(120).row();

		containerTable.add(sortingOptionsTable).center().row();

		containerTable.add(settlerScrollPane).row();
	}

	@Override
	public void resize(int width, int height) {
		numSettlerTablesPerRow = (int)((float)width / ((float)UI_WIDTH_REQUIRED_PER_SETTLER * uiScale));
		super.resize(width, height);
	}

	@Override
	public void dispose() {

	}


	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public String getTitleI18nKey() {
		return "GUI.SETTLER_MANAGEMENT.TITLE";
	}

	@Override
	public String getName() {
		return "SETTLER_MANAGEMENT";
	}

	@Override
	public void clearContextRelatedState() {
		selectedProfessions.clear();
	}

	private void resetProfessionsTable(Map<Profession, List<Entity>> byProfession) {
		List<Profession> currentProfessions = new ArrayList<>(byProfession.keySet());
		currentProfessions.sort(Comparator.comparing(Profession::getName)); // TODO sort by i18n translation

		professionsTable.clearChildren();

		for (int cursor = 0; cursor < currentProfessions.size(); cursor++) {
			Table singleProfessionTable = new Table(uiSkin);

			Profession profession = currentProfessions.get(cursor);
			ImageButton imageButton = profession.getImageButton().clone();
			imageButton.setTogglable(true);
			imageButton.setAction(() -> {
				if (imageButton.getToggledOn()) {
					selectedProfessions.add(profession);
				} else {
					selectedProfessions.remove(profession);
				}
				resetSettlerTable();
			});

			singleProfessionTable.add(imageButton).center().pad(2).colspan(2).row();
			singleProfessionTable.add(i18nWidgetFactory.createLabel(profession.getI18nKey())).right();
			int professionCount = byProfession.get(profession).size();
			singleProfessionTable.add(new Label("(" + professionCount + ")", uiSkin)).left().row();

			professionsTable.add(singleProfessionTable);
			// TODO add new row after X profession items
		}
	}

	private void resetSettlerTable() {
		settlerTable.clearChildren();
		int counter = 0;

		List<Entity> livingSettlers = new ArrayList<>(settlerTracker.getLiving());

		switch (this.selectedSortOption) {
			case BY_NAME:
				livingSettlers.sort(Comparator.comparing(a -> ((HumanoidEntityAttributes)a.getPhysicalEntityComponent().getAttributes()).getName().toString()));
				break;
			case BY_UNHAPPINESS:
				livingSettlers.sort(Comparator.comparingInt(o -> o.getComponent(HappinessComponent.class).getNetModifier()));
				break;
			default:
				Logger.error("Unknown sorting option: " + this.selectedSortOption.name());
		}


		List<Entity> displayedSettlers = new ArrayList<>();
		List<Entity> inactiveSettlers = new ArrayList<>();

		for (Entity settler : livingSettlers) {
			boolean showAsActive = true;
			if (!selectedProfessions.isEmpty()) {
				ProfessionsComponent professionsComponent = settler.getComponent(ProfessionsComponent.class);
				if (professionsComponent == null) {
					continue;
				}

				showAsActive = professionsComponent.hasAnyActiveProfession(selectedProfessions);

//				boolean hasAllSelectedProfessions = true;
//				for (Profession selectedProfession : selectedProfessions) {
//					if (!professionsComponent.hasActiveProfession(selectedProfession)) {
//						hasAllSelectedProfessions = false;
//						break;
//					}
//				}
//
//				if (!hasAllSelectedProfessions) {
//					continue;
//				}
			}

			if (showAsActive) {
				displayedSettlers.add(settler);
			} else {
				inactiveSettlers.add(settler);
			}
		}

		displayedSettlers.addAll(inactiveSettlers);

		for (Entity settler : displayedSettlers) {
			ClickableTable singleSettlerTable = clickableTableFactory.create();
			singleSettlerTable.setBackground("default-rect");
			singleSettlerTable.pad(2);

			EntityDrawable settlerDrawable = entityDrawableFactory.create(settler);
			if (inactiveSettlers.contains(settler)) {
				settlerDrawable.setOverrideColor(HexColors.get("#2f2f2f"));
			}
			singleSettlerTable.add(new Image(settlerDrawable));

			Table nameHappinessBlockTable = new Table(uiSkin);

			Table nameTable = new Table(uiSkin);
			populateSettlerNameTable(settler, nameTable, i18nTranslator, uiSkin, gameContext, messageDispatcher);
			nameHappinessBlockTable.add(nameTable).left().row();

			Table happinessTable = new Table(uiSkin);
			Label modifierLabel = buildHappinessModifierLabel(settler.getComponent(HappinessComponent.class), uiSkin);
			happinessTable.add(new I18nTextWidget(i18nTranslator.getTranslatedString("HAPPINESS_MODIFIER.TITLE"), uiSkin, messageDispatcher));
			happinessTable.add(modifierLabel);
			nameHappinessBlockTable.add(happinessTable).left();

			singleSettlerTable.add(nameHappinessBlockTable).pad(4).top().left();


			Table needsTable = new Table(uiSkin);
			populateNeedsTable(needsTable, settler, i18nWidgetFactory.createNeedsLabels(), uiSkin);
			singleSettlerTable.add(needsTable).pad(4).top().left();

			singleSettlerTable.setWidth(UI_WIDTH_REQUIRED_PER_SETTLER);

			settlerTable.add(singleSettlerTable).left().fillX().pad(2);
			counter++;
			if (counter % numSettlerTablesPerRow == 0) {
				settlerTable.row();
			}

			singleSettlerTable.setAction(() -> {
				Vector2 position = settler.getLocationComponent().getWorldOrParentPosition();
				messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
				messageDispatcher.dispatchMessage(MessageType.MOVE_CAMERA_TO, position);
				messageDispatcher.dispatchMessage(MessageType.CHOOSE_SELECTABLE, new Selectable(settler, 0));
			});
		}


	}

	public enum SettlerSorting {

		BY_NAME, BY_UNHAPPINESS

	}

}
