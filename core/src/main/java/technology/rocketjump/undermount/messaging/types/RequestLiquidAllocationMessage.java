package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.components.LiquidAllocation;
import technology.rocketjump.undermount.entities.model.Entity;

import java.util.Optional;

public class RequestLiquidAllocationMessage {

	public final Entity requestingEntity;
	public final float amountRequired;
	public final boolean mustQuenchThirst;
	public final boolean isAlcoholic;
	public final LiquidAllocationCallback callback;

	public RequestLiquidAllocationMessage(Entity requestingEntity, float amountRequired, boolean isAlcoholic, boolean mustQuenchThirst, LiquidAllocationCallback callback) {
		this.requestingEntity = requestingEntity;
		this.amountRequired = amountRequired;
		this.mustQuenchThirst = mustQuenchThirst;
		this.isAlcoholic = isAlcoholic;
		this.callback = callback;
	}

	public interface LiquidAllocationCallback {

		void allocationFound(Optional<LiquidAllocation> liquidAllocation);

	}
}
