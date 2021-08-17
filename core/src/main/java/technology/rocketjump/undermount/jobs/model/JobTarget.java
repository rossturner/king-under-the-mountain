package technology.rocketjump.undermount.jobs.model;

import com.badlogic.gdx.graphics.Color;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.cooking.model.CookingRecipe;
import technology.rocketjump.undermount.crafting.model.CraftingRecipe;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoof;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.rooms.Bridge;
import technology.rocketjump.undermount.rooms.constructions.Construction;

import static technology.rocketjump.undermount.entities.model.EntityType.ITEM;
import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;

public class JobTarget {

	public final JobTargetType type;
	private CookingRecipe cookingRecipe;
	private CraftingRecipe craftingRecipe;
	private Entity entity;
	private Construction construction;
	private Bridge bridge;
	private MapTile tile;
	private TileRoof roof;

	public JobTarget(CookingRecipe cookingRecipe) {
		this.type = JobTargetType.COOKING_RECIPE;
		this.cookingRecipe = cookingRecipe;
	}

	public JobTarget(Entity entity) {
		this.type = JobTargetType.ENTITY;
		this.entity = entity;
	}

	public JobTarget(Construction construction) {
		this.type = JobTargetType.CONSTRUCTION;
		this.construction = construction;
	}

	public JobTarget(Bridge bridge) {
		this.type = JobTargetType.BRIDGE;
		this.bridge = bridge;
	}

	public JobTarget(MapTile tile) {
		this.type = JobTargetType.TILE;
		this.tile = tile;
	}

	public JobTarget(MapTile tile, TileRoof roof) {
		this.type = JobTargetType.ROOF;
		this.roof = roof;
		this.tile = tile;
	}

	public JobTarget(CraftingRecipe craftingRecipe, Entity craftingStation) {
		this.type = JobTargetType.CRAFTING_RECIPE;
		this.craftingRecipe = craftingRecipe;
		this.entity = craftingStation;
	}

	public CookingRecipe getCookingRecipe() {
		return cookingRecipe;
	}

	public Entity getEntity() {
		return entity;
	}

	public Construction getConstruction() {
		return construction;
	}

	public Bridge getBridge() {
		return bridge;
	}

	public MapTile getTile() {
		return tile;
	}

	public TileRoof getRoof() {
		return roof;
	}

	public GameMaterial getTargetMaterial() {
		switch (type) {
			case CRAFTING_RECIPE: {
				InventoryComponent craftingStationInventory = entity.getComponent(InventoryComponent.class);
				if (craftingRecipe.getMaterialTypesToCopyOver() != null && !craftingRecipe.getMaterialTypesToCopyOver().isEmpty()) {
					for (QuantifiedItemTypeWithMaterial requirement : craftingRecipe.getInput()) {
						if (requirement.getItemType() != null && requirement.getItemType().getPrimaryMaterialType().equals(craftingRecipe.getMaterialTypesToCopyOver().get(0))) {
							InventoryComponent.InventoryEntry inventoryEntry = craftingStationInventory.findByItemType(requirement.getItemType(), null);
							if (inventoryEntry != null && inventoryEntry.entity.getType().equals(ITEM)) {
								return ((ItemEntityAttributes)inventoryEntry.entity.getPhysicalEntityComponent().getAttributes()).getPrimaryMaterial();
							}
						}
					}
				} else {
					for (QuantifiedItemTypeWithMaterial requirement : craftingRecipe.getInput()) {
						if (requirement.getItemType() != null) {
							InventoryComponent.InventoryEntry inventoryEntry = craftingStationInventory.findByItemType(requirement.getItemType(), null);
							if (inventoryEntry != null && inventoryEntry.entity.getType().equals(ITEM)) {
								return ((ItemEntityAttributes)inventoryEntry.entity.getPhysicalEntityComponent().getAttributes()).getPrimaryMaterial();
							}
						}
					}
				}
				break;
			}
			case TILE: {
				if (tile.hasWall()) {
					if (tile.getWall().hasOre()) {
						return tile.getWall().getOreMaterial();
					} else {
						return tile.getWall().getMaterial();
					}
				} else {
					return tile.getFloor().getMaterial();
				}
			}
			case ROOF: {
				return roof.getRoofMaterial();
			}
			case CONSTRUCTION: {
				GameMaterial constructionMaterial = construction.getPrimaryMaterial();
				if (constructionMaterial.equals(NULL_MATERIAL)) {
					return null;
				} else {
					return constructionMaterial;
				}
			}
			case ENTITY: {
				switch (entity.getType()) {
					case FURNITURE:
						return ((FurnitureEntityAttributes)entity.getPhysicalEntityComponent().getAttributes()).getPrimaryMaterial();
					case PLANT:
						return ((PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getSpecies().getMaterial();
					case ITEM:
						return ((ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getPrimaryMaterial();
					default:
						return null;
				}
			}
			default:
		}
		Logger.warn("Not yet implemented: JobTarget.getTargetMaterial() for " + type);
		return null;
	}

	public Color getTargetColor() {
		switch (type) {
			case ENTITY: {
				switch (entity.getType()) {
					case PLANT: {
						PlantEntityAttributes attributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
						switch (attributes.getSpecies().getPlantType()) {
							case MUSHROOM:
							case MUSHROOM_TREE:
								return null;
							case CROP:
								if (attributes.getColor(ColoringLayer.LEAF_COLOR) != null) {
									return attributes.getColor(ColoringLayer.LEAF_COLOR);
								} else {
									return attributes.getColor(ColoringLayer.BRANCHES_COLOR);
								}
							case TREE:
							case SHRUB:
								return attributes.getColor(ColoringLayer.LEAF_COLOR);
						}
					}
					default: {
						return null;
					}
				}
			}
		}

		GameMaterial targetMaterial = getTargetMaterial();
		if (targetMaterial != null) {
			return targetMaterial.getColor();
		}
		Logger.warn("Not yet implemented: JobTarget.getTargetColor() for " + type);
		return null;
	}

	public enum JobTargetType {

		COOKING_RECIPE,
		CRAFTING_RECIPE,
		ENTITY,
		CONSTRUCTION,
		BRIDGE,
		TILE,
		ROOF

	}

	public static JobTarget NULL_TARGET = new NullJobTarget();

	private static class NullJobTarget extends JobTarget {

		public NullJobTarget() {
			super(Entity.NULL_ENTITY);
		}

		@Override
		public GameMaterial getTargetMaterial() {
			return null;
		}

		@Override
		public Color getTargetColor() {
			return null;
		}
	}
}
