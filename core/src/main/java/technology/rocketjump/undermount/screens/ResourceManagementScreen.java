package technology.rocketjump.undermount.screens;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.common.collect.ImmutableMap;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.rendering.entities.EntityRenderer;
import technology.rocketjump.undermount.rooms.StockpileGroup;
import technology.rocketjump.undermount.rooms.StockpileGroupDictionary;
import technology.rocketjump.undermount.settlement.ItemTracker;
import technology.rocketjump.undermount.ui.Scene2DUtils;
import technology.rocketjump.undermount.ui.Selectable;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nWord;
import technology.rocketjump.undermount.ui.i18n.I18nWordClass;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Singleton
public class ResourceManagementScreen extends ManagementScreen {

	private static final float INDENT_WIDTH = 50f;
	private final ItemTracker itemTracker;
	private final StockpileGroupDictionary stockpileGroupDictionary;
	private final ClickableTableFactory clickableTableFactory;

	private final Map<StockpileGroup, I18nLabel> groupLabels = new HashMap<>();
	private final EntityRenderer entityRenderer;

	private final Table scrollableTable;
	private final ScrollPane scrollableTablePane;

	private final Set<String> selectedRows = new HashSet<>();

	@Inject
	public ResourceManagementScreen(UserPreferences userPreferences, MessageDispatcher messageDispatcher,
									GuiSkinRepository guiSkinRepository, I18nWidgetFactory i18nWidgetFactory,
									I18nTranslator i18nTranslator, IconButtonFactory iconButtonFactory,
									ItemTracker itemTracker, ClickableTableFactory clickableTableFactory,
									EntityRenderer entityRenderer, StockpileGroupDictionary stockpileGroupDictionary) {
		super(userPreferences, messageDispatcher, guiSkinRepository, i18nWidgetFactory, i18nTranslator, iconButtonFactory);
		this.itemTracker = itemTracker;
		this.clickableTableFactory = clickableTableFactory;
		this.entityRenderer = entityRenderer;
		this.stockpileGroupDictionary = stockpileGroupDictionary;

		for (StockpileGroup group : stockpileGroupDictionary.getAll()) {
			groupLabels.put(group, i18nWidgetFactory.createLabel(group.getI18nKey(), I18nWordClass.PLURAL));
		}

		scrollableTable = new Table(uiSkin);
		scrollableTablePane = Scene2DUtils.wrapWithScrollPane(scrollableTable, uiSkin);
	}

	@Override
	public void reset() {
		containerTable.clearChildren();
		containerTable.add(titleLabel).center().pad(5).row();
		scrollableTable.clearChildren();

		Map<StockpileGroup, Map<ItemType, Map<GameMaterial, Map<Long, Entity>>>> itemsByGroupByType = new LinkedHashMap<>();
		Map<ItemType, Map<GameMaterial, Map<Long, Entity>>> allByItemType = itemTracker.getAllByItemType();
		for (Map.Entry<ItemType, Map<GameMaterial, Map<Long, Entity>>> itemTypeMapEntry : allByItemType.entrySet()) {
			if (itemTypeMapEntry.getKey().getStockpileGroup() != null) {
				itemsByGroupByType.computeIfAbsent(itemTypeMapEntry.getKey().getStockpileGroup(), a -> new LinkedHashMap<>())
						.put(itemTypeMapEntry.getKey(), itemTypeMapEntry.getValue());
			}
		}


		for (StockpileGroup stockpileGroup : stockpileGroupDictionary.getAll()) {
			if (itemsByGroupByType.containsKey(stockpileGroup)) {
				Table groupTable = new Table(uiSkin);

				groupTable.add(groupLabels.get(stockpileGroup)).center().row();

				Map<ItemType, Map<GameMaterial, Map<Long, Entity>>> itemsByType = itemsByGroupByType.get(stockpileGroup);
				for (Map.Entry<ItemType, Map<GameMaterial, Map<Long, Entity>>> itemTypeMapEntry : itemsByType.entrySet()) {
					ItemType itemType = itemTypeMapEntry.getKey();
					String itemTypeRowName = "itemType:"+itemType.getItemTypeName();

					Entity firstEntity = itemTypeMapEntry.getValue().values().iterator().next().values().iterator().next();

					int totalQuantity = 0;
					int totalUnallocated = 0;
					for (Map<Long, Entity> entityMap : itemTypeMapEntry.getValue().values()) {
						for (Entity itemEntity : entityMap.values()) {
							ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
							totalQuantity += attributes.getQuantity();
							totalUnallocated += itemEntity.getOrCreateComponent(ItemAllocationComponent.class).getNumUnallocated();
						}
					}

					I18nText itemTypeDisplayName = i18nTranslator.getTranslatedString(itemType.getI18nKey());

					addRowToTable(groupTable, firstEntity, itemTypeRowName, itemTypeDisplayName, totalUnallocated, totalQuantity, 0, false);


					if (selectedRows.contains(itemTypeRowName)) {
						for (Map.Entry<GameMaterial, Map<Long, Entity>> gameMaterialMapEntry : itemTypeMapEntry.getValue().entrySet()) {
							GameMaterial material = gameMaterialMapEntry.getKey();
							String materialRowName = itemTypeRowName + ":material:" + material.getMaterialName();

							Entity firstMaterialEntity = gameMaterialMapEntry.getValue().values().iterator().next();

							I18nText materialDescription = i18nTranslator.getItemDescription(1, material, itemType);

							int totalMaterialQuantity = 0;
							int totalMaterialUnallocated = 0;

							for (Entity itemEntity : gameMaterialMapEntry.getValue().values()) {
								ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
								totalMaterialQuantity += attributes.getQuantity();
								totalMaterialUnallocated += itemEntity.getOrCreateComponent(ItemAllocationComponent.class).getNumUnallocated();
							}

							addRowToTable(groupTable, firstMaterialEntity, materialRowName, materialDescription, totalMaterialUnallocated, totalMaterialQuantity, 1, false);

							if (selectedRows.contains(materialRowName)) {
								for (Entity itemEntity : gameMaterialMapEntry.getValue().values()) {
									String entityRowName = materialRowName + ":" + itemEntity.getId();

									ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();

									addRowToTable(groupTable, itemEntity, entityRowName, i18nTranslator.getDescription(itemEntity), itemEntity.getOrCreateComponent(ItemAllocationComponent.class).getNumUnallocated(), attributes.getQuantity(), 2, true);
								}

							}
						}
					}

				}
				scrollableTable.add(groupTable).center().pad(5).row();

			}
		}

		containerTable.add(scrollableTablePane).pad(2);
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
		return "GUI.RESOURCE_MANAGEMENT.TITLE";
	}

	@Override
	public String getName() {
		return "RESOURCES";
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

}
