package technology.rocketjump.undermount.rooms;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.rooms.components.StockpileComponent;

@Singleton
public class StockpileComponentUpdater {

	private final ItemTypeDictionary itemTypeDictionary;
	private final GameMaterialDictionary gameMaterialDictionary;

	@Inject
	public StockpileComponentUpdater(ItemTypeDictionary itemTypeDictionary, GameMaterialDictionary gameMaterialDictionary) {
		this.itemTypeDictionary = itemTypeDictionary;
		this.gameMaterialDictionary = gameMaterialDictionary;
	}

	public void toggleGroup(StockpileComponent stockpileComponent, StockpileGroup group, boolean enabled, boolean recurseToChildren) {
		stockpileComponent.toggleGroup(group, enabled);

		if (recurseToChildren) {
			for (ItemType itemType : itemTypeDictionary.getByStockpileGroup(group)) {
				toggleItem(stockpileComponent, itemType, enabled, false, true);
			}
		}
	}

	public void toggleItem(StockpileComponent stockpileComponent, ItemType itemType, boolean enabled, boolean recurseToParent, boolean recurseToChildren) {
		stockpileComponent.toggleItem(itemType, enabled);

		if (recurseToChildren) {
			for (GameMaterial gameMaterial : gameMaterialDictionary.getByType(itemType.getPrimaryMaterialType())) {
				stockpileComponent.toggleMaterial(itemType, gameMaterial, enabled);
				toggleMaterial(stockpileComponent, itemType, gameMaterial, enabled, false);
			}
		}

		if (recurseToParent) {
			boolean allSiblingsDisabled = true;

			for (ItemType siblingItem : itemTypeDictionary.getByStockpileGroup(itemType.getStockpileGroup())) {
				if (stockpileComponent.isEnabled(siblingItem)) {
					allSiblingsDisabled = false;
					break;
				}
			}

			if (enabled) {
				// when enabled, always enable parent(s)
				toggleGroup(stockpileComponent, itemType.getStockpileGroup(), true, false);
			}
			if (allSiblingsDisabled) {
				toggleGroup(stockpileComponent, itemType.getStockpileGroup(), false, false);
			}
		}
	}

	public void toggleMaterial(StockpileComponent stockpileComponent, ItemType itemType, GameMaterial gameMaterial, boolean enabled, boolean recurseToParent) {
		stockpileComponent.toggleMaterial(itemType, gameMaterial, enabled);

		if (recurseToParent) {
			boolean allSiblingsDisabled = true;

			for (GameMaterial material : gameMaterialDictionary.getByType(itemType.getPrimaryMaterialType())) {
				if (stockpileComponent.isEnabled(material, itemType)) {
					allSiblingsDisabled = false;
					break;
				}
			}

			if (enabled) {
				// when enabled, always enable parents
				toggleItem(stockpileComponent, itemType, true, true, false);
				toggleGroup(stockpileComponent, itemType.getStockpileGroup(), true, false);
			}
			if (allSiblingsDisabled) {
				toggleItem(stockpileComponent, itemType, false, true, false); // recurseToParent to toggle group is necessary
			}
		}
	}

}
