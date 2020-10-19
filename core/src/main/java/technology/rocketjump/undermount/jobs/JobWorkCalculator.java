package technology.rocketjump.undermount.jobs;

import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.undermount.rooms.constructions.Construction;

public class JobWorkCalculator {

	public static final float MAX_WORK_TIME = 20f;

	public float getTotalWorkToDo(Construction furnitureConstruction) {
		int totalRequiredMaterials = 0;
		for (QuantifiedItemTypeWithMaterial requirement : furnitureConstruction.getRequirements()) {
			totalRequiredMaterials += requirement.getQuantity();
		}
		return Math.min(totalRequiredMaterials, MAX_WORK_TIME);
	}

}
