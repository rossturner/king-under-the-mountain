package technology.rocketjump.undermount.jobs;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.rooms.Bridge;

@Singleton
public class JobFactory {

	private final CraftingTypeDictionary craftingTypeDictionary;
	private final JobTypeDictionary jobTypeDictionary;

	@Inject
	public JobFactory(CraftingTypeDictionary craftingTypeDictionary, JobTypeDictionary jobTypeDictionary) {
		this.craftingTypeDictionary = craftingTypeDictionary;
		this.jobTypeDictionary = jobTypeDictionary;
	}

	public Job constructionJob(CraftingType craftingType) {
		Job job = new Job(jobTypeDictionary.getByName("CONSTRUCT"));
		job.setRequiredProfession(craftingType.getProfessionRequired());
		job.setRequiredItemType(craftingType.getDefaultItemType());
		return job;
	}

	public Job deconstructionJob(MapTile targetTile) {
		Job job = new Job(jobTypeDictionary.getByName("DECONSTRUCT"));
		job.setJobLocation(targetTile.getTilePosition());
		if (targetTile.hasWall()) {
			if (targetTile.getWall().getWallType() == null || targetTile.getWall().getWallType().getCraftingType() == null) {
				Logger.error("Attempting to deconstruct wall with null craftingType");
				return null;
			}
			// deconstructing a wall
			job.setRequiredProfession(targetTile.getWall().getWallType().getCraftingType().getProfessionRequired());
			job.setRequiredItemType(targetTile.getWall().getWallType().getCraftingType().getDefaultItemType());
		} else if (targetTile.hasDoorway()) {
			// deconstructing door
			FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) targetTile.getDoorway().getDoorEntity().getPhysicalEntityComponent().getAttributes();
			FurnitureType furnitureType = attributes.getFurnitureType();
			CraftingType craftingType = craftingTypeDictionary.getByFurnitureConstruction(attributes.getPrimaryMaterialType());
			job.setRequiredProfession(craftingType.getProfessionRequired());
			job.setRequiredItemType(craftingType.getDefaultItemType());
			// Do not set entity ID for doorway, only for furniture entities
		} else if (targetTile.getFloor().hasBridge()) {
			// Deconstructing bridge
			Bridge bridge = targetTile.getFloor().getBridge();
			job.setRequiredProfession(bridge.getBridgeType().getCraftingType().getProfessionRequired());
			job.setRequiredItemType(bridge.getBridgeType().getCraftingType().getDefaultItemType());
		} else {
			Entity targetFurniture = null;
			for (Entity entity : targetTile.getEntities()) {
				if (entity.getType().equals(EntityType.FURNITURE)) {
					targetFurniture = entity;
					break;
				}
			}
			if (targetFurniture == null) {
				// Could not find anything to deconstruct
				return null;
			}
			FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) targetFurniture.getPhysicalEntityComponent().getAttributes();
			CraftingType craftingType = craftingTypeDictionary.getByFurnitureConstruction(attributes.getPrimaryMaterialType());
			if (craftingType == null) {
				Logger.error("Attempting deconstruction job for furniture which does not have a crafting type - needs investigating.");
				return null;
			}
			job.setRequiredProfession(craftingType.getProfessionRequired());
			// Not requiring an item so player doesn't get stuck needing an item to deconstruct furniture containing it
			job.setTargetId(targetFurniture.getId());
		}
		return job;
	}
}
