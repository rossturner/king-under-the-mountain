package technology.rocketjump.undermount.jobs.model;

import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.cooking.model.CookingRecipe;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.rooms.Bridge;
import technology.rocketjump.undermount.rooms.constructions.Construction;

public class JobTarget {

	public final JobTargetType type;
	private CookingRecipe cookingRecipe;
	private Entity entity;
	private Construction construction;
	private Bridge bridge;
	private MapTile tile;

	public JobTarget(JobTargetType type, CookingRecipe cookingRecipe) {
		this.type = type;
		this.cookingRecipe = cookingRecipe;
	}

	public JobTarget(JobTargetType type, Entity entity) {
		this.type = type;
		this.entity = entity;
	}

	public JobTarget(JobTargetType type, Construction construction) {
		this.type = type;
		this.construction = construction;
	}

	public JobTarget(JobTargetType type, Bridge bridge) {
		this.type = type;
		this.bridge = bridge;
	}

	public JobTarget(JobTargetType type, MapTile tile) {
		this.type = type;
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
			default:
				Logger.warn("Not yet implemented: JobTarget.getTargetMaterial() for " + type);
				return null;
		}
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
			super(JobTargetType.ENTITY, Entity.NULL_ENTITY);
		}

		@Override
		public GameMaterial getTargetMaterial() {
			return null;
		}
	}
}
