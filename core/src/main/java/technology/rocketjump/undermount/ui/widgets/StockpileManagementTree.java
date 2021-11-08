package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import technology.rocketjump.undermount.entities.model.physical.creature.Race;
import technology.rocketjump.undermount.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rooms.StockpileComponentUpdater;
import technology.rocketjump.undermount.rooms.StockpileGroup;
import technology.rocketjump.undermount.rooms.StockpileGroupDictionary;
import technology.rocketjump.undermount.rooms.components.StockpileComponent;
import technology.rocketjump.undermount.ui.Scene2DUtils;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;

public class StockpileManagementTree extends Table {

	private final StockpileComponent stockpileComponent;
	private final StockpileComponentUpdater stockpileComponentUpdater;
	private final StockpileGroupDictionary stockpileGroupDictionary;
	private final ScrollPane scrollPane;
	private final Skin uiSkin;
	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;

	public StockpileManagementTree(Skin uiSkin, MessageDispatcher messageDispatcher, StockpileComponent stockpileComponent,
								   StockpileComponentUpdater stockpileComponentUpdater, StockpileGroupDictionary stockpileGroupDictionary,
								   I18nTranslator i18nTranslator, ItemTypeDictionary itemTypeDictionary, GameMaterialDictionary gameMaterialDictionary,
								   RaceDictionary raceDictionary, Race settlerRace) {
		this.uiSkin = uiSkin;
		this.stockpileComponent = stockpileComponent;
		this.stockpileComponentUpdater = stockpileComponentUpdater;
		this.stockpileGroupDictionary = stockpileGroupDictionary;
		this.i18nTranslator = i18nTranslator;
		this.messageDispatcher = messageDispatcher;

		Tree<StockpileTreeNode, String> treeRoot = new Tree<>(uiSkin);

		for (StockpileGroup stockpileGroup : stockpileGroupDictionary.getAll()) {
			StockpileTreeNode groupNode = new StockpileTreeNode();
			groupNode.setValue(new StockpileTreeValue(stockpileGroup));
			createCheckbox(groupNode, stockpileGroup.getI18nKey());
			treeRoot.add(groupNode);

			boolean allItemsDisabled = true;
			boolean allItemsEnabled = true;

			if (stockpileGroup.isIncludesCreatureCorpses()) {
				StockpileTreeNode corpseGroupNode = new StockpileTreeNode();
				corpseGroupNode.setValue(new StockpileTreeValue("Corpse group"));
				createCheckbox(corpseGroupNode, "CARCASS.GROUP");
				groupNode.add(corpseGroupNode);

				if (stockpileComponent.isAcceptingCorpses()) {
					allItemsDisabled = false;
				} else {
					allItemsEnabled = false;
				}

				boolean allRacesEnabled = true;
				boolean allRacesDisabled = true;

				for (Race race : raceDictionary.getAll()) {
					if (race.equals(settlerRace)) {
						continue;
					}
					StockpileTreeNode raceNode = new StockpileTreeNode();
					raceNode.setValue(new StockpileTreeValue(race));
					createCheckbox(raceNode, race.getI18nKey());
					corpseGroupNode.add(raceNode);

					if (stockpileComponent.isEnabled(race)) {
						allRacesDisabled = false;
					} else {
						allRacesEnabled = false;
					}
				}

				if (!allRacesDisabled && !allRacesEnabled) {
					groupNode.setExpanded(true);
					// setting these to cascade expansion up to parent
					allItemsEnabled = false;
					allItemsDisabled = false;
				}

			}

			for (ItemType itemType : itemTypeDictionary.getByStockpileGroup(stockpileGroup)) {
				StockpileTreeNode itemTypeNode = new StockpileTreeNode();
				itemTypeNode.setValue(new StockpileTreeValue(itemType));
				createCheckbox(itemTypeNode, itemType.getI18nKey());
				groupNode.add(itemTypeNode);

				if (stockpileComponent.isEnabled(itemType)) {
					allItemsDisabled = false;
				} else {
					allItemsEnabled = false;
				}

				boolean allMaterialsEnabled = true;
				boolean allMaterialsDisabled = true;

				for (GameMaterial material : gameMaterialDictionary.getByType(itemType.getPrimaryMaterialType())) {
					if (material.isUseMaterialTypeAsAdjective()) {
						// This is for wood types from bushes that should not show up anywhere, so skip them here
						continue;
					}
					StockpileTreeNode materialNode = new StockpileTreeNode();
					materialNode.setValue(new StockpileTreeValue(material, itemType));
					createCheckbox(materialNode, material.getI18nKey());
					itemTypeNode.add(materialNode);


					if (stockpileComponent.isEnabled(material, itemType)) {
						allMaterialsDisabled = false;
					} else {
						allMaterialsEnabled = false;
					}
				}

				if (!allMaterialsEnabled && !allMaterialsDisabled) {
					itemTypeNode.setExpanded(true);
					// setting these to cascade expansion up to parent
					allItemsEnabled = false;
					allItemsDisabled = false;
				}
			}

			if (!allItemsEnabled && !allItemsDisabled) {
				groupNode.setExpanded(true);
			}
		}

		scrollPane = Scene2DUtils.wrapWithScrollPane(treeRoot, uiSkin);
		this.add(scrollPane).width(350).height(400).left();
	}

	private void createCheckbox(StockpileTreeNode node, String i18nKey) {
		CheckBox checkbox = new CheckBox(i18nTranslator.getTranslatedString(i18nKey).toString(), uiSkin);
		checkbox.getLabelCell().padLeft(5f);
		checkbox.setProgrammaticChangeEvents(false);
		checkbox.addListener(new StockpileTreeNodeEventListener(node));
		node.setActor(checkbox);
		updateCheckedState(node);
	}

	private void updateCheckedState(StockpileTreeNode node) {
		if (node.getValue().isGroup()) {
			node.getActor().setChecked(stockpileComponent.isEnabled(node.getValue().group));
		} else if (node.getValue().isItemType()) {
			node.getActor().setChecked(stockpileComponent.isEnabled(node.getValue().itemType));
		} else if (node.getValue().isMaterial()) {
			node.getActor().setChecked(stockpileComponent.isEnabled(node.getValue().material, node.getValue().itemType));
		} else if (node.getValue().isCorpseGroup) {
			node.getActor().setChecked(stockpileComponent.isAcceptingCorpses());
		} else if (node.getValue().isRace()) {
			node.getActor().setChecked(stockpileComponent.isEnabled(node.getValue().race));
		}
 	}

	private void updateChildren(StockpileTreeNode node) {
		Array<StockpileTreeNode> children = node.getChildren();
		if (children != null && !children.isEmpty()) {
			for (StockpileTreeNode childNode : children) {
				updateCheckedState(childNode);
				updateChildren(childNode);
			}
		}
	}

	private class StockpileTreeNodeEventListener implements EventListener {


		private final StockpileTreeNode node;

		public StockpileTreeNodeEventListener(StockpileTreeNode node) {
			this.node = node;
		}

		@Override
		public boolean handle(Event event) {
			if (event instanceof ChangeListener.ChangeEvent) {
				StockpileTreeValue value = node.getValue();

				if (value.isGroup()) {
					stockpileComponentUpdater.toggleGroup(stockpileComponent, value.group, node.getActor().isChecked(), true);
					messageDispatcher.dispatchMessage(MessageType.STOCKPILE_SETTING_UPDATED, stockpileComponent.getParent());
				} else if (value.isItemType()) {
					stockpileComponentUpdater.toggleItem(stockpileComponent, value.itemType, node.getActor().isChecked(), true, true);
					messageDispatcher.dispatchMessage(MessageType.STOCKPILE_SETTING_UPDATED, stockpileComponent.getParent());
				} else if (value.isMaterial()) {
					stockpileComponentUpdater.toggleMaterial(stockpileComponent, value.itemType, value.material, node.getActor().isChecked(), true);
					messageDispatcher.dispatchMessage(MessageType.STOCKPILE_SETTING_UPDATED, stockpileComponent.getParent());
				} else if (value.isCorpseGroup) {
					stockpileComponentUpdater.toggleCorpseGroup(stockpileComponent, node.getActor().isChecked(), node.getParent().getValue().group, true, true);
					messageDispatcher.dispatchMessage(MessageType.STOCKPILE_SETTING_UPDATED, stockpileComponent.getParent());
				} else if (value.isRace()) {
					stockpileComponentUpdater.toggleRaceCorpse(stockpileComponent, value.race, node.getParent().getParent().getValue().group, node.getActor().isChecked(), true);
					messageDispatcher.dispatchMessage(MessageType.STOCKPILE_SETTING_UPDATED, stockpileComponent.getParent());
				}

				StockpileTreeNode parent = node.getParent();
				while (parent != null) {
					updateCheckedState(parent);
					parent = parent.getParent();
				}

				updateChildren(node);

				Array<StockpileTreeNode> children = node.getChildren();
				if (children != null && children.isEmpty()) {
					for (StockpileTreeNode childNode : children) {
						updateCheckedState(childNode);
						updateChildren(childNode);
					}
				}

				return true;
			} else {
				return false;
			}
		}

	}

	public static class StockpileTreeValue {

		public final StockpileGroup group;
		public final ItemType itemType;
		public final GameMaterial material;
		public final boolean isCorpseGroup;
		public final Race race;

		public StockpileTreeValue(StockpileGroup group) {
			this.group = group;
			this.itemType = null;
			this.material = null;
			this.isCorpseGroup = false;
			this.race = null;
		}

		public StockpileTreeValue(ItemType itemType) {
			this.group = null;
			this.itemType = itemType;
			this.material = null;
			this.isCorpseGroup = false;
			this.race = null;
		}

		public StockpileTreeValue(GameMaterial material, ItemType itemType) {
			this.group = null;
			this.itemType = itemType;
			this.material = material;
			this.isCorpseGroup = false;
			this.race = null;
		}

		public StockpileTreeValue(String unused) {
			this.group = null;
			this.itemType = null;
			this.material = null;
			this.isCorpseGroup = true;
			this.race = null;
		}

		public StockpileTreeValue(Race race) {
			this.group = null;
			this.itemType = null;
			this.material = null;
			this.isCorpseGroup = false;
			this.race = race;
		}

		public boolean isGroup() {
			return group != null;
		}

		public boolean isItemType() {
			return itemType != null && material == null;
		}

		public boolean isMaterial() {
			return material != null;
		}

		public boolean isRace() {
			return race != null;
		}
	}

	public static class StockpileTreeNode extends Tree.Node<StockpileTreeNode, StockpileTreeValue, CheckBox> {

	}
}
