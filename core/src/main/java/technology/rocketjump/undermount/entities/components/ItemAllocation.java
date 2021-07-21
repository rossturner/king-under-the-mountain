package technology.rocketjump.undermount.entities.components;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.Objects;

public class ItemAllocation implements Persistable {

	private Long itemAllocationId;

	private Long targetItemEntityId;
	private int allocationAmount;

	private Long owningEntityId;
	private Purpose purpose;
	private AllocationState state = AllocationState.ACTIVE;
	private Long relatedHaulingAllocationId;

	public enum Purpose {

		DUE_TO_BE_HAULED,
		HAULING, // Should only be set and cleared by HaulingComponent
		EQUIPPED, // Should only be set and cleared by EquippedItemComponent
		/*
			Items held in inventory should always be HELD_IN_INVENTORY if they are assigned, or else not have an ItemAssignment
			This is to avoid confusion with a FOOD_ALLOCATION or other purpose
		 */
		HELD_IN_INVENTORY,
		PLACED_FOR_CONSTRUCTION, // TODO look closely at this one, is this where a bug is coming from?
		FOOD_ALLOCATION,
		CONTENTS_TO_BE_DUMPED,
		ON_FIRE

	}


	public enum AllocationState {

		ACTIVE,
		CANCELLED;

	}
	public ItemAllocation() {

	}

	public ItemAllocation(Entity targetItemEntity, int quantity, Entity owningEntity, Purpose purpose) {
		itemAllocationId = SequentialIdGenerator.nextId();

		this.targetItemEntityId = targetItemEntity.getId();
		this.allocationAmount = quantity;

		this.owningEntityId = owningEntity.getId();
		this.purpose = purpose;
	}

	public ItemAllocation clone() {
		ItemAllocation cloned = new ItemAllocation();
		cloned.itemAllocationId = this.itemAllocationId;

		cloned.targetItemEntityId = this.targetItemEntityId;
		cloned.allocationAmount = this.allocationAmount;

		cloned.owningEntityId = this.owningEntityId;
		cloned.purpose = this.purpose;
		cloned.state = this.state;

		return cloned;
	}

	public int getAllocationAmount() {
		return allocationAmount;
	}

	public Long getItemAllocationId() {
		return itemAllocationId;
	}

	public Long getTargetItemEntityId() {
		return targetItemEntityId;
	}

	public Long getOwningEntityId() {
		return owningEntityId;
	}

	public Purpose getPurpose() {
		return purpose;
	}

	public AllocationState getState() {
		return state;
	}

	public void markAsCancelled() {
		this.state = AllocationState.CANCELLED;
	}

	public boolean isCancelled() {
		return state.equals(AllocationState.CANCELLED);
	}

	public void setAllocationAmount(int allocationAmount) {
		this.allocationAmount = allocationAmount;
	}

	public void setPurpose(Purpose newPurpose) {
		this.purpose = newPurpose;
	}

	public Long getRelatedHaulingAllocationId() {
		return relatedHaulingAllocationId;
	}

	public void setRelatedHaulingAllocationId(Long relatedHaulingAllocationId) {
		this.relatedHaulingAllocationId = relatedHaulingAllocationId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ItemAllocation that = (ItemAllocation) o;
		return Objects.equals(itemAllocationId, that.itemAllocationId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(itemAllocationId);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append(allocationAmount).append("x").append(purpose.name());
		if (!state.equals(AllocationState.ACTIVE)) {
			builder.append(" state:").append(state);
		}
		builder.append("]");
		return builder.toString();
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.itemAllocations.containsKey(this.itemAllocationId)) {
			return;
		}

		JSONObject asJson = new JSONObject(true);
		asJson.put("id", itemAllocationId);

		asJson.put("target", targetItemEntityId);
		asJson.put("amount", allocationAmount);
		asJson.put("owner", owningEntityId);
		if (!purpose.equals(Purpose.HELD_IN_INVENTORY)) {
			asJson.put("purpose", purpose.name());
		}
		if (!state.equals(AllocationState.ACTIVE)) {
			asJson.put("state", state.name());
		}
		if (relatedHaulingAllocationId != null) {
			asJson.put("relatedHaulingAllocationId", relatedHaulingAllocationId);
		}

		savedGameStateHolder.itemAllocations.put(this.itemAllocationId, this);
		savedGameStateHolder.itemAllocationsJson.add(asJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.itemAllocationId = asJson.getLongValue("id");
		this.targetItemEntityId = asJson.getLongValue("target");
		this.allocationAmount = asJson.getIntValue("amount");
		this.owningEntityId = asJson.getLongValue("owner");
		this.purpose = EnumParser.getEnumValue(asJson, "purpose", Purpose.class, Purpose.HELD_IN_INVENTORY);
		this.state = EnumParser.getEnumValue(asJson, "state", AllocationState.class, AllocationState.ACTIVE);
		this.relatedHaulingAllocationId = asJson.getLong("relatedHaulingAllocationId");

		savedGameStateHolder.itemAllocations.put(this.itemAllocationId, this);
	}

}
