package technology.rocketjump.undermount.entities.components;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.entities.behaviour.creature.SettlerBehaviour;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemHoldPosition;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.*;

import static technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation.DOWN;
import static technology.rocketjump.undermount.entities.components.ItemAllocation.Purpose.HELD_IN_INVENTORY;
import static technology.rocketjump.undermount.entities.model.EntityType.ITEM;

public class InventoryComponent implements EntityComponent, Destructible {

	private Map<Long, InventoryEntry> inventoryEntries = new HashMap<>();

	/**
	 * This flag is used to determine if items added to the inventory should default to being automatically fully allocated (false)
	 * or not allocated when added so they can be re-allocated elsewhere (true)
	 */
	private boolean itemsUnallocated = false;

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		for (InventoryComponent.InventoryEntry inventoryEntry : new ArrayList<>(getInventoryEntries())) {
			if (inventoryEntry.entity.getBehaviourComponent() instanceof SettlerBehaviour) {
				SettlerBehaviour settlerBehaviour = (SettlerBehaviour) inventoryEntry.entity.getBehaviourComponent();
				if (settlerBehaviour.getCurrentGoal() != null && settlerBehaviour.getCurrentGoal().getCurrentAction() != null) {
					settlerBehaviour.getCurrentGoal().getCurrentAction().actionInterrupted(gameContext);
				}
			} else if (inventoryEntry.entity.getType().equals(ITEM)) {
				ItemEntityAttributes itemAttributes = (ItemEntityAttributes) inventoryEntry.entity.getPhysicalEntityComponent().getAttributes();
				// Dump out items to floor
				List<MapTile> parentTiles = new ArrayList<>();
				MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldOrParentPosition());
				if (parentTile == null) {
					// Not on a valid tile, just destroy inventory entity
					messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, inventoryEntry.entity);
				} else {
					parentTiles.add(parentTile);
					if (parentEntity.getType().equals(EntityType.FURNITURE)) {
						FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
						for (GridPoint2 extraTileOffset : attributes.getCurrentLayout().getExtraTiles()) {
							MapTile extraTile = gameContext.getAreaMap().getTile(parentTile.getTilePosition().cpy().add(extraTileOffset));
							if (extraTile != null) {
								parentTiles.add(extraTile);
							}
						}
					}

					MapTile targetTile = pickEmptyTileOrRandom(parentTiles, gameContext.getRandom());
					remove(inventoryEntry.entity.getId());

					Entity matchingInTile = targetTile.getItemMatching(itemAttributes);
					if (matchingInTile == null) {
						// Just place into tile
						inventoryEntry.entity.getLocationComponent().setWorldPosition(targetTile.getWorldPositionOfCenter().cpy(), false);
						inventoryEntry.entity.getLocationComponent().setFacing(DOWN.toVector2());
						itemAttributes.setItemPlacement(ItemPlacement.ON_GROUND);
						messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, inventoryEntry.entity);
					} else {
						// Merge into tile
						ItemEntityAttributes itemInTileAttributes = (ItemEntityAttributes) matchingInTile.getPhysicalEntityComponent().getAttributes();
						int newQuantity = Math.min(itemInTileAttributes.getQuantity() + itemAttributes.getQuantity(), itemAttributes.getItemType().getMaxStackSize());
						itemInTileAttributes.setQuantity(newQuantity);
						messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, matchingInTile);
						messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, inventoryEntry.entity);
					}
				}
			} else {
				messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, inventoryEntry.entity);
			}
		}
	}

	public void destroyAllEntities(MessageDispatcher messageDispatcher) {
		for (InventoryComponent.InventoryEntry inventoryEntry : new ArrayList<>(getInventoryEntries())) {
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, inventoryEntry.entity);
		}
	}

	private MapTile pickEmptyTileOrRandom(List<MapTile> parentTiles, Random random) {
		for (MapTile parentTile : parentTiles) {
			if (!parentTile.hasItem()) {
				return parentTile;
			}
		}
		return parentTiles.get(random.nextInt(parentTiles.size()));
	}

	public InventoryEntry findByItemType(ItemType itemType, GameClock gameClock) {
		for (InventoryEntry entry : inventoryEntries.values()) {
			Entity item = entry.entity;
			if (ITEM.equals(item.getType())) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) item.getPhysicalEntityComponent().getAttributes();
				if (itemType.equals(attributes.getItemType())) {
					entry.touch(gameClock);
					return entry;
				}
			}
		}
		return null;
	}

	public InventoryEntry findByItemTypeAndMaterial(ItemType itemType, GameMaterial material, GameClock gameClock) {
		for (InventoryEntry entry : inventoryEntries.values()) {
			Entity item = entry.entity;
			if (ITEM.equals(item.getType())) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) item.getPhysicalEntityComponent().getAttributes();
				if (itemType.equals(attributes.getItemType()) && material.equals(attributes.getMaterial(material.getMaterialType()))) {
					entry.touch(gameClock);
					return entry;
				}
			}
		}
		return null;
	}


	public InventoryEntry add(Entity entityToAdd, Entity parentEntity, MessageDispatcher messageDispatcher, GameClock gameClock) {
		return add(entityToAdd, parentEntity, messageDispatcher, gameClock, null);
	}

	public InventoryEntry add(Entity entityToAdd, Entity parentEntity, MessageDispatcher messageDispatcher, GameClock gameClock, ItemHoldPosition preferredPosition) {
		if (entityToAdd.getType().equals(ITEM)) {
			ItemEntityAttributes attributesToAdd = (ItemEntityAttributes) entityToAdd.getPhysicalEntityComponent().getAttributes();

			// Possibly merge into existing item in inventory
			InventoryEntry matchingEntry = findMatchingEntry(attributesToAdd);
			if (matchingEntry != null) {
				// Try merging into existing item
				ItemEntityAttributes matchingItemAttributes = (ItemEntityAttributes) matchingEntry.entity.getPhysicalEntityComponent().getAttributes();
				if (matchingItemAttributes.getQuantity() + attributesToAdd.getQuantity() <= attributesToAdd.getItemType().getMaxStackSize()) {
					// Can merge into existing item
					matchingItemAttributes.setQuantity(matchingItemAttributes.getQuantity() + attributesToAdd.getQuantity());
					if (!itemsUnallocated) {
						ItemAllocationComponent itemAllocationComponent = matchingEntry.entity.getOrCreateComponent(ItemAllocationComponent.class);
						ItemAllocation allocation = itemAllocationComponent.getAllocationForPurpose(HELD_IN_INVENTORY);
						if (allocation != null) {
							allocation.setAllocationAmount(matchingItemAttributes.getQuantity());
						} else {
							Logger.error("Could not find expected allocation to adjust quantity of");
						}
					}

					messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, entityToAdd);
					messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, matchingEntry.entity);
					inventoryEntityModified(matchingEntry.entity, gameClock);
					return matchingEntry;
				} // else can't merge into existing item, fall through to addition as new item below
			}

			// Add as new item in inventory
			attributesToAdd.setItemPlacement(ItemPlacement.ON_GROUND);
			if (!itemsUnallocated) {
				entityToAdd.getOrCreateComponent(ItemAllocationComponent.class).createAllocation(attributesToAdd.getQuantity(), parentEntity, HELD_IN_INVENTORY);
			}
			return addEntity(entityToAdd, parentEntity, messageDispatcher, gameClock, preferredPosition);
		} else {
			return addEntity(entityToAdd, parentEntity, messageDispatcher, gameClock, preferredPosition);
		}
	}

	public void inventoryEntityModified(Entity entityCurrentlyInInventory, GameClock gameClock) {
		InventoryEntry entry = inventoryEntries.get(entityCurrentlyInInventory.getId());
		if (entry != null) {
			entry.touch(gameClock);
		}
	}

	public Entity remove(long entityToRemoveId) {
		InventoryEntry removed = inventoryEntries.remove(entityToRemoveId);
		if (removed == null) {
			return null;
		} else {
			removed.entity.getLocationComponent().setContainerEntity(null);
			if (removed.entity.getType().equals(ITEM)) {
				ItemAllocationComponent itemAllocationComponent = removed.entity.getOrCreateComponent(ItemAllocationComponent.class);
				itemAllocationComponent.cancelAll();
			}
			return removed.entity;
		}
	}

	public void setItemsUnallocated(boolean itemsUnallocated) {
		this.itemsUnallocated = itemsUnallocated;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		// Cloning inventory component does not clone contained items!
		return new InventoryComponent();
	}

	public Entity getById(long entityId) {
		InventoryEntry entry = inventoryEntries.get(entityId);
		if (entry == null) {
			return null;
		} else {
			return entry.entity;
		}
	}

	public Collection<InventoryEntry> getInventoryEntries() {
		return inventoryEntries.values();
	}

	public boolean isEmpty() {
		return inventoryEntries.isEmpty();
	}

	// Private function to deal with steps that should be applied to any entity
	private InventoryEntry addEntity(Entity entityToAdd, Entity parentEntity, MessageDispatcher messageDispatcher, GameClock gameClock, ItemHoldPosition preferredPosition) {
		entityToAdd.getLocationComponent().setWorldPosition(null, false);
		entityToAdd.getLocationComponent().setContainerEntity(parentEntity);
		entityToAdd.getLocationComponent().setOrientation(EntityAssetOrientation.DOWN);
		InventoryEntry entry = new InventoryEntry(entityToAdd, gameClock, preferredPosition);
		inventoryEntries.put(entityToAdd.getId(), entry);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entityToAdd);
		return entry;
	}

	private InventoryEntry findMatchingEntry(ItemEntityAttributes attributes) {
		for (InventoryEntry entry : inventoryEntries.values()) {
			Entity inventoryItem = entry.entity;
			if (inventoryItem.getType().equals(ITEM)) {
				ItemEntityAttributes inventoryItemAttributes = (ItemEntityAttributes) inventoryItem.getPhysicalEntityComponent().getAttributes();
				if (inventoryItemAttributes.canMerge(attributes)) {
					return entry;
				}
			}
		}
		return null;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!inventoryEntries.isEmpty()) {
			JSONArray inventoryEntriesJson = new JSONArray();
			for (InventoryEntry inventoryEntry : this.inventoryEntries.values()) {
				JSONObject entryJson = new JSONObject(true);
				inventoryEntry.entity.writeTo(savedGameStateHolder);
				entryJson.put("entity", inventoryEntry.entity.getId());
				entryJson.put("lastUpdate", inventoryEntry.lastUpdateGameTime);
				if (inventoryEntry.getPreferredPosition() != null) {
					entryJson.put("position", inventoryEntry.getPreferredPosition().name());
				}
				inventoryEntriesJson.add(entryJson);
			}
			asJson.put("items", inventoryEntriesJson);
		}

		if (itemsUnallocated) {
			asJson.put("itemsUnallocated", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONArray inventoryEntries = asJson.getJSONArray("items");
		if (inventoryEntries != null) {
			for (int cursor = 0; cursor < inventoryEntries.size(); cursor++) {
				JSONObject entryJson = inventoryEntries.getJSONObject(cursor);
				Entity entity = savedGameStateHolder.entities.get(entryJson.getLongValue("entity"));
				InventoryEntry inventoryEntry = new InventoryEntry(entity, entryJson.getDoubleValue("lastUpdate"),
						EnumParser.getEnumValue(entryJson, "position", ItemHoldPosition.class, null));
				this.inventoryEntries.put(entity.getId(), inventoryEntry);
			}
		}

		this.itemsUnallocated = asJson.getBooleanValue("itemsUnallocated");
	}

	public static class InventoryEntry {

		private double lastUpdateGameTime;
		public final Entity entity;
		private ItemHoldPosition preferredPosition; // Usually null, can be set to specify a preferred workspace position

		public InventoryEntry(Entity entity, GameClock gameClock, ItemHoldPosition preferredPosition) {
			this.entity = entity;
			this.lastUpdateGameTime = gameClock.getCurrentGameTime();
			this.preferredPosition = preferredPosition;
		}

		public InventoryEntry(Entity entity, double lastUpdateGameTime, ItemHoldPosition preferredPosition) {
			this.entity = entity;
			this.lastUpdateGameTime = lastUpdateGameTime;
			this.preferredPosition = preferredPosition;
		}

		public ItemHoldPosition getPreferredPosition() {
			return preferredPosition;
		}

		public double getLastUpdateGameTime() {
			return lastUpdateGameTime;
		}

		public void setLastUpdateGameTime(double lastUpdateGameTime) {
			this.lastUpdateGameTime = lastUpdateGameTime;
		}

		public void touch(GameClock gameClock) {
			if (gameClock != null) {
				this.lastUpdateGameTime = gameClock.getCurrentGameTime();
			}
		}
	}
}
