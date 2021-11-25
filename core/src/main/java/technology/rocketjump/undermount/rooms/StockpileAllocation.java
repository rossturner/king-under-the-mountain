package technology.rocketjump.undermount.rooms;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.entities.behaviour.creature.CorpseBehaviour;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.Race;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

/**
 * This class is how a StockpileComponent keeps track of what is allocated to each tile
 * for the purposes of placing new assignments
 */
public class StockpileAllocation implements ChildPersistable {

	private final GridPoint2 position;

	private ItemType itemType;
	private GameMaterial gameMaterial;
	private Race raceCorpse; // could be race for corpse instead of itemtype and material

	private int incomingHaulingQuantity;
	private int quantityInTile;

	public StockpileAllocation(GridPoint2 position) {
		this.position = position;
	}

	public void refreshQuantityInTile(MapTile mapTile) {
		quantityInTile = 0;
		for (Entity entity : mapTile.getEntities()) {
			if (entity.getType().equals(EntityType.ITEM)) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getItemType().equals(itemType) && attributes.getMaterial(attributes.getItemType().getPrimaryMaterialType()).equals(gameMaterial)) {
					quantityInTile += attributes.getQuantity();
				}
			} else if (entity.getType().equals(EntityType.CREATURE) && entity.getBehaviourComponent() instanceof CorpseBehaviour) {
				CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (raceCorpse != null && raceCorpse.equals(attributes.getRace())) {
					quantityInTile += 1;
				}
			}
		}
	}

	public GridPoint2 getPosition() {
		return position;
	}

	public ItemType getItemType() {
		return itemType;
	}

	public void setItemType(ItemType itemType) {
		this.itemType = itemType;
	}

	public GameMaterial getGameMaterial() {
		return gameMaterial;
	}

	public void setGameMaterial(GameMaterial gameMaterial) {
		this.gameMaterial = gameMaterial;
	}

	public Race getRaceCorpse() {
		return raceCorpse;
	}

	public void setRaceCorpse(Race raceCorpse) {
		this.raceCorpse = raceCorpse;
	}

	public int getIncomingHaulingQuantity() {
		return incomingHaulingQuantity;
	}

	public void incrementIncomingHaulingQuantity(int extraIncomingHaulingQuantity) {
		this.incomingHaulingQuantity += extraIncomingHaulingQuantity;
	}

	public void decrementIncomingHaulingQuantity(int lessIncomingHaulingQuantity) {
		this.incomingHaulingQuantity -= lessIncomingHaulingQuantity;
	}

	public int getQuantityInTile() {
		return quantityInTile;
	}

	public void setQuantityInTile(int quantityInTile) {
		this.quantityInTile = quantityInTile;
	}

	public int getTotalQuantity() {
		return incomingHaulingQuantity + quantityInTile;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (itemType != null) {
			asJson.put("itemType", itemType.getItemTypeName());
		}
		if (gameMaterial != null) {
			asJson.put("material", gameMaterial.getMaterialName());
		}
		if (raceCorpse != null) {
			asJson.put("race", raceCorpse.getName());
		}
		if (incomingHaulingQuantity > 0) {
			asJson.put("incoming", incomingHaulingQuantity);
		}
		if (quantityInTile > 0) {
			asJson.put("inTile", quantityInTile);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		String itemTypeName = asJson.getString("itemType");
		if (itemTypeName != null) {
			this.itemType = relatedStores.itemTypeDictionary.getByName(itemTypeName);
			if (this.itemType == null) {
				throw new InvalidSaveException("Could not find item type by name " + itemTypeName);
			}
		}
		String materialName = asJson.getString("material");
		if (materialName != null) {
			this.gameMaterial = relatedStores.gameMaterialDictionary.getByName(materialName);
			if (this.gameMaterial == null) {
				throw new InvalidSaveException("Could not find game material by name " + materialName);
			}
		}
		String raceName = asJson.getString("race");
		if (raceName != null) {
			this.raceCorpse = relatedStores.raceDictionary.getByName(raceName);
			if (this.raceCorpse == null) {
				throw new InvalidSaveException("Could not find race with name " + raceName);
			}
		}
		this.incomingHaulingQuantity = asJson.getIntValue("incoming");
		this.quantityInTile = asJson.getIntValue("inTile");
	}
}
