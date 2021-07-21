package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.graphics.Color;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.item.ItemEntityAssetDictionary;
import technology.rocketjump.undermount.assets.entities.item.ItemEntityAssetsByPlacement;
import technology.rocketjump.undermount.assets.entities.item.ItemEntityAssetsBySize;
import technology.rocketjump.undermount.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.undermount.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.undermount.assets.entities.item.model.ItemSize;
import technology.rocketjump.undermount.assets.entities.item.model.ItemStyle;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.doors.Doorway;
import technology.rocketjump.undermount.entities.EntityAssetUpdater;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.undermount.mapping.tile.wall.Wall;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static technology.rocketjump.undermount.rooms.constructions.ConstructionMessageHandler.otherColorsToCopy;

@Singleton
public class ItemEntityAttributesFactory {

	private final ItemEntityAssetDictionary itemEntityAssetDictionary;
	private final EntityAssetUpdater entityAssetUpdater;
	private final List<ItemSize> sizesToCheckInOrder;
	private final List<ItemStyle> stylesToCheckInOrder;

	@Inject
	public ItemEntityAttributesFactory(ItemEntityAssetDictionary itemEntityAssetDictionary, EntityAssetUpdater entityAssetUpdater) {
		this.itemEntityAssetDictionary = itemEntityAssetDictionary;
		this.entityAssetUpdater = entityAssetUpdater;

		sizesToCheckInOrder = new ArrayList<>();
		sizesToCheckInOrder.addAll(Arrays.asList(ItemSize.values()));
		sizesToCheckInOrder.remove(ItemSize.AVERAGE);
		sizesToCheckInOrder.add(0, ItemSize.AVERAGE);

		stylesToCheckInOrder = new ArrayList<>();
		stylesToCheckInOrder.addAll(Arrays.asList(ItemStyle.values()));
		stylesToCheckInOrder.remove(ItemStyle.DEFAULT);
		stylesToCheckInOrder.add(ItemStyle.DEFAULT);
	}

	public ItemEntityAttributes resourceFromDoorway(Doorway doorway) {
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) doorway.getDoorEntity().getPhysicalEntityComponent().getAttributes();
		QuantifiedItemType requirement = attributes.getFurnitureType().getRequirements().get(attributes.getPrimaryMaterialType()).get(0); // Assuming single resource requirement for doors
		int quantityToCreate = Math.max(1, requirement.getQuantity() / 2);

		ItemEntityAttributes newItemAttributes = new ItemEntityAttributes(SequentialIdGenerator.nextId());
		newItemAttributes.setItemPlacement(ItemPlacement.ON_GROUND);
		newItemAttributes.setItemType(requirement.getItemType());
		newItemAttributes.setQuantity(quantityToCreate);

		for (GameMaterial gameMaterial : attributes.getMaterials().values()) {
			newItemAttributes.setMaterial(gameMaterial);
		}
		ColoringLayer coloringLayer = ColoringLayer.getByMaterialType(attributes.getPrimaryMaterialType());
		if (coloringLayer != null) {
			newItemAttributes.setColor(coloringLayer, attributes.getColor(coloringLayer));
		}

		setItemSizeAndStyle(newItemAttributes);


		return newItemAttributes;
	}

	public ItemEntityAttributes resourceFromWall(Wall wall) {
		WallType wallType = wall.getWallType();
		if (wallType.isConstructed() && wallType.getRequirements() != null && wallType.getRequirements().get(wall.getMaterial().getMaterialType()) != null &&
				wallType.getRequirements().get(wall.getMaterial().getMaterialType()).size() > 0) {
			QuantifiedItemType requirement = wallType.getRequirements().get(wall.getMaterial().getMaterialType()).get(0);// Assuming single requirement for wall
			int quantityToCreate = Math.max(1, requirement.getQuantity() / 2);

			ItemEntityAttributes newItemAttributes = new ItemEntityAttributes(SequentialIdGenerator.nextId());
			newItemAttributes.setItemPlacement(ItemPlacement.ON_GROUND);
			newItemAttributes.setItemType(requirement.getItemType());
			newItemAttributes.setQuantity(quantityToCreate);

			newItemAttributes.setMaterial(wall.getMaterial());
			ColoringLayer coloringLayer = ColoringLayer.getByMaterialType(wall.getMaterial().getMaterialType());
			if (coloringLayer != null) {
				newItemAttributes.setColor(coloringLayer, wall.getMaterial().getColor());
			}

			setItemSizeAndStyle(newItemAttributes);

			return newItemAttributes;
		} else {
			Logger.error("Attempting to create requirement item from wall type " + wallType.toString());
			return null; // This shouldn't be called?
		}
	}

	public List<ItemEntityAttributes> resourcesFromFurniture(Entity furnitureEntity) {
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();
		FurnitureType furnitureType = attributes.getFurnitureType();
		List<QuantifiedItemType> requirements = furnitureType.getRequirements().get(attributes.getPrimaryMaterialType());

		List<ItemEntityAttributes> resourceItemAttributes = new LinkedList<>();
		if (requirements != null && !attributes.isDestroyed()) {
			for (QuantifiedItemType requirement : requirements) {
				int quantity = Math.max(1, requirement.getQuantity() / 2);
				ItemEntityAttributes newItemAttributes = createItemAttributes(requirement.getItemType(), quantity, attributes);
				resourceItemAttributes.add(newItemAttributes);
			}
		}
		return resourceItemAttributes;
	}

	public List<ItemEntityAttributes> resourcesFromFurniture(Entity furnitureEntity, List<ItemType> replacementItems) {
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();

		List<ItemEntityAttributes> itemAttributes = new LinkedList<>();
		if (!attributes.isDestroyed()) {
			for (ItemType itemType : replacementItems) {
				ItemEntityAttributes newItemAttributes = createItemAttributes(itemType, 1, attributes);
				itemAttributes.add(newItemAttributes);
			}
		}
		return itemAttributes;
	}

	public ItemEntityAttributes createItemAttributes(ItemType itemTypeToCreate, int quantityToCreate, GameMaterial... materials) {
		ItemEntityAttributes newItemAttributes = new ItemEntityAttributes(SequentialIdGenerator.nextId());
		newItemAttributes.setItemPlacement(ItemPlacement.ON_GROUND);
		newItemAttributes.setItemType(itemTypeToCreate);
		newItemAttributes.setQuantity(quantityToCreate);
		for (GameMaterial material : materials) {
			newItemAttributes.setMaterial(material);
		}
		setItemSizeAndStyle(newItemAttributes);
		return newItemAttributes;
	}

	private ItemEntityAttributes createItemAttributes(ItemType itemTypeToCreate, int quantityToCreate, FurnitureEntityAttributes furnitureAttributes) {
		ItemEntityAttributes newItemAttributes = new ItemEntityAttributes(SequentialIdGenerator.nextId());
		newItemAttributes.setItemPlacement(ItemPlacement.ON_GROUND);
		newItemAttributes.setItemType(itemTypeToCreate);
		newItemAttributes.setQuantity(quantityToCreate);

		for (GameMaterial gameMaterial : furnitureAttributes.getMaterials().values()) {
			// Always skipping liquids
			if (!gameMaterial.getMaterialType().equals(GameMaterialType.LIQUID)) {
				newItemAttributes.setMaterial(gameMaterial);
			}
		}

		setItemSizeAndStyle(newItemAttributes);

		ColoringLayer coloringLayer = ColoringLayer.getByMaterialType(furnitureAttributes.getPrimaryMaterialType());
		if (coloringLayer != null) {
			newItemAttributes.setColor(coloringLayer, furnitureAttributes.getColor(coloringLayer));
		}

		for (ColoringLayer otherColorLayerToCopy : otherColorsToCopy) {
			Color color = furnitureAttributes.getColor(otherColorLayerToCopy);
			if (color != null) {
				newItemAttributes.setColor(otherColorLayerToCopy, color);
			}
		}


		return newItemAttributes;
	}

	/**
	 * This method should determine if non-default item size and style needs to be used
	 */
	public void setItemSizeAndStyle(ItemEntityAttributes newItemAttributes) {
		ItemEntityAssetsBySize sizeMap = itemEntityAssetDictionary.getQuantityMap()
				.getAssetTypeMapByQuantity(newItemAttributes.getQuantity())
				.getItemTypeMapByAssetType(entityAssetUpdater.ITEM_BASE_LAYER)
				.getSizeMapByItemType(newItemAttributes.getItemType());
		boolean sizeAndStyleMatched = false;

		for (ItemSize itemSize : sizesToCheckInOrder) {
			ItemEntityAssetsByPlacement bySize = sizeMap.getBySize(itemSize);
			if (bySize != null) {
				for (ItemStyle itemStyle : stylesToCheckInOrder) {
					List<ItemEntityAsset> assets = bySize.getByPlacement(newItemAttributes.getItemPlacement()).getByStyle(itemStyle);
					if (!assets.isEmpty()) {
						newItemAttributes.setItemSize(itemSize);
						newItemAttributes.setItemStyle(itemStyle);
						return;
					}
				}
			}
		}
	}
}
