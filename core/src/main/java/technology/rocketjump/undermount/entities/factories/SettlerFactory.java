package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent;
import technology.rocketjump.undermount.entities.components.humanoid.StatusComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.CraftingTypeDictionary;
import technology.rocketjump.undermount.jobs.JobTypeDictionary;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.jobs.model.Profession;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.ItemPrimaryMaterialChangedMessage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent.HappinessModifier.NEW_SETTLEMENT_OPTIMISM;

@Singleton
public class SettlerFactory {

	private final HumanoidEntityAttributesFactory attributesFactory;
	private final HumanoidEntityFactory entityFactory;
	private final ItemTypeDictionary itemTypeDictionary; // Needed to ensure order of JobType initialisation
	private final PlantSpeciesDictionary plantSpeciesDictionary;
	private final ItemEntityFactory itemEntityFactory;
	private final MessageDispatcher messageDispatcher;
	private final GameMaterialDictionary materialDictionary;

	private Map<Profession, Set<ItemType>> professionItemMapping = new HashMap<>();

	@Inject
	public SettlerFactory(HumanoidEntityAttributesFactory attributesFactory, HumanoidEntityFactory entityFactory,
						  ItemTypeDictionary itemTypeDictionary, CraftingTypeDictionary craftingTypeDictionary,
						  PlantSpeciesDictionary plantSpeciesDictionary, ItemEntityFactory itemEntityFactory, MessageDispatcher messageDispatcher,
						  JobTypeDictionary jobTypeDictionary, GameMaterialDictionary materialDictionary) {
		this.attributesFactory = attributesFactory;
		this.entityFactory = entityFactory;
		this.itemTypeDictionary = itemTypeDictionary;
		this.plantSpeciesDictionary = plantSpeciesDictionary;
		this.itemEntityFactory = itemEntityFactory;
		this.messageDispatcher = messageDispatcher;
		this.materialDictionary = materialDictionary;

		for (CraftingType craftingType : craftingTypeDictionary.getAll()) {
			if (craftingType.getDefaultItemType() != null && craftingType.getProfessionRequired() != null) {
				Set<ItemType> itemsForProfession = professionItemMapping.computeIfAbsent(craftingType.getProfessionRequired(), (e) -> new HashSet<>());
				itemsForProfession.add(craftingType.getDefaultItemType());
			}
		}

		for (JobType jobType : jobTypeDictionary.getAll()) {
			if (jobType.getRequiredProfession() != null && jobType.getRequiredItemType() != null) {
				Set<ItemType> itemsForProfession = professionItemMapping.computeIfAbsent(jobType.getRequiredProfession(), (e) -> new HashSet<>());
				itemsForProfession.add(jobType.getRequiredItemType());
			}
		}

	}

	public Entity create(Vector2 worldPosition, Vector2 facing, Profession primaryProfession, Profession secondaryProfession, GameContext gameContext) {
		HumanoidEntityAttributes attributes = attributesFactory.create();

		Entity entity = entityFactory.create(attributes, worldPosition, facing, primaryProfession, secondaryProfession, gameContext);

		entity.getOrCreateComponent(StatusComponent.class).init(entity, messageDispatcher, gameContext);

		HappinessComponent happinessComponent = entity.getOrCreateComponent(HappinessComponent.class);
		happinessComponent.add(NEW_SETTLEMENT_OPTIMISM);

		addRations(entity, messageDispatcher, gameContext);

		messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, entity);
		return entity;
	}

	private void addRations(Entity settler, MessageDispatcher messageDispatcher, GameContext gameContext) {
		InventoryComponent inventoryComponent = settler.getOrCreateComponent(InventoryComponent.class);

		ItemType rationItemType = itemTypeDictionary.getByName("Product-Ration");
		Entity rationItem = itemEntityFactory.createByItemType(rationItemType, gameContext);
		ItemEntityAttributes attributes = (ItemEntityAttributes) rationItem.getPhysicalEntityComponent().getAttributes();
		attributes.setQuantity(50);
		GameMaterial oldPrimaryMaterial = attributes.getPrimaryMaterial();
		attributes.setMaterial(materialDictionary.getByName("Rockbread"));
		if (!oldPrimaryMaterial.equals(attributes.getPrimaryMaterial())) {
			messageDispatcher.dispatchMessage(MessageType.ITEM_PRIMARY_MATERIAL_CHANGED, new ItemPrimaryMaterialChangedMessage(rationItem, oldPrimaryMaterial));
		}

		inventoryComponent.add(rationItem, settler, messageDispatcher, gameContext.getGameClock());
	}
}
