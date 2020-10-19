package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.crafting.model.CraftingRecipe;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.jobs.model.Job;

import java.util.List;

public class ProductionAssignmentRequestMessage {

	public final CraftingType craftingType;
	public final ProductionAssignmentCallback callback;

	public ProductionAssignmentRequestMessage(CraftingType craftingType, ProductionAssignmentCallback callback) {
		this.craftingType = craftingType;
		this.callback = callback;
	}

	public interface ProductionAssignmentCallback {

		void productionAssignmentCallback(List<CraftingRecipe> potentialCraftingRecipes);

	}
}
