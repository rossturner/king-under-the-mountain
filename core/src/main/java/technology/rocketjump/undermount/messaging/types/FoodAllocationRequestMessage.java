package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.cooking.model.FoodAllocation;
import technology.rocketjump.undermount.entities.model.Entity;

public class FoodAllocationRequestMessage {

	public final Entity requestingEntity;
	public final FoodAllocationCallback callback;

	public FoodAllocationRequestMessage(Entity requestingEntity, FoodAllocationCallback callback) {
		this.requestingEntity = requestingEntity;
		this.callback = callback;
	}

	public interface FoodAllocationCallback {

		void foodAssigned(FoodAllocation allocation);

	}

}
