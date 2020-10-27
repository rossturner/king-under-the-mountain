package technology.rocketjump.undermount.screens;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.google.common.collect.ImmutableMap;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.crafting.CraftingRecipeDictionary;
import technology.rocketjump.undermount.crafting.model.CraftingRecipe;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.factories.ItemEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemGroup;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.CraftingTypeDictionary;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.materials.model.GameMaterial;
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
	private final Map<CraftingType, Map<ItemType, List<CraftingRecipe>>> producedItemTypesByCraftingRecipe = new HashMap<>();
	private final Map<CraftingType, Map<GameMaterial, List<CraftingRecipe>>> producedLiquidsByCraftingRecipe = new HashMap<>();
	private final Map<ItemType, Entity> exampleEntities = new HashMap<>();

	private final Map<ItemGroup, I18nLabel> groupLabels = new EnumMap<>(ItemGroup.class);
	private final EntityRenderer entityRenderer;

	private final Table scrollableTable;
	private final ScrollPane scrollableTablePane;

	private final Set<String> selectedRows = new HashSet<>();
	private final CraftingRecipeDictionary craftingRecipeDictionary;
	private final ItemEntityFactory itemEntityFactory;
	private final RoomTypeDictionary roomTypeDictionary;
	private boolean initialised = false;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final CraftingTypeDictionary craftingTypeDictionary;

	@Inject
	public CraftingManagementScreen(UserPreferences userPreferences, MessageDispatcher messageDispatcher,
									GuiSkinRepository guiSkinRepository, I18nWidgetFactory i18nWidgetFactory,
									I18nTranslator i18nTranslator, IconButtonFactory iconButtonFactory,
									CraftingTypeDictionary craftingTypeDictionary, FurnitureTypeDictionary furnitureTypeDictionary,
									RoomTypeDictionary roomTypeDictionary, CraftingRecipeDictionary craftingRecipeDictionary,
									ClickableTableFactory clickableTableFactory,
									EntityRenderer entityRenderer, ItemEntityFactory itemEntityFactory) {
		super(userPreferences, messageDispatcher, guiSkinRepository, i18nWidgetFactory, i18nTranslator, iconButtonFactory);
		this.clickableTableFactory = clickableTableFactory;
		this.entityRenderer = entityRenderer;
		this.craftingRecipeDictionary = craftingRecipeDictionary;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.itemEntityFactory = itemEntityFactory;
		this.roomTypeDictionary = roomTypeDictionary;
		this.craftingTypeDictionary = craftingTypeDictionary;

		scrollableTable = new Table(uiSkin);
		scrollableTablePane = Scene2DUtils.wrapWithScrollPane(scrollableTable, uiSkin);

		for (CraftingType craftingType : craftingTypeDictionary.getAll()) {
			displayedCraftingTypes.add(craftingType);
			craftingStationsByType.put(craftingType, new ArrayList<>());
		}
	}

	private void initalise() {
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


					Map<ItemType, List<CraftingRecipe>> recipesForProducedItems = new HashMap<>();
					Map<GameMaterial, List<CraftingRecipe>> recipesForProducedLiquids = new HashMap<>();
					for (CraftingRecipe craftingRecipe : craftingRecipeDictionary.getByCraftingType(craftingType)) {
						for (QuantifiedItemTypeWithMaterial recipeOutput : craftingRecipe.getOutput()) {
							if (recipeOutput.isLiquid()) {
								recipesForProducedLiquids.computeIfAbsent(recipeOutput.getMaterial(), a -> new ArrayList<>()).add(craftingRecipe);
							} else {
								recipesForProducedItems.computeIfAbsent(recipeOutput.getItemType(), a -> new ArrayList<>()).add(craftingRecipe);
							}
						}
					}

					producedItemTypesByCraftingRecipe.put(craftingType, recipesForProducedItems);
					producedLiquidsByCraftingRecipe.put(craftingType, recipesForProducedLiquids);
				} else {
					Logger.error("Could not find crafting type with name " + craftingTypeName + " from tag for " + furnitureType.getName() + " in " + this.getClass().getSimpleName());
				}
			}
		}

		GameContext nullContext = new GameContext();
		nullContext.setRandom(new RandomXS128());
		Set<ItemType> craftedItemTypes = producedItemTypesByCraftingRecipe.values().stream().flatMap(value -> value.keySet().stream()).collect(Collectors.toSet());
		for (ItemType craftedItemType : craftedItemTypes) {
			Entity entity = itemEntityFactory.createByItemType(craftedItemType, nullContext, false);

			// Use any hard-specified materials in recipe outputs
			for (Map<ItemType, List<CraftingRecipe>> itemTypeMap : producedItemTypesByCraftingRecipe.values()) {
				if (itemTypeMap.containsKey(craftedItemType)) {
					for (CraftingRecipe recipe : itemTypeMap.get(craftedItemType)) {
						for (QuantifiedItemTypeWithMaterial output : recipe.getOutput()) {
							if (craftedItemType.equals(output.getItemType()) && output.getMaterial() != null) {
								ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
								attributes.setMaterial(output.getMaterial());
							}
						}
					}
				}
			}

			exampleEntities.put(craftedItemType, entity);
		}

		onLanguageUpdated();
	}

	@Override
	public void reset() {
		if (!initialised) {
			initalise();
			initialised = true;
		}
		containerTable.clearChildren();
		containerTable.add(titleLabel).center().pad(5).row();
		scrollableTable.clearChildren();

		for (CraftingType craftingType : displayedCraftingTypes) {
			List<FurnitureType> craftingStations = craftingStationsByType.get(craftingType);
			addCraftingTypeRow(craftingType, craftingStations);

			boolean craftingTypeExpanded = true;

			if (craftingTypeExpanded) {
				for (Map.Entry<ItemType, List<CraftingRecipe>> producedItemEntry : producedItemTypesByCraftingRecipe.get(craftingType).entrySet()) {
					addProducedItemRow(producedItemEntry.getKey());
				}
				for (Map.Entry<GameMaterial, List<CraftingRecipe>> producedLiquidEntry : producedLiquidsByCraftingRecipe.get(craftingType).entrySet()) {

				}
			}
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

	private void addProducedItemRow(ItemType producedItemType) {
		Table rowContainerTable = new Table(uiSkin);
		rowContainerTable.add(new Container<>()).width(1 * INDENT_WIDTH);

		ClickableTable clickableRow = clickableTableFactory.create();
		clickableRow.setBackground("default-rect");
		clickableRow.pad(2);
		clickableRow.setFillParent(true);
		clickableRow.setAction(() -> {
			Logger.info("TODO: Click on this row");
		});

		// add item image
		EntityDrawable materialDrawable = new EntityDrawable(exampleEntities.get(producedItemType), entityRenderer);
		clickableRow.add(new Image(materialDrawable)).left().width(80).pad(5);

		// add item label
		clickableRow.add(new I18nTextWidget(i18nTranslator.getTranslatedString(producedItemType.getI18nKey()), uiSkin, messageDispatcher)).left().pad(5);

		// add production quota description
		clickableRow.add(new Label("MAINTAINING", uiSkin)).pad(5);

		TextField quantityInput = new TextField("1", uiSkin);
		quantityInput.setTextFieldFilter(new DigitFilter());
		quantityInput.setAlignment(Align.center);
		clickableRow.add(quantityInput).width(70);

		SelectBox<QuotaSetting> quotaSettingSelect = new SelectBox<>(uiSkin);
		Array<QuotaSetting> settingList = new Array<>();
		settingList.add(QuotaSetting.FIXED_AMOUNT);
		settingList.add(QuotaSetting.PER_SETTLER);
		quotaSettingSelect.setItems(settingList);
		quotaSettingSelect.setSelected(new RandomXS128().nextBoolean() ? QuotaSetting.FIXED_AMOUNT : QuotaSetting.PER_SETTLER);
		quotaSettingSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				QuotaSetting selected = quotaSettingSelect.getSelected();
				Logger.info("TODO: Something with " + selected);
			}
		});
		clickableRow.add(quotaSettingSelect);

		clickableRow.add(new Container<>()).right().expandX();

		rowContainerTable.add(clickableRow).width(DEFAULT_ROW_WIDTH - INDENT_WIDTH);
		scrollableTable.add(rowContainerTable).width(DEFAULT_ROW_WIDTH).right().row();
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

		QuotaSetting.FIXED_AMOUNT.i18nValue = i18nTranslator.getTranslatedString("QUOTA.FIXED_AMOUNT").toString();
		QuotaSetting.PER_SETTLER.i18nValue = i18nTranslator.getTranslatedString("QUOTA.PER_SETTLER").toString();
	}

	public enum QuotaSetting {

		FIXED_AMOUNT,
		PER_SETTLER;

		public String i18nValue;


		@Override
		public String toString() {
			return i18nValue == null ? this.name() : i18nValue;
		}
	}

	public static class DigitFilter implements TextField.TextFieldFilter {

		private char[] accepted;

		public DigitFilter() {
			accepted = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.'};
		}

		@Override
		public boolean acceptChar(TextField textField, char c) {
			for (char a : accepted)
				if (a == c) return true;
			return false;
		}
	}
}
