package technology.rocketjump.undermount.jobs.model;

import com.badlogic.gdx.graphics.Color;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.cooking.model.CookingRecipe;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.rooms.Bridge;
import technology.rocketjump.undermount.rooms.constructions.Construction;

import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;

public class JobTarget {

	public final JobTargetType type;
	private CookingRecipe cookingRecipe;
	private Entity entity;
	private Construction construction;
	private Bridge bridge;
	private MapTile tile;

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

	public GameMaterial getTargetMaterial() {
		switch (type) {
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
					case PLANT: {
						return ((PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getSpecies().getMaterial();
					}
				}
			}
			default:
				Logger.warn("Not yet implemented: JobTarget.getTargetMaterial() for " + type);
				return null;
		}
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
		ENTITY,
		CONSTRUCTION,
		BRIDGE,
		TILE

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
