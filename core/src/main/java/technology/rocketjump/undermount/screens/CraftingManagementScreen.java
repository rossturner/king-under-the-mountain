package technology.rocketjump.undermount.screens;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.google.common.collect.ImmutableMap;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.crafting.CraftingRecipeDictionary;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemGroup;
import technology.rocketjump.undermount.jobs.CraftingTypeDictionary;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.rendering.entities.EntityRenderer;
import technology.rocketjump.undermount.rooms.RoomType;
import technology.rocketjump.undermount.rooms.RoomTypeDictionary;
import technology.rocketjump.undermount.ui.Scene2DUtils;
import technology.rocketjump.undermount.ui.Selectable;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nUpdatable;
import technology.rocketjump.undermount.ui.i18n.I18nWord;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.ImageButton;
import technology.rocketjump.undermount.ui.widgets.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static technology.rocketjump.undermount.entities.tags.CraftingStationBehaviourTag.CRAFTING_STATION_BEHAVIOUR_TAGNAME;

@Singleton
public class CraftingManagementScreen extends ManagementScreen implements I18nUpdatable {

	private static final float INDENT_WIDTH = 50f;
	public static final int DEFAULT_ROW_WIDTH = 900;
	private final ClickableTableFactory clickableTableFactory;

	private final List<CraftingType> displayedCraftingTypes = new ArrayList<>();
	private final Map<CraftingType, List<FurnitureType>> craftingStationsByType = new TreeMap<>();
	private final Map<FurnitureType, List<RoomType>> roomsForCraftingStations = new HashMap<>();

	private final Map<ItemGroup, I18nLabel> groupLabels = new EnumMap<>(ItemGroup.class);
	private final EntityRenderer entityRenderer;

	private final Table scrollableTable;
	private final ScrollPane scrollableTablePane;

	private final Set<String> selectedRows = new HashSet<>();
	private final CraftingRecipeDictionary craftingRecipeDictionary;

	@Inject
	public CraftingManagementScreen(UserPreferences userPreferences, MessageDispatcher messageDispatcher,
									GuiSkinRepository guiSkinRepository, I18nWidgetFactory i18nWidgetFactory,
									I18nTranslator i18nTranslator, IconButtonFactory iconButtonFactory,
									CraftingTypeDictionary craftingTypeDictionary, FurnitureTypeDictionary furnitureTypeDictionary,
									RoomTypeDictionary roomTypeDictionary, CraftingRecipeDictionary craftingRecipeDictionary,
									ClickableTableFactory clickableTableFactory,
									EntityRenderer entityRenderer) {
		super(userPreferences, messageDispatcher, guiSkinRepository, i18nWidgetFactory, i18nTranslator, iconButtonFactory);
		this.clickableTableFactory = clickableTableFactory;
		this.entityRenderer = entityRenderer;
		this.craftingRecipeDictionary = craftingRecipeDictionary;

		scrollableTable = new Table(uiSkin);
		scrollableTablePane = Scene2DUtils.wrapWithScrollPane(scrollableTable, uiSkin);

		for (CraftingType craftingType : craftingTypeDictionary.getAll()) {
			displayedCraftingTypes.add(craftingType);
			craftingStationsByType.put(craftingType, new ArrayList<>());
		}

		onLanguageUpdated();

		for (FurnitureType furnitureType : furnitureTypeDictionary.getAll()) {
			if (furnitureType.getTags().containsKey(CRAFTING_STATION_BEHAVIOUR_TAGNAME)) {
				String craftingTypeName = furnitureType.getTags().get(CRAFTING_STATION_BEHAVIOUR_TAGNAME).get(0);
				CraftingType craftingType = craftingTypeDictionary.getByName(craftingTypeName);
				if (craftingType != null) {
					craftingStationsByType.get(craftingType).add(furnitureType);

					for (RoomType roomType : roomTypeDictionary.getAll()) {
						if (roomType.getFurnitureNames().contains(furnitureType.getName())) {
							roomsForCraftingStations.computeIfAbsent(furnitureType, a -> new ArrayList<>()).add(roomType);
						}
					}

				} else {
					Logger.error("Could not find crafting type with name " + craftingTypeName + " from tag for " + furnitureType.getName() + " in " + this.getClass().getSimpleName());
				}
			}
		}

	}

	@Override
	public void reset() {
		containerTable.clearChildren();
		containerTable.add(titleLabel).center().pad(5).row();
		scrollableTable.clearChildren();

		for (CraftingType craftingType : displayedCraftingTypes) {
			List<FurnitureType> craftingStations = craftingStationsByType.get(craftingType);
			addCraftingTypeRow(craftingType, craftingStations);
		}


		containerTable.add(scrollableTablePane).pad(2);
	}

	private void addCraftingTypeRow(CraftingType craftingType, List<FurnitureType> craftingStations) {

		ClickableTable clickableRow = clickableTableFactory.create();
		clickableRow.setBackground("default-rect");
		clickableRow.pad(2);
		clickableRow.setAction(() -> {
			Logger.info("TODO: Click on this row");
		});


		if (craftingType.getProfessionRequired() != null) {
			ImageButton imageButton = craftingType.getProfessionRequired().getImageButton();
			clickableRow.add(new Image(imageButton.getIconSprite())).left().padLeft(10);
		}
		clickableRow.add(new I18nTextWidget(i18nTranslator.getTranslatedString(craftingType.getI18nKey()), uiSkin, messageDispatcher)).left().pad(5).padLeft(10);

		String collectedCraftingStationText = craftingStations.stream()
				.map(craftingStation -> {
					String craftingStationName = i18nTranslator.getTranslatedString(craftingStation.getI18nKey()).toString();

					List<RoomType> roomTypes = roomsForCraftingStations.get(craftingStation);
					String roomTypesString = roomTypes.stream().map(roomType -> i18nTranslator.getTranslatedString(roomType.getI18nKey()).toString()).collect(Collectors.joining(", "));
					return craftingStationName + " (" + roomTypesString + ")";
				})
				.collect(Collectors.joining(", "));
		clickableRow.add(new Label(collectedCraftingStationText, uiSkin)).right().expandX().pad(5);

		scrollableTable.add(clickableRow).center().width(DEFAULT_ROW_WIDTH).height(64).row();
	}

	private void addRowToTable(Table groupTable, Entity itemEntity, String rowName, I18nText displayName, int unallocated, int total, int indents, boolean clickToEntity) {
		Table rowContainerTable = new Table(uiSkin);
		if (indents > 0) {
			rowContainerTable.add(new Container<>()).width(indents * INDENT_WIDTH);
		}

		ClickableTable clickableRow = clickableTableFactory.create();
		clickableRow.setBackground("default-rect");
		clickableRow.pad(2);
		if (clickToEntity) {
			clickableRow.setAction(() -> {
				Entity target = itemEntity;
				while (target.getLocationComponent().getContainerEntity() != null) {
					target = target.getLocationComponent().getContainerEntity();
				}
				Vector2 position = target.getLocationComponent().getWorldOrParentPosition();

				if (position != null) {
					messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
					messageDispatcher.dispatchMessage(MessageType.MOVE_CAMERA_TO, position);
					messageDispatcher.dispatchMessage(MessageType.CHOOSE_SELECTABLE, new Selectable(target, 0));
				} else {
					Logger.error("Attempting to move to entity with no position or container");
				}
			});
		} else {
			clickableRow.setAction(() -> {
				if (selectedRows.contains(rowName)) {
					selectedRows.remove(rowName);
				} else {
					selectedRows.add(rowName);
				}
				reset();
			});
		}

		EntityDrawable materialDrawable = new EntityDrawable(itemEntity, entityRenderer);
		clickableRow.add(new Image(materialDrawable)).center().width(80).pad(5);

		clickableRow.add(new I18nTextWidget(displayName, uiSkin, messageDispatcher)).left().width(400f - (indents * INDENT_WIDTH)).pad(2);

		clickableRow.add(new I18nTextWidget(i18nTranslator.getTranslatedWordWithReplacements(
				"GUI.RESOURCE_MANAGEMENT.UNALLOCATED_LABEL", ImmutableMap.of("count", new I18nWord(String.valueOf(unallocated)))), uiSkin, messageDispatcher)
		).center().width(100);
		clickableRow.add(new I18nTextWidget(i18nTranslator.getTranslatedWordWithReplacements(
				"GUI.SETTLER_MANAGEMENT.TOTAL_QUANTITY_LABEL", ImmutableMap.of("count", new I18nWord(String.valueOf(total)))), uiSkin, messageDispatcher)
		).center().width(100);

		rowContainerTable.add(clickableRow);
		groupTable.add(rowContainerTable).right().row();
	}

	@Override
	public String getTitleI18nKey() {
		return "GUI.CRAFTING_MANAGEMENT.TITLE";
	}

	@Override
	public String getName() {
		return "CRAFTING";
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

	@Override
	public void clearContextRelatedState() {
		selectedRows.clear();
	}

	@Override
	public void onLanguageUpdated() {
		displayedCraftingTypes.sort(Comparator.comparing(a -> i18nTranslator.getTranslatedString(a.getI18nKey()).toString()));
	}
}
