package technology.rocketjump.undermount.screens;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.crafting.CraftingRecipeDictionary;
import technology.rocketjump.undermount.crafting.model.CraftingRecipe;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.factories.ItemEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.CraftingTypeDictionary;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.rendering.entities.EntityRenderer;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.rooms.RoomType;
import technology.rocketjump.undermount.rooms.RoomTypeDictionary;
import technology.rocketjump.undermount.settlement.ItemTracker;
import technology.rocketjump.undermount.settlement.LiquidTracker;
import technology.rocketjump.undermount.settlement.SettlerTracker;
import technology.rocketjump.undermount.settlement.production.ProductionManager;
import technology.rocketjump.undermount.settlement.production.ProductionQuota;
import technology.rocketjump.undermount.ui.Scene2DUtils;
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
	public static final int DEFAULT_ROW_WIDTH = 1050;
	private final ItemType SHOW_LIQUID_ITEM_TYPE;

	private final ClickableTableFactory clickableTableFactory;
	private final EntityRenderer entityRenderer;
	private final CraftingRecipeDictionary craftingRecipeDictionary;
	private final ItemEntityFactory itemEntityFactory;
	private final RoomTypeDictionary roomTypeDictionary;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final CraftingTypeDictionary craftingTypeDictionary;
	private final ProductionManager productionManager;
	private final SettlerTracker settlerTracker;
	private final IconButtonFactory iconButtonFactory;

	private boolean initialised = false;
	private final List<CraftingType> displayedCraftingTypes = new ArrayList<>();
	private final Map<CraftingType, List<FurnitureType>> craftingStationsByType = new TreeMap<>();
	private final Map<FurnitureType, List<RoomType>> roomsForCraftingStations = new HashMap<>();
	private final Map<CraftingType, Map<ItemType, List<CraftingRecipe>>> producedItemTypesByCraftingRecipe = new HashMap<>();
	private final Map<CraftingType, Map<GameMaterial, List<CraftingRecipe>>> producedLiquidsByCraftingRecipe = new HashMap<>();

	private final Table scrollableTable;
	private final ScrollPane scrollableTablePane;

	private final Set<CraftingType> expandedCraftingTypes = new HashSet<>();
	private final Set<ItemType> expandedItemTypes = new HashSet<>();
	private final Set<GameMaterial> expandedLiquidMaterials = new HashSet<>();
	private final ItemTracker itemTracker;
	private final LiquidTracker liquidTracker;

	@Inject
	public CraftingManagementScreen(UserPreferences userPreferences, MessageDispatcher messageDispatcher,
									GuiSkinRepository guiSkinRepository, I18nWidgetFactory i18nWidgetFactory,
									I18nTranslator i18nTranslator, IconButtonFactory iconButtonFactory,
									CraftingTypeDictionary craftingTypeDictionary, FurnitureTypeDictionary furnitureTypeDictionary,
									RoomTypeDictionary roomTypeDictionary, CraftingRecipeDictionary craftingRecipeDictionary,
									ClickableTableFactory clickableTableFactory,
									EntityRenderer entityRenderer, ItemEntityFactory itemEntityFactory, ProductionManager productionManager,
									SettlerTracker settlerTracker, ItemTracker itemTracker, LiquidTracker liquidTracker, ItemTypeDictionary itemTypeDictionary) {
		super(userPreferences, messageDispatcher, guiSkinRepository, i18nWidgetFactory, i18nTranslator, iconButtonFactory);
		this.clickableTableFactory = clickableTableFactory;
		this.entityRenderer = entityRenderer;
		this.craftingRecipeDictionary = craftingRecipeDictionary;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.itemEntityFactory = itemEntityFactory;
		this.roomTypeDictionary = roomTypeDictionary;
		this.craftingTypeDictionary = craftingTypeDictionary;
		this.productionManager = productionManager;
		this.settlerTracker = settlerTracker;
		this.iconButtonFactory = iconButtonFactory;
		this.itemTracker = itemTracker;
		this.liquidTracker = liquidTracker;
		this.SHOW_LIQUID_ITEM_TYPE = itemTypeDictionary.getByName("Resource-Liquid-Example");

		scrollableTable = new Table(uiSkin);
		scrollableTablePane = Scene2DUtils.wrapWithScrollPane(scrollableTable, uiSkin);

		for (CraftingType craftingType : craftingTypeDictionary.getAll()) {
			displayedCraftingTypes.add(craftingType);
			craftingStationsByType.put(craftingType, new ArrayList<>());
		}
	}

	private void initialise() {
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

		onLanguageUpdated();
	}

	@Override
	public void reset() {
		if (!initialised) {
			initialise();
			initialised = true;
		}
		containerTable.clearChildren();
		containerTable.add(titleLabel).center().pad(5).row();
		scrollableTable.clearChildren();

		for (CraftingType craftingType : displayedCraftingTypes) {
			List<FurnitureType> craftingStations = craftingStationsByType.get(craftingType);
			addCraftingTypeRow(craftingType, craftingStations);

			boolean craftingTypeExpanded = expandedCraftingTypes.contains(craftingType);

			if (craftingTypeExpanded) {
				for (Map.Entry<ItemType, List<CraftingRecipe>> producedItemEntry : producedItemTypesByCraftingRecipe.get(craftingType).entrySet()) {
					addProducedItemRow(new CraftingOutput(producedItemEntry.getKey()));

					if (expandedItemTypes.contains(producedItemEntry.getKey())) {
						for (CraftingRecipe craftingRecipe : producedItemEntry.getValue()) {
							addCraftingRecipeRow(craftingRecipe);
						}
					}
				}
				for (Map.Entry<GameMaterial, List<CraftingRecipe>> producedLiquidEntry : producedLiquidsByCraftingRecipe.get(craftingType).entrySet()) {
					addProducedItemRow(new CraftingOutput(producedLiquidEntry.getKey()));

					if (expandedLiquidMaterials.contains(producedLiquidEntry.getKey())) {
						for (CraftingRecipe craftingRecipe : producedLiquidEntry.getValue()) {
							addCraftingRecipeRow(craftingRecipe);
						}
					}
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
			if (expandedCraftingTypes.contains(craftingType)) {
				expandedCraftingTypes.remove(craftingType);
			} else {
				expandedCraftingTypes.add(craftingType);
				// Also remove all expanded children
				for (ItemType childItemType : producedItemTypesByCraftingRecipe.get(craftingType).keySet()) {
					expandedItemTypes.remove(childItemType);
				}
				for (GameMaterial childLiquidMaterial : producedLiquidsByCraftingRecipe.get(craftingType).keySet()) {
					expandedLiquidMaterials.remove(childLiquidMaterial);
				}
			}
			reset();
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

	public static class CraftingOutput {

		public final boolean isLiquid;
		public final GameMaterial liquidMaterial;
		public final ItemType itemType;

		public CraftingOutput(GameMaterial liquidMaterial) {
			this.isLiquid = true;
			this.liquidMaterial = liquidMaterial;
			this.itemType = null;
		}

		public CraftingOutput(ItemType itemType) {
			this.isLiquid = false;
			this.liquidMaterial = null;
			this.itemType = itemType;
		}

	}

	private void addProducedItemRow(CraftingOutput craftingOutput) {
		Table rowContainerTable = new Table(uiSkin);
		rowContainerTable.add(new Container<>()).width(1 * INDENT_WIDTH);
		ProductionQuota currentProductionQuota;
		if (craftingOutput.isLiquid) {
			currentProductionQuota = productionManager.getProductionQuota(craftingOutput.liquidMaterial);
		} else {
			currentProductionQuota = productionManager.getProductionQuota(craftingOutput.itemType);
		}

		ClickableTable clickableRow = clickableTableFactory.create();
		clickableRow.setBackground("default-rect");
		clickableRow.pad(2);
		clickableRow.setFillParent(true);
		clickableRow.setAction(() -> {
			if (craftingOutput.isLiquid) {
				if (expandedLiquidMaterials.contains(craftingOutput.liquidMaterial)) {
					expandedLiquidMaterials.remove(craftingOutput.liquidMaterial);
				} else {
					expandedLiquidMaterials.add(craftingOutput.liquidMaterial);
				}
			} else {
				if (expandedItemTypes.contains(craftingOutput.itemType)) {
					expandedItemTypes.remove(craftingOutput.itemType);
				} else {
					expandedItemTypes.add(craftingOutput.itemType);
				}
			}
			reset();
		});

		EntityDrawable materialDrawable = new EntityDrawable(getExampleEntity(craftingOutput.itemType, craftingOutput.liquidMaterial), entityRenderer);
		clickableRow.add(new Image(materialDrawable)).left().width(80).pad(5);

		String i18nKey = craftingOutput.isLiquid ? craftingOutput.liquidMaterial.getI18nKey() : craftingOutput.itemType.getI18nKey();
		clickableRow.add(new I18nTextWidget(i18nTranslator.getTranslatedString(i18nKey), uiSkin, messageDispatcher)).left().pad(10);

		clickableRow.add(new I18nTextWidget(i18nTranslator.getTranslatedString("GUI.CRAFTING_MANAGEMENT.MAINTAINING_TEXT"), uiSkin, messageDispatcher)).pad(5);

		TextField quantityInput = new TextField("0", uiSkin);
		if (currentProductionQuota.isFixedAmount()) {
			quantityInput.setText(String.valueOf(currentProductionQuota.getFixedAmount()));
		} else {
			quantityInput.setText(String.valueOf(currentProductionQuota.getPerSettler()));
		}
		quantityInput.setTextFieldFilter(new DigitFilter());
		quantityInput.setAlignment(Align.center);
		clickableRow.add(quantityInput).width(70);

		SelectBox<QuotaSetting> quotaSettingSelect = new SelectBox<>(uiSkin);
		Array<QuotaSetting> settingList = new Array<>();
		settingList.add(QuotaSetting.FIXED_AMOUNT);
		settingList.add(QuotaSetting.PER_SETTLER);
		quotaSettingSelect.setItems(settingList);
		if (currentProductionQuota.isFixedAmount()) {
			quotaSettingSelect.setSelected(QuotaSetting.FIXED_AMOUNT);
		} else {
			quotaSettingSelect.setSelected(QuotaSetting.PER_SETTLER);
		}
		clickableRow.add(quotaSettingSelect);

		I18nTextWidget perSettlerTotalAmountHint = new I18nTextWidget(null, uiSkin, messageDispatcher);

		quantityInput.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				updateQuota(craftingOutput, getInputQuantityValue(quantityInput), quotaSettingSelect.getSelected(), perSettlerTotalAmountHint);
			}
		});
		quotaSettingSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				updateQuota(craftingOutput, getInputQuantityValue(quantityInput), quotaSettingSelect.getSelected(), perSettlerTotalAmountHint);
			}
		});

		clickableRow.add(new Container<>()).pad(5);


		updateHint(perSettlerTotalAmountHint, currentProductionQuota);
		clickableRow.add(perSettlerTotalAmountHint);

		I18nText actualAmountText = i18nTranslator.getTranslatedWordWithReplacements("GUI.CRAFTING_MANAGEMENT.TOTAL_QUANTITY_ACTUAL",
				Map.of("quantity", new I18nWord(String.valueOf(count(craftingOutput)))));
		clickableRow.add(new I18nTextWidget(actualAmountText, uiSkin, messageDispatcher)).right().expandX().padRight(100);

		rowContainerTable.add(clickableRow).width(DEFAULT_ROW_WIDTH - INDENT_WIDTH);
		scrollableTable.add(rowContainerTable).width(DEFAULT_ROW_WIDTH).right().row();
	}

	private void addCraftingRecipeRow(CraftingRecipe craftingRecipe) {
		Table rowContainerTable = new Table(uiSkin);
		rowContainerTable.add(new Container<>()).width(2 * INDENT_WIDTH);

		ClickableTable clickableRow = clickableTableFactory.create();
		clickableRow.setBackground("default-rect");
		clickableRow.pad(2);
		clickableRow.setFillParent(true);
		clickableRow.setAction(() -> {
			Logger.info("TODO: Clicked on " + craftingRecipe.toString());
		});


		for (int inputCursor = 0; inputCursor < craftingRecipe.getInput().size(); inputCursor++) {
			QuantifiedItemTypeWithMaterial inputRequirement = craftingRecipe.getInput().get(inputCursor);
			EntityDrawable itemDrawable = new EntityDrawable(getExampleEntity(inputRequirement.getItemType(), inputRequirement.getMaterial()), entityRenderer);
			clickableRow.add(new Image(itemDrawable)).left().pad(5);
			I18nText description = i18nTranslator.getItemDescription(inputRequirement.getQuantity(), inputRequirement.getMaterial(), inputRequirement.getItemType());
			clickableRow.add(new I18nTextWidget(description, uiSkin, messageDispatcher)).pad(5);

			if (inputCursor < craftingRecipe.getInput().size() - 1) {
				// add + for next input
				clickableRow.add(new Label("+", uiSkin)).pad(5);
			}
		}

		clickableRow.add(new Label("=>", uiSkin)).pad(5);

		for (int outputCursor = 0; outputCursor < craftingRecipe.getOutput().size(); outputCursor++) {
			QuantifiedItemTypeWithMaterial outputRequirement = craftingRecipe.getOutput().get(outputCursor);
			EntityDrawable entityDrawable = new EntityDrawable(getExampleEntity(outputRequirement.getItemType(), outputRequirement.getMaterial()), entityRenderer);
			clickableRow.add(new Image(entityDrawable)).left().pad(5);
			I18nText description = i18nTranslator.getItemDescription(outputRequirement.getQuantity(), outputRequirement.getMaterial(), outputRequirement.getItemType());
			clickableRow.add(new I18nTextWidget(description, uiSkin, messageDispatcher)).pad(5);

			if (outputCursor < craftingRecipe.getOutput().size() - 1) {
				// add + for next input
				clickableRow.add(new Label("+", uiSkin)).pad(5);
			}
		}

		IconOnlyButton enableDisableButton;
		boolean recipeEnabled = productionManager.isRecipeEnabled(craftingRecipe);
		if (recipeEnabled) {
			enableDisableButton = iconButtonFactory.create("check-mark");
			enableDisableButton.setForegroundColor(HexColors.POSITIVE_COLOR);
			enableDisableButton.setAction(() -> {
				productionManager.setRecipeEnabled(craftingRecipe, false);
				reset();
			});
		} else {
			enableDisableButton = iconButtonFactory.create("pause-button");
			enableDisableButton.setForegroundColor(HexColors.NEGATIVE_COLOR);
			enableDisableButton.setAction(() -> {
				productionManager.setRecipeEnabled(craftingRecipe, true);
				reset();
			});
		}


		clickableRow.add(enableDisableButton).right().pad(10).padRight(INDENT_WIDTH * 3).expandX();

		rowContainerTable.add(clickableRow).width(DEFAULT_ROW_WIDTH - (INDENT_WIDTH * 2));
		scrollableTable.add(rowContainerTable).width(DEFAULT_ROW_WIDTH).right().row();
	}

	private void updateQuota(CraftingOutput craftingOutput, float quantity, QuotaSetting quotaSetting, I18nTextWidget perSettlerTotalAmountHint) {
		ProductionQuota quota = new ProductionQuota();
		if (quotaSetting.equals(QuotaSetting.FIXED_AMOUNT)) {
			quota.setFixedAmount((int)quantity);
		} else {
			quota.setPerSettler(quantity);
		}

		if (craftingOutput.isLiquid) {
			productionManager.productionQuoteModified(craftingOutput.liquidMaterial, quota);
		} else {
			productionManager.productionQuoteModified(craftingOutput.itemType, quota);
		}

		updateHint(perSettlerTotalAmountHint, quota);
	}

	private void updateHint(I18nTextWidget perSettlerTotalAmountHint, ProductionQuota productionQuota) {
		int requiredAmount = productionQuota.getRequiredAmount(settlerTracker.getLiving().size());
		if (productionQuota.isFixedAmount() && requiredAmount > 0) {
			perSettlerTotalAmountHint.setVisible(false);
		} else {
			perSettlerTotalAmountHint.setError(requiredAmount == 0);
			I18nText actualAmountText = i18nTranslator.getTranslatedWordWithReplacements("GUI.CRAFTING_MANAGEMENT.TOTAL_QUANTITY_HINT",
					Map.of("quantity", new I18nWord(String.valueOf(requiredAmount))));
			perSettlerTotalAmountHint.setI18nText(actualAmountText);
			perSettlerTotalAmountHint.setVisible(true);

		}
	}

	public float getInputQuantityValue(TextField quantityInput) {
		String text = quantityInput.getText();
		float asFloat = 0f;
		try {
			asFloat = Float.valueOf(text);
		} catch (NumberFormatException e) {
			quantityInput.setText("0");
		}
		return asFloat;
	}

	private int count(CraftingOutput craftingOutput) {
		if (craftingOutput.isLiquid) {
			return (int) liquidTracker.getCurrentLiquidAmount(craftingOutput.liquidMaterial);
		} else {
			return itemTracker.getItemsByType(craftingOutput.itemType, false).stream()
					.map(entity -> ((ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getQuantity())
					.reduce(0, Integer::sum);
		}
	}

	static	GameContext nullContext = new GameContext();
	static {
		nullContext.setRandom(new RandomXS128());
	}
	private final Map<ItemType, Map<GameMaterial, Entity>> exampleEntities = new HashMap<>();

	private Entity getExampleEntity(ItemType itemType, GameMaterial material) {
		final ItemType itemTypeOrLiquidIte = itemType == null ? SHOW_LIQUID_ITEM_TYPE : itemType;

		Map<GameMaterial, Entity> materialsToEntitiesMap = exampleEntities.computeIfAbsent(itemTypeOrLiquidIte, a -> new HashMap<>());
		if (material == null) {
			// Get any entity in map or create one
			if (materialsToEntitiesMap.isEmpty()) {
				Entity entity = itemEntityFactory.createByItemType(itemTypeOrLiquidIte, nullContext, false);
				ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				materialsToEntitiesMap.put(attributes.getPrimaryMaterial(), entity);
				return entity;
			} else {
				return materialsToEntitiesMap.values().iterator().next();
			}
		} else {
			// Material is specified
			return materialsToEntitiesMap.computeIfAbsent(material, a -> {
				Entity entity = itemEntityFactory.createByItemType(itemTypeOrLiquidIte, nullContext, false);
				ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				attributes.setMaterial(material);
				return entity;
			});
		}
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
