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

	public void toggleGroup(StockpileComponent stockpileComponent, StockpileGroup group, boolean enabled) {
		stockpileComponent.toggleGroup(group, enabled);
		for (ItemType itemType : itemTypeDictionary.getByStockpileGroup(group)) {
			toggleItem(stockpileComponent, itemType, enabled);
		}
	}

	public void toggleItem(StockpileComponent stockpileComponent, ItemType itemType, boolean enabled) {
		stockpileComponent.toggleItem(itemType, enabled);

		for (GameMaterial gameMaterial : gameMaterialDictionary.getByType(itemType.getPrimaryMaterialType())) {
			toggleMaterial(stockpileComponent, itemType, gameMaterial, enabled);
		}
	}

	private void toggleMaterial(StockpileComponent stockpileComponent, ItemType itemType, GameMaterial gameMaterial, boolean enabled) {
		stockpileComponent.toggleMaterial(itemType, gameMaterial, enabled);
	}

}
