package technology.rocketjump.undermount.entities.model.physical.item;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.factories.ItemEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.materials.model.GameMaterial;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class ExampleItemDictionary {

	static GameContext nullContext = new GameContext();
	static {
		nullContext.setRandom(new RandomXS128());
	}
	private final ItemType SHOW_LIQUID_ITEM_TYPE;
	private final Map<ItemType, Map<GameMaterial, Entity>> exampleEntities = new HashMap<>();
	private final Map<ItemType, Entity> ghostEntities = new HashMap<>();
	private final ItemTypeDictionary itemTypeDictionary;
	private final ItemEntityFactory itemEntityFactory;

	@Inject
	public ExampleItemDictionary(ItemTypeDictionary itemTypeDictionary, ItemEntityFactory itemEntityFactory) {
		this.itemTypeDictionary = itemTypeDictionary;
		this.SHOW_LIQUID_ITEM_TYPE = itemTypeDictionary.getByName("Resource-Liquid-Example");
		this.itemEntityFactory = itemEntityFactory;
	}

	public Entity getExampleItemEntity(ItemType itemType, Optional<GameMaterial> material) {
		final ItemType itemTypeOrLiquidItem = itemType == null ? SHOW_LIQUID_ITEM_TYPE : itemType;

		Map<GameMaterial, Entity> materialsToEntitiesMap = exampleEntities.computeIfAbsent(itemTypeOrLiquidItem, a -> new HashMap<>());
		if (material.isPresent()) {
			// Material is specified
			return materialsToEntitiesMap.computeIfAbsent(material.get(), a -> {
				Entity entity = itemEntityFactory.createByItemType(itemTypeOrLiquidItem, nullContext, false);
				ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				attributes.setMaterial(material.get());
				return entity;
			});
		} else {
			// Get any entity in map or create one
			if (materialsToEntitiesMap.isEmpty()) {
				Entity entity = itemEntityFactory.createByItemType(itemTypeOrLiquidItem, nullContext, false);
				ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				materialsToEntitiesMap.put(attributes.getPrimaryMaterial(), entity);
				return entity;
			} else {
				return materialsToEntitiesMap.values().iterator().next();
			}
		}
	}
}
