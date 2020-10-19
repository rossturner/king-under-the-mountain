package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.rooms.HaulingAllocation;

public class RequestHaulingAllocationMessage {

	public final Entity requestingEntity;
	public final Vector2 requesterPosition;
	public final ItemType requiredItemType;
	public final GameMaterial requiredMaterial;
	public final GameMaterial requiredContainedLiquid;
	public final boolean includeFromFurniture;
	public final ItemAllocationCallback allocationCallback;
	public final Integer maxAmountRequired;

	public RequestHaulingAllocationMessage(Entity requestingEntity, Vector2 requesterPosition, ItemType requiredItemType, GameMaterial requiredMaterial,
	                                       boolean includeFromFurniture, Integer maxAmountRequired, GameMaterial requiredContainedLiquid, ItemAllocationCallback allocationCallback) {
		this.requestingEntity = requestingEntity;
		this.requesterPosition = requesterPosition;
		this.requiredItemType = requiredItemType;
		this.requiredMaterial = requiredMaterial;
		this.includeFromFurniture = includeFromFurniture;
		this.maxAmountRequired = maxAmountRequired;
		this.requiredContainedLiquid = requiredContainedLiquid;
		this.allocationCallback = allocationCallback;
	}

	public interface ItemAllocationCallback {
		void allocationFound(HaulingAllocation haulingAllocation);
	}
}
