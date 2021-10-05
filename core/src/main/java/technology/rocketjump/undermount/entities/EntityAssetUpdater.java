package technology.rocketjump.undermount.entities;

import com.badlogic.gdx.graphics.Color;
import com.google.inject.Inject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.assets.entities.creature.CreatureEntityAssetDictionary;
import technology.rocketjump.undermount.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.undermount.assets.entities.furniture.FurnitureEntityAssetDictionary;
import technology.rocketjump.undermount.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.undermount.assets.entities.item.ItemEntityAssetDictionary;
import technology.rocketjump.undermount.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.undermount.assets.entities.mechanism.MechanismEntityAssetDictionary;
import technology.rocketjump.undermount.assets.entities.mechanism.model.MechanismEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.*;
import technology.rocketjump.undermount.assets.entities.plant.PlantEntityAssetDictionary;
import technology.rocketjump.undermount.assets.entities.plant.model.PlantEntityAsset;
import technology.rocketjump.undermount.entities.components.LiquidContainerComponent;
import technology.rocketjump.undermount.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.AttachedEntity;
import technology.rocketjump.undermount.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.Gender;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectType;
import technology.rocketjump.undermount.entities.model.physical.furniture.DoorwayEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesGrowthStage;
import technology.rocketjump.undermount.entities.tags.Tag;
import technology.rocketjump.undermount.entities.tags.TagProcessor;
import technology.rocketjump.undermount.jobs.ProfessionDictionary;
import technology.rocketjump.undermount.jobs.model.Profession;
import technology.rocketjump.undermount.materials.model.GameMaterial;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class EntityAssetUpdater {

	private final CreatureEntityAssetDictionary creatureEntityAssetDictionary;
	private final ItemEntityAssetDictionary itemEntityAssetDictionary;
	private final FurnitureEntityAssetDictionary furnitureEntityAssetDictionary;
	private final PlantEntityAssetDictionary plantEntityAssetDictionary;
	private final MechanismEntityAssetDictionary mechanismEntityAssetDictionary;
	private final TagProcessor tagProcessor;
	private final Profession defaultProfession;

	public final EntityAssetType branchAssetType;
	public final EntityAssetType leafAssetType;
	public final EntityAssetType fruitAssetType;
	public final EntityAssetType ITEM_BASE_LAYER;
	public final EntityAssetType ITEM_LIQUID_LAYER;
	public final EntityAssetType ITEM_COVER_LAYER;
	public final EntityAssetType FURNITURE_BASE_LAYER;
	public final EntityAssetType CREATURE_BODY;
	public final EntityAssetType CREATURE_HAIR;
	public final EntityAssetType CREATURE_EYEBROWS;
	public final EntityAssetType CREATURE_BEARD;
	public final EntityAssetType CREATURE_LEFT_HAND;
	public final EntityAssetType CREATURE_RIGHT_HAND;
	public final EntityAssetType CREATURE_HEAD;
	public final EntityAssetType FURNITURE_LIQUID_LAYER;
	public final EntityAssetType FURNITURE_COVER_LAYER;
	public final EntityAssetType MECHANISM_BASE_LAYER;

	@Inject
	public EntityAssetUpdater(ItemEntityAssetDictionary itemEntityAssetDictionary, FurnitureEntityAssetDictionary furnitureEntityAssetDictionary,
							  PlantEntityAssetDictionary plantEntityAssetDictionary, MechanismEntityAssetDictionary mechanismEntityAssetDictionary,
							  EntityAssetTypeDictionary entityAssetTypeDictionary,
							  ProfessionDictionary professionDictionary, CreatureEntityAssetDictionary creatureEntityAssetDictionary,
							  TagProcessor tagProcessor) {
		this.itemEntityAssetDictionary = itemEntityAssetDictionary;
		this.plantEntityAssetDictionary = plantEntityAssetDictionary;
		this.furnitureEntityAssetDictionary = furnitureEntityAssetDictionary;
		this.mechanismEntityAssetDictionary = mechanismEntityAssetDictionary;

		this.defaultProfession = professionDictionary.getByName("VILLAGER");
		CREATURE_BODY = entityAssetTypeDictionary.getByName("CREATURE_BODY");

		branchAssetType = entityAssetTypeDictionary.getByName("PLANT_BRANCHES");
		leafAssetType = entityAssetTypeDictionary.getByName("PLANT_LEAVES");
		fruitAssetType = entityAssetTypeDictionary.getByName("PLANT_FRUIT");

		ITEM_BASE_LAYER = entityAssetTypeDictionary.getByName("ITEM_BASE_LAYER");
		ITEM_LIQUID_LAYER = entityAssetTypeDictionary.getByName("ITEM_LIQUID_LAYER");
		ITEM_COVER_LAYER = entityAssetTypeDictionary.getByName("ITEM_COVER_LAYER");

		FURNITURE_BASE_LAYER = entityAssetTypeDictionary.getByName("BASE_LAYER");
		FURNITURE_LIQUID_LAYER = entityAssetTypeDictionary.getByName("FURNITURE_LIQUID_LAYER");
		FURNITURE_COVER_LAYER = entityAssetTypeDictionary.getByName("FURNITURE_COVER_LAYER");

		CREATURE_HEAD = entityAssetTypeDictionary.getByName("CREATURE_HEAD");
		CREATURE_HAIR = entityAssetTypeDictionary.getByName("CREATURE_HAIR");
		CREATURE_EYEBROWS = entityAssetTypeDictionary.getByName("CREATURE_EYEBROWS");
		CREATURE_BEARD = entityAssetTypeDictionary.getByName("CREATURE_BEARD");

		CREATURE_LEFT_HAND = entityAssetTypeDictionary.getByName("CREATURE_LEFT_HAND");
		CREATURE_RIGHT_HAND = entityAssetTypeDictionary.getByName("CREATURE_RIGHT_HAND");

		MECHANISM_BASE_LAYER = entityAssetTypeDictionary.getByName("MECHANISM_BASE_LAYER");

		this.creatureEntityAssetDictionary = creatureEntityAssetDictionary;
		this.tagProcessor = tagProcessor;
	}

	public void updateEntityAssets(Entity entity) {
		switch (entity.getType()) {
			case CREATURE:
				updateCreatureAssets(entity);
				break;
			case ITEM:
				updateItemAssets(entity);
				break;
			case FURNITURE:
				updateFurnitureAssets(entity);
				break;
			case PLANT:
				updatePlantAssets(entity);
				break;
			case MECHANISM:
				updateMechanismAssets(entity);
				break;
			case ONGOING_EFFECT:
				processTags(entity);
				break;
			default:
				throw new RuntimeException("Unhandled entity type " + entity.getType() + " in " + this.getClass().getSimpleName());
		}
	}

	private void updateCreatureAssets(Entity entity) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		ProfessionsComponent professionsComponent = entity.getComponent(ProfessionsComponent.class);
		Profession primaryProfession = defaultProfession;
		if (professionsComponent != null) {
			primaryProfession = professionsComponent.getPrimaryProfession(defaultProfession);
		}


		CreatureEntityAsset baseAsset;
		if (entity.getLocationComponent().getContainerEntity() == null) {
			baseAsset = creatureEntityAssetDictionary.getMatching(CREATURE_BODY, attributes, primaryProfession);
		} else {
			// Only show head and above when inside a container
			baseAsset = creatureEntityAssetDictionary.getMatching(CREATURE_HEAD, attributes, primaryProfession);
		}

		entity.getPhysicalEntityComponent().getTypeMap().clear();
		entity.getPhysicalEntityComponent().setBaseAsset(baseAsset);
		if (baseAsset == null) {
			Logger.error("Base asset is null for " + attributes.toString());
		} else {
			entity.getPhysicalEntityComponent().getTypeMap().put(baseAsset.getType(), baseAsset);
			addOtherCreatureAssetTypes(baseAsset.getType(), entity.getPhysicalEntityComponent(), attributes, primaryProfession);
		}

		// Some gender-specific stuff that should be extracted elsewhere
		if (!attributes.getHasHair()) {
			entity.getPhysicalEntityComponent().getTypeMap().remove(CREATURE_HAIR);
		}
		if (attributes.getGender().equals(Gender.FEMALE)) {
			entity.getPhysicalEntityComponent().getTypeMap().remove(CREATURE_EYEBROWS);
			entity.getPhysicalEntityComponent().getTypeMap().remove(CREATURE_BEARD);
		}

		if (entity.getLocationComponent().getContainerEntity() == null) {
			addOtherCreatureAssetTypes(CREATURE_LEFT_HAND, entity.getPhysicalEntityComponent(), attributes, primaryProfession);
			addOtherCreatureAssetTypes(CREATURE_RIGHT_HAND, entity.getPhysicalEntityComponent(), attributes, primaryProfession);
		}

		// Tag processing
		processTags(entity);
	}

	private void addOtherCreatureAssetTypes(EntityAssetType assetType, PhysicalEntityComponent physicalComponent, CreatureEntityAttributes attributes,
											Profession primaryProfession) {
		CreatureEntityAsset asset = creatureEntityAssetDictionary.getMatching(assetType, attributes, primaryProfession);

		if (asset != null && asset.getType() != null) {
			physicalComponent.getTypeMap().put(asset.getType(), asset);

			Set<EntityAssetType> attachedTypes = new HashSet<>();
			for (SpriteDescriptor spriteDescriptor : asset.getSpriteDescriptors().values()) {
				for (EntityChildAssetDescriptor childAssetDescriptor : spriteDescriptor.getChildAssets()) {
					if (childAssetDescriptor.getSpecificAssetName() == null) {
						// FIXME https://github.com/RocketJumpTechnology/King-under-the-Mountain-Issue-Tracking/issues/3
						// FIXME #110
						// Specific assets should be found at setup time

						attachedTypes.add(childAssetDescriptor.getType());
					}
				}
			}

			for (EntityAssetType attachedType : attachedTypes) {
				addOtherCreatureAssetTypes(attachedType, physicalComponent, attributes, primaryProfession);
			}
		}
	}

	private void updatePlantAssets(Entity entity) {
		PhysicalEntityComponent physicalComponent = entity.getPhysicalEntityComponent();
		PlantEntityAttributes attributes = (PlantEntityAttributes) physicalComponent.getAttributes();

		PlantEntityAsset baseAsset = plantEntityAssetDictionary.getPlantEntityAsset(branchAssetType, attributes);
		physicalComponent.setBaseAsset(baseAsset);
		physicalComponent.getTypeMap().clear();
		physicalComponent.getTypeMap().put(branchAssetType, baseAsset);

		PlantSpeciesGrowthStage growthStage = attributes.getSpecies().getGrowthStages().get(attributes.getGrowthStageCursor());

		Color leafColor = attributes.getColor(ColoringLayer.LEAF_COLOR);
		if (leafColor != null && !Color.CLEAR.equals(leafColor)) {
			PlantEntityAsset leafAsset = plantEntityAssetDictionary.getPlantEntityAsset(leafAssetType, attributes);
			physicalComponent.getTypeMap().put(leafAssetType, leafAsset);
		}

		if (growthStage.isShowFruit()) {
			PlantEntityAsset fruitAsset = plantEntityAssetDictionary.getPlantEntityAsset(fruitAssetType, attributes);
			physicalComponent.getTypeMap().put(fruitAssetType, fruitAsset);
		}

		processTags(entity);
	}

	private void updateItemAssets(Entity entity) {
		ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();

		ItemEntityAsset baseAsset = itemEntityAssetDictionary.getItemEntityAsset(ITEM_BASE_LAYER, attributes);
		entity.getPhysicalEntityComponent().setBaseAsset(baseAsset);
		if (baseAsset != null) {
			addOtherItemAssetTypes(baseAsset.getType(), entity, attributes);
		}

		// Tag processing
		processTags(entity);
	}

	private void updateMechanismAssets(Entity entity) {
		MechanismEntityAttributes attributes = (MechanismEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();

		MechanismEntityAsset baseAsset = mechanismEntityAssetDictionary.getMechanismEntityAsset(MECHANISM_BASE_LAYER, attributes);
		entity.getPhysicalEntityComponent().setBaseAsset(baseAsset);
		if (baseAsset != null) {
			addOtherMechanismAssetTypes(baseAsset.getType(), entity, attributes);
		}

		// Tag processing
		processTags(entity);
	}

	public void processTags(Entity entity) {
		Set<Tag> attachedTags = findAttachedTags(entity);
		entity.setTags(attachedTags);
		tagProcessor.apply(attachedTags, entity);
	}

	private void addOtherItemAssetTypes(EntityAssetType assetType, Entity entity, ItemEntityAttributes attributes) {
		ItemEntityAsset asset = itemEntityAssetDictionary.getItemEntityAsset(assetType, attributes);

		if (asset != null) {
			entity.getPhysicalEntityComponent().getTypeMap().put(asset.getType(), asset);

			Set<EntityAssetType> attachedTypes = new HashSet<>();
			for (SpriteDescriptor spriteDescriptor : asset.getSpriteDescriptors().values()) {
				for (EntityChildAssetDescriptor childAssetDescriptor : spriteDescriptor.getChildAssets()) {
					if (childAssetDescriptor.getSpecificAssetName() == null) {
						// FIXME https://github.com/rossturner/king-under-the-mountain/issues/18
						// Specific assets should be found at setup time

						attachedTypes.add(childAssetDescriptor.getType());
					}
				}
			}

			for (EntityAssetType attachedType : attachedTypes) {
				if (shouldAssetTypeApply(attachedType, entity)) {
					addOtherItemAssetTypes(attachedType, entity, attributes);
				}
			}
		}
	}

	private void addOtherMechanismAssetTypes(EntityAssetType assetType, Entity entity, MechanismEntityAttributes attributes) {
		MechanismEntityAsset asset = mechanismEntityAssetDictionary.getMechanismEntityAsset(assetType, attributes);

		if (asset != null) {
			entity.getPhysicalEntityComponent().getTypeMap().put(asset.getType(), asset);

			Set<EntityAssetType> attachedTypes = new HashSet<>();
			for (SpriteDescriptor spriteDescriptor : asset.getSpriteDescriptors().values()) {
				for (EntityChildAssetDescriptor childAssetDescriptor : spriteDescriptor.getChildAssets()) {
					if (childAssetDescriptor.getSpecificAssetName() == null) {
						// FIXME https://github.com/rossturner/king-under-the-mountain/issues/18
						// Specific assets should be found at setup time

						attachedTypes.add(childAssetDescriptor.getType());
					}
				}
			}

			for (EntityAssetType attachedType : attachedTypes) {
				if (shouldAssetTypeApply(attachedType, entity)) {
					addOtherMechanismAssetTypes(attachedType, entity, attributes);
				}
			}
		}
	}

	private void updateFurnitureAssets(Entity entity) {
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();

		if (attributes instanceof DoorwayEntityAttributes) {
			return;
		}
		FurnitureEntityAsset baseAsset = furnitureEntityAssetDictionary.getFurnitureEntityAsset(FURNITURE_BASE_LAYER, attributes);
		entity.getPhysicalEntityComponent().setBaseAsset(baseAsset);
		if (baseAsset != null) {
			addOtherFurnitureAssetTypes(baseAsset.getType(), entity, attributes);
		}

		Set<Tag> attachedTags = findAttachedTags(entity);
		attachedTags.addAll(attributes.getFurnitureType().getProcessedTags());
		entity.setTags(attachedTags);
		tagProcessor.apply(attachedTags, entity);
	}

	private void addOtherFurnitureAssetTypes(EntityAssetType assetType, Entity entity, FurnitureEntityAttributes attributes) {
		FurnitureEntityAsset asset = furnitureEntityAssetDictionary.getFurnitureEntityAsset(assetType, attributes);

		if (asset != null) {
			entity.getPhysicalEntityComponent().getTypeMap().put(asset.getType(), asset);

			Set<EntityAssetType> attachedTypes = new HashSet<>();
			for (SpriteDescriptor spriteDescriptor : asset.getSpriteDescriptors().values()) {
				for (EntityChildAssetDescriptor childAssetDescriptor : spriteDescriptor.getChildAssets()) {
					if (childAssetDescriptor.getSpecificAssetName() == null) {
						// FIXME https://github.com/rossturner/king-under-the-mountain/issues/18
						// Specific assets should be found at setup time

						attachedTypes.add(childAssetDescriptor.getType());
					}
				}
			}

			for (EntityAssetType attachedType : attachedTypes) {
				if (shouldAssetTypeApply(attachedType, entity)) {
					addOtherFurnitureAssetTypes(attachedType, entity, attributes);
				}
			}
		}
	}

	private Set<Tag> findAttachedTags(Entity entity) {
		Set<Tag> attachedTags = new LinkedHashSet<>();
		for (EntityAsset entityAsset : entity.getPhysicalEntityComponent().getTypeMap().values()) {
			attachedTags.addAll(tagProcessor.processRawTags(entityAsset.getTags()));
		}

		for (AttachedEntity attachedEntity : entity.getAttachedEntities()) {
			attachedTags.addAll(findAttachedTags(attachedEntity.entity));
		}

		if (entity.getPhysicalEntityComponent().getAttributes() instanceof FurnitureEntityAttributes) {
			FurnitureType furnitureType = ((FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getFurnitureType();
			attachedTags.addAll(furnitureType.getProcessedTags());
		} else if (entity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes) {
			ItemType itemType = ((ItemEntityAttributes)entity.getPhysicalEntityComponent().getAttributes()).getItemType();
			attachedTags.addAll(itemType.getProcessedTags());
		} else if (entity.getPhysicalEntityComponent().getAttributes() instanceof PlantEntityAttributes) {
			PlantSpecies plantSpecies = ((PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getSpecies();
			attachedTags.addAll(plantSpecies.getProcessedTags());
		} else if (entity.getPhysicalEntityComponent().getAttributes() instanceof OngoingEffectAttributes) {
			OngoingEffectType type = ((OngoingEffectAttributes) entity.getPhysicalEntityComponent().getAttributes()).getType();
			attachedTags.addAll(type.getProcessedTags());
		}

		return attachedTags;
	}

	private boolean shouldAssetTypeApply(EntityAssetType attachedType, Entity entity) {
		if (attachedType.equals(ITEM_COVER_LAYER) || attachedType.equals(FURNITURE_COVER_LAYER)) {
			LiquidContainerComponent liquidContainerComponent = entity.getComponent(LiquidContainerComponent.class);
			return liquidContainerComponent != null && liquidContainerComponent.getTargetLiquidMaterial() != null &&
					!shouldShowLiquidLayer(liquidContainerComponent.getTargetLiquidMaterial()) && liquidContainerComponent.getLiquidQuantity() > 0.1;
		} else if (attachedType.equals(ITEM_LIQUID_LAYER) || attachedType.equals(FURNITURE_LIQUID_LAYER)) {
			LiquidContainerComponent liquidContainerComponent = entity.getComponent(LiquidContainerComponent.class);
			return liquidContainerComponent != null && liquidContainerComponent.getTargetLiquidMaterial() != null &&
					shouldShowLiquidLayer(liquidContainerComponent.getTargetLiquidMaterial()) && liquidContainerComponent.getLiquidQuantity() > 0.1;
		} else {
			return true;
		}
	}

	private boolean shouldShowLiquidLayer(GameMaterial material) {
		return !material.isAlcoholic() && (material.isEdible() || material.isQuenchesThirst());
	}
}
