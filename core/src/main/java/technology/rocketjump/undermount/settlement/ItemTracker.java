package technology.rocketjump.undermount.settlement;

import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.materials.model.GameMaterial;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;

/**
 * This class is responsible for keeping track of all items (allocated or not) on the map
 */
@Singleton
public class ItemTracker implements GameContextAware {

	private static final Map<GameMaterial, Map<Long, Entity>> EMPTY_1 = new HashMap<>();
	private static final Map<Long, Entity> EMPTY_2 = new HashMap<>();
	private final Map<ItemType, Map<GameMaterial, Map<Long, Entity>>> itemTypesToMaterialsToEntitiesMap = new HashMap<>();
	private final Map<Long, Entity> edibleItems = new HashMap<>();

	public Set<GameMaterial> getMaterialsByItemType(ItemType itemType) {
		Map<GameMaterial, Map<Long, Entity>> materialMap = itemTypesToMaterialsToEntitiesMap.get(itemType);
		if (materialMap != null) {
			return materialMap.keySet();
		}
		return null;
	}

	public void itemAdded(Entity entity) {
		if (entity.getLocationComponent().isUntracked()) {
			return;
		}
		ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		ItemType itemType = attributes.getItemType();
		GameMaterial material = attributes.getMaterial(itemType.getPrimaryMaterialType());
		if (material == null) {
			Logger.error("Attempting to track an item which does not have a material for its primary type");
			return;
		}
		Map<GameMaterial, Map<Long, Entity>> materialsToEntities = itemTypesToMaterialsToEntitiesMap.computeIfAbsent(itemType, a -> new ConcurrentHashMap<>());
		Map<Long, Entity> entityMap = materialsToEntities.computeIfAbsent(material, a -> new ConcurrentHashMap<>());
		entityMap.put(entity.getId(), entity);

		if (isItemEdible(attributes)) {
			edibleItems.put(entity.getId(), entity);
		}
	}

	public void itemRemoved(Entity entity) {
		ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		itemTypesToMaterialsToEntitiesMap.getOrDefault(attributes.getItemType(), EMPTY_1)
				.getOrDefault(attributes.getPrimaryMaterial(), EMPTY_2)
				.remove(entity.getId());
		cullEmptyMapEntries(attributes.getItemType(), attributes.getPrimaryMaterial());
		// Easiest to just always remove from edibleItems
		edibleItems.remove(entity.getId());
	}

	private static final Map<GameMaterial, Map<Long, Entity>> empty = new HashMap<>();
	private static final Map<Long, Entity> alsoEmpty = new HashMap<>();

	public void primaryMaterialChanged(Entity entity, GameMaterial oldPrimaryMaterial) {
		if (entity.getLocationComponent().isUntracked()) {
			return;
		}
		ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		itemTypesToMaterialsToEntitiesMap.getOrDefault(attributes.getItemType(), empty)
				.getOrDefault(oldPrimaryMaterial, alsoEmpty)
				.remove(entity.getId());
		cullEmptyMapEntries(attributes.getItemType(), oldPrimaryMaterial);

		itemTypesToMaterialsToEntitiesMap.computeIfAbsent(attributes.getItemType(), a -> new ConcurrentHashMap<>())
				.computeIfAbsent(attributes.getPrimaryMaterial(), a -> new ConcurrentHashMap<>())
				.put(entity.getId(), entity);


		if (oldPrimaryMaterial.isEdible()) {
			edibleItems.remove(entity.getId());
		}
		if (attributes.getPrimaryMaterial().isEdible()) {
			edibleItems.put(entity.getId(), entity);
		}
	}

	public Map<ItemType, Map<GameMaterial, Map<Long, Entity>>> getAllByItemType() {
		return itemTypesToMaterialsToEntitiesMap;
	}

	public List<Entity> getUnallocatedEdibleItems() {
		List<Entity> found = new ArrayList<>();
		for (Entity entity : edibleItems.values()) {
			ItemAllocationComponent itemAllocationComponent = entity.getOrCreateComponent(ItemAllocationComponent.class);
			if (itemAllocationComponent.getNumUnallocated() > 0) {
				found.add(entity);
			}
		}
		return found;
	}

	public List<Entity> getItemsByType(ItemType itemType, boolean unallocatedOnly) {
		List<Entity> found = new ArrayList<>();
		Map<GameMaterial, Map<Long, Entity>> materialMap = itemTypesToMaterialsToEntitiesMap.get(itemType);
		if (materialMap != null) {
			for (Map<Long, Entity> entityLongMap : materialMap.values()) {
				for (Entity entity : entityLongMap.values()) {
					if (unallocatedOnly) {
						ItemAllocationComponent itemAllocationComponent = entity.getOrCreateComponent(ItemAllocationComponent.class);
						if (itemAllocationComponent.getNumUnallocated() > 0) {
							found.add(entity);
						}
					} else {
						found.add(entity);
					}
				}
			}
		}
		return found;
	}

	public List<Entity> getItemsByTypeAndMaterial(ItemType itemType, GameMaterial gameMaterial, boolean unallocatedOnly) {
		List<Entity> found = new ArrayList<>();
		Map<GameMaterial, Map<Long, Entity>> materialMap = itemTypesToMaterialsToEntitiesMap.get(itemType);
		if (materialMap != null) {
			if (NULL_MATERIAL.equals(gameMaterial)) {
				return found;
			} else {
				if (materialMap.containsKey(gameMaterial)) {
					for (Entity entity : materialMap.get(gameMaterial).values()) {
						if (unallocatedOnly) {
							ItemAllocationComponent itemAllocationComponent = entity.getOrCreateComponent(ItemAllocationComponent.class);
							if (itemAllocationComponent.getNumUnallocated() > 0) {
								found.add(entity);
							}
						} else {
							found.add(entity);
						}
					}
				}
			}

		}
		return found;
	}

	public GameMaterial getMostUnassignedMaterialForItemType(ItemType resourceItemType) {
		List<Entity> itemsOfType = getItemsByType(resourceItemType, true);
		Map<GameMaterial, Integer> unassignedCount = new HashMap<>();

		for (Entity itemEntity : itemsOfType) {
			ItemAllocationComponent itemAllocationComponent = itemEntity.getOrCreateComponent(ItemAllocationComponent.class);
			ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
			GameMaterial material = attributes.getMaterial(resourceItemType.getPrimaryMaterialType());
			int count = 0;
			if (unassignedCount.containsKey(material)) {
				count = unassignedCount.get(material);
			}
			count += itemAllocationComponent.getNumUnallocated();
			unassignedCount.put(material, count);
		}

		int largestQuantity = 0;
		GameMaterial mostCommonMaterial = null; // TODO should this be non-null for cases where no materials currently exist?
		for (Map.Entry<GameMaterial, Integer> entry : unassignedCount.entrySet()) {
			if (entry.getValue() > largestQuantity) {
				largestQuantity = entry.getValue();
				mostCommonMaterial = entry.getKey();
			}
		}

		return mostCommonMaterial;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		// Does not use GameContext, only needs to clearContextRelatedState state
	}

	@Override
	public void clearContextRelatedState() {
		itemTypesToMaterialsToEntitiesMap.clear();
		edibleItems.clear();
	}

	/**
	 * Currently defining something as edible if one of its constituent materials is edible
	 * This is probably going to cause all sorts of hilarity one day...
	 */
	public static boolean isItemEdible(ItemEntityAttributes attributes) {
		Collection<? extends GameMaterial> allMaterials = attributes.getAllMaterials();
		if (allMaterials.isEmpty()) {
			return false;
		}
		for (GameMaterial material : allMaterials) {
			if (material.isEdible()) {
				return true;
			}
		}
		return false; // all materials inedible
	}

	private void cullEmptyMapEntries(ItemType itemType, GameMaterial material) {
		if (itemTypesToMaterialsToEntitiesMap.containsKey(itemType) && itemTypesToMaterialsToEntitiesMap.get(itemType).containsKey(material)) {
			if (itemTypesToMaterialsToEntitiesMap.get(itemType).get(material).isEmpty()) {
				itemTypesToMaterialsToEntitiesMap.get(itemType).remove(material);

				if (itemTypesToMaterialsToEntitiesMap.get(itemType).isEmpty()) {
					itemTypesToMaterialsToEntitiesMap.remove(itemType);
				}
			}
		}
	}
}
