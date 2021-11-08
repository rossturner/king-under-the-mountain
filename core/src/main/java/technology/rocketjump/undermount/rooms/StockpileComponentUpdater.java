package technology.rocketjump.undermount.rooms;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.model.physical.creature.Race;
import technology.rocketjump.undermount.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.rooms.components.StockpileComponent;

@Singleton
public class StockpileComponentUpdater implements GameContextAware {

	private final ItemTypeDictionary itemTypeDictionary;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final RaceDictionary raceDictionary;
	private GameContext gameContext;

	@Inject
	public StockpileComponentUpdater(ItemTypeDictionary itemTypeDictionary, GameMaterialDictionary gameMaterialDictionary, RaceDictionary raceDictionary) {
		this.itemTypeDictionary = itemTypeDictionary;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.raceDictionary = raceDictionary;
	}

	public void toggleGroup(StockpileComponent stockpileComponent, StockpileGroup group, boolean enabled, boolean recurseToChildren) {
		stockpileComponent.toggleGroup(group, enabled);

		if (recurseToChildren) {
			for (ItemType itemType : itemTypeDictionary.getByStockpileGroup(group)) {
				toggleItem(stockpileComponent, itemType, enabled, false, true);
			}
			if (group.isIncludesCreatureCorpses()) {
				toggleCorpseGroup(stockpileComponent, enabled, group,false, true);
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
			boolean allGroupChildrenDisabled = allGroupChildrenDisabled(stockpileComponent, itemType.getStockpileGroup());

			if (enabled) {
				// when enabled, always enable parent(s)
				toggleGroup(stockpileComponent, itemType.getStockpileGroup(), true, false);
			}
			if (allGroupChildrenDisabled) {
				toggleGroup(stockpileComponent, itemType.getStockpileGroup(), false, false);
			}
		}
	}

	public void toggleCorpseGroup(StockpileComponent stockpileComponent, boolean enabled, StockpileGroup parentGroup, boolean recurseToParent, boolean recurseToChildren) {
		stockpileComponent.toggleAcceptingCorpses(enabled);

		if (recurseToChildren) {
			for (Race race : raceDictionary.getAll()) {
				if (race.equals(gameContext.getSettlementState().getSettlerRace())) {
					continue;
				}
				stockpileComponent.toggleRaceCorpse(race, enabled);
				toggleRaceCorpse(stockpileComponent, race, parentGroup, enabled, false);
			}
		}

		if (recurseToParent) {
			boolean allGroupChildrenDisabled = allGroupChildrenDisabled(stockpileComponent, parentGroup);

			if (enabled) {
				// when enabled, always enable parent(s)
				toggleGroup(stockpileComponent, parentGroup, true, false);
			}
			if (allGroupChildrenDisabled) {
				toggleGroup(stockpileComponent, parentGroup, false, false);
			}
		}
	}

	private boolean allGroupChildrenDisabled(StockpileComponent stockpileComponent, StockpileGroup group) {
		boolean allSiblingsDisabled = true;

		for (ItemType siblingItem : itemTypeDictionary.getByStockpileGroup(group)) {
			if (stockpileComponent.isEnabled(siblingItem)) {
				allSiblingsDisabled = false;
				break;
			}
		}

		if (group.isIncludesCreatureCorpses() && stockpileComponent.isAcceptingCorpses()) {
			allSiblingsDisabled = false;
		}
		return allSiblingsDisabled;
	}

	public void toggleRaceCorpse(StockpileComponent stockpileComponent, Race race, StockpileGroup parentGroup, boolean enabled, boolean recurseToParent) {
		stockpileComponent.toggleRaceCorpse(race, enabled);

		if (recurseToParent) {
			boolean allSiblingsDisabled = true;

			for (Race sibling : raceDictionary.getAll()) {
				if (race.equals(gameContext.getSettlementState().getSettlerRace())) {
					continue;
				}
				if (stockpileComponent.isEnabled(sibling)) {
					allSiblingsDisabled = false;
					break;
				}
			}

			if (enabled) {
				// when enabled, always enable parents
				toggleCorpseGroup(stockpileComponent, true, parentGroup, true, false);
				toggleGroup(stockpileComponent, parentGroup, true, false);
			}
			if (allSiblingsDisabled) {
				toggleCorpseGroup(stockpileComponent,false, parentGroup, true, false); // recurseToParent to toggle group is necessary
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

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
