package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.entities.EntityAssetUpdater;
import technology.rocketjump.undermount.entities.behaviour.items.ItemBehaviour;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.LocationComponent;
import technology.rocketjump.undermount.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.messaging.MessageType;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ItemEntityFactory {

	private static final float MAX_POSITION_OFFSET = 0.05f;
	private static final float ITEM_RADIUS = 0.4f;

	private final ItemEntityAttributesFactory itemEntityAttributesFactory;
	private final MessageDispatcher messageDispatcher;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final EntityAssetUpdater entityAssetUpdater;

	@Inject
	public ItemEntityFactory(ItemEntityAttributesFactory itemEntityAttributesFactory, MessageDispatcher messageDispatcher, GameMaterialDictionary gameMaterialDictionary, EntityAssetUpdater entityAssetUpdater) {
		this.itemEntityAttributesFactory = itemEntityAttributesFactory;
		this.messageDispatcher = messageDispatcher;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.entityAssetUpdater = entityAssetUpdater;
	}
	
	public Entity createByItemType(ItemType itemType, GameContext gameContext, boolean addToGameContext) {
		ItemEntityAttributes attributes = new ItemEntityAttributes(gameContext.getRandom().nextLong());
		attributes.setItemType(itemType);
		itemEntityAttributesFactory.setItemSizeAndStyle(attributes);

		for (GameMaterialType requiredMaterialType : itemType.getMaterialTypes()) {
			List<GameMaterial> materialsToPickFrom = gameMaterialDictionary.getByType(requiredMaterialType).stream()
					.filter(GameMaterial::isUseInRandomGeneration).collect(Collectors.toList());
			if (materialsToPickFrom.isEmpty()) {
				// No use-in-random-generation materials
				Logger.error("Needed a material of type " + requiredMaterialType + " to use in random generation");
			}
			GameMaterial material = materialsToPickFrom.get(gameContext.getRandom().nextInt(materialsToPickFrom.size()));
			attributes.setMaterial(material);
		}
		attributes.setQuantity(1);

		return this.create(attributes, null, addToGameContext, gameContext);
	}


	public Entity create(ItemEntityAttributes attributes, GridPoint2 tilePosition, boolean addToGameContext, GameContext gameContext) {
		PhysicalEntityComponent physicalComponent = new PhysicalEntityComponent();
		itemEntityAttributesFactory.setItemSizeAndStyle(attributes);
		physicalComponent.setAttributes(attributes);
		BehaviourComponent behaviorComponent = new ItemBehaviour();
		LocationComponent locationComponent = createLocationComponent(tilePosition, attributes.getSeed());


		Entity entity = new Entity(EntityType.ITEM, physicalComponent, behaviorComponent, locationComponent, messageDispatcher, gameContext);
		entity.addComponent(new ItemAllocationComponent());
		entity.init(messageDispatcher, gameContext);

		entityAssetUpdater.updateEntityAssets(entity);
		if (addToGameContext) {
			messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, entity);
		}
		return entity;
	}

	public static LocationComponent createLocationComponent(GridPoint2 tilePosition, long seed) {
		Random random = new RandomXS128(seed);

		LocationComponent locationComponent = new LocationComponent();
		if (tilePosition != null) {
			Vector2 worldPosition = new Vector2(tilePosition.x + 0.5f, tilePosition.y + 0.5f);
			// Randomise world position offset
			worldPosition.x += (random.nextFloat() * MAX_POSITION_OFFSET * 2) - MAX_POSITION_OFFSET;
			worldPosition.y += (random.nextFloat() * MAX_POSITION_OFFSET * 2) - MAX_POSITION_OFFSET;

			locationComponent.setWorldPosition(worldPosition, false);
		}
		locationComponent.setFacing(EntityAssetOrientation.DOWN.toVector2().cpy());
		locationComponent.setRadius(ITEM_RADIUS);
		return locationComponent;
	}

}
