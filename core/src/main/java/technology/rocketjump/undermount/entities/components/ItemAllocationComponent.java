package technology.rocketjump.undermount.entities.components;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;

public class ItemAllocationComponent implements ParentDependentEntityComponent, Destructible {

	private Entity parentEntity;

	private List<ItemAllocation> allocations = new ArrayList<>();
	private GameContext gameContext;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
		this.gameContext = gameContext;
		if (!parentEntity.getType().equals(EntityType.ITEM)) {
//			throw new RuntimeException("ItemAllocationComponent can only be applied to ITEM entities");
		}
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		ItemAllocationComponent other = new ItemAllocationComponent();
		for (ItemAllocation allocation : allocations) {
			other.allocations.add(allocation.clone());
		}
		return other;
	}

	public ItemAllocation createAllocation(int numToAllocate, Entity requestingEntity, ItemAllocation.Purpose purpose) {
		ItemEntityAttributes attributes = (ItemEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		int currentAllocated = this.getNumAllocated();
		if (currentAllocated + numToAllocate > attributes.getQuantity()) {
			throw new RuntimeException("Attempting to requestAllocation too many items");
		} else {
			ItemAllocation itemAllocation = new ItemAllocation(this.parentEntity, numToAllocate, requestingEntity, purpose);
			allocations.add(itemAllocation);
			return itemAllocation;
		}
	}

	public ItemAllocation cancel(ItemAllocation itemAllocaton) {
		if (allocations.contains(itemAllocaton) && !itemAllocaton.isCancelled()) {
			allocations.remove(itemAllocaton);
			itemAllocaton.markAsCancelled();
			return itemAllocaton;
		} else {
			Logger.error("Incorrect cancellation of " + this.getClass().getSimpleName());
			return null;
		}
	}

	public void cancelAll(ItemAllocation.Purpose purposeToCancel) {
		for (ItemAllocation allocation : new ArrayList<>(allocations)) {
			if (allocation.getPurpose().equals(purposeToCancel)) {
				cancel(allocation);
			}
		}
	}

	public void cancelAll() {
		for (ItemAllocation allocation : new ArrayList<>(allocations)) {
			cancel(allocation);
		}
	}

	public void cancelAllocationAndDecrementQuantity(ItemAllocation allocation) {
		allocation = this.cancel(allocation);

		if (allocation != null) {
			Entity itemEntity = gameContext.getEntities().get(allocation.getTargetItemEntityId());
			if (itemEntity != null) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
				attributes.setQuantity(attributes.getQuantity() - allocation.getAllocationAmount());
			} else {
				Logger.error("Could not find item with ID " + allocation.getTargetItemEntityId());
			}
		}
	}

	public ItemAllocation swapAllocationPurpose(ItemAllocation.Purpose existingPurpose, ItemAllocation.Purpose newPurpose, int quantity) {
		for (ItemAllocation existingAllocation : new ArrayList<>(this.allocations)) {
			if (existingAllocation.getPurpose().equals(existingPurpose) && existingAllocation.getAllocationAmount() >= quantity) {

				ItemAllocation newAllocation = existingAllocation.clone();
				newAllocation.setPurpose(newPurpose);
				newAllocation.setAllocationAmount(quantity);

				existingAllocation.setAllocationAmount(existingAllocation.getAllocationAmount() - quantity);
				if (existingAllocation.getAllocationAmount() == 0) {
					cancel(existingAllocation);
				}

				return newAllocation;
			}
		}

		Logger.error("Could not swap allocation purpose");
		return null;
	}

	public ItemAllocation getAllocationForPurpose(ItemAllocation.Purpose requiredPurpose) {
		for (ItemAllocation allocation : allocations) {
			if (allocation.getPurpose().equals(requiredPurpose)) {
				return allocation;
			}
		}
		return null;
	}

	public int getNumAllocated() {
		int total = 0;
		for (ItemAllocation itemAllocation : allocations) {
			total += itemAllocation.getAllocationAmount();
		}
		return total;
	}

	public int getNumUnallocated() {
		ItemEntityAttributes attributes = (ItemEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		return attributes.getQuantity() - getNumAllocated();
	}


	public List<ItemAllocation> getAll() {
		return this.allocations;
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {

	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!allocations.isEmpty()) {
			JSONArray allocationsArray = new JSONArray();
			for (ItemAllocation allocation : allocations) {
				allocation.writeTo(savedGameStateHolder);
				allocationsArray.add(allocation.getItemAllocationId());
			}
			asJson.put("allocations", allocationsArray);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONArray allocationsArray = asJson.getJSONArray("allocations");
		if (allocationsArray != null) {
			for (int cursor = 0; cursor < allocationsArray.size(); cursor++) {
				long allocationId = allocationsArray.getLongValue(cursor);
				ItemAllocation itemAllocation = savedGameStateHolder.itemAllocations.get(allocationId);
				if (itemAllocation == null) {
					throw new InvalidSaveException("Could not find item allocation with ID " + allocationId);
				} else {
					this.allocations.add(itemAllocation);
				}
			}
		}
	}
}
