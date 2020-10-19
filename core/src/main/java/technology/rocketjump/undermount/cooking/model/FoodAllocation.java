package technology.rocketjump.undermount.cooking.model;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.entities.components.ItemAllocation;
import technology.rocketjump.undermount.entities.components.LiquidAllocation;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

/**
 * This class represents which food has been assigned and where it is
 */
public class FoodAllocation implements ChildPersistable {

	private FoodAllocationType type;
	private Entity targetEntity;
	private boolean isPreparedMeal;
	private ItemAllocation itemAllocaton;
	private LiquidAllocation liquidAllocation;

	public FoodAllocation() {

	}

	public FoodAllocation(FoodAllocationType type, Entity targetEntity, ItemAllocation itemAllocation) {
		this.type = type;
		this.targetEntity = targetEntity;
		this.itemAllocaton = itemAllocation;
	}

	public FoodAllocation(FoodAllocationType type, Entity targetEntity, LiquidAllocation liquidAllocation) {
		this.type = type;
		this.targetEntity = targetEntity;
		this.liquidAllocation = liquidAllocation;
	}

	public FoodAllocationType getType() {
		return type;
	}

	public void setType(FoodAllocationType type) {
		this.type = type;
	}

	public Entity getTargetEntity() {
		return targetEntity;
	}

	public void setTargetEntity(Entity targetEntity) {
		this.targetEntity = targetEntity;
	}

	public void setPreparedMeal(boolean preparedMeal) {
		isPreparedMeal = preparedMeal;
	}

	public boolean isPreparedMeal() {
		return isPreparedMeal;
	}

	public ItemAllocation getItemAllocaton() {
		return itemAllocaton;
	}

	public void setItemAllocaton(ItemAllocation itemAllocaton) {
		this.itemAllocaton = itemAllocaton;
	}

	public LiquidAllocation getLiquidAllocation() {
		return liquidAllocation;
	}

	public void setLiquidAllocation(LiquidAllocation liquidAllocation) {
		this.liquidAllocation = liquidAllocation;
	}

	@Override
	public String toString() {
		return type.name() + " - " + targetEntity.toString();
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("type", type.name());

		targetEntity.writeTo(savedGameStateHolder);
		asJson.put("targetEntity", targetEntity.getId());

		if (isPreparedMeal) {
			asJson.put("prepared", true);
		}

		if (itemAllocaton != null) {
			itemAllocaton.writeTo(savedGameStateHolder);
			asJson.put("itemAllocationId", itemAllocaton.getItemAllocationId());
		}

		if (liquidAllocation != null) {
			liquidAllocation.writeTo(savedGameStateHolder);
			asJson.put("liquidAllocationId", liquidAllocation.getLiquidAllocationId());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		type = EnumParser.getEnumValue(asJson, "type", FoodAllocationType.class, FoodAllocationType.LOOSE_ITEM);

		this.targetEntity = savedGameStateHolder.entities.get(asJson.getLongValue("targetEntity"));
		if (this.targetEntity == null) {
			throw new InvalidSaveException("Could not find entity by ID " + asJson.getLongValue("targetEntity"));
		}

		this.isPreparedMeal = asJson.getBooleanValue("prepared");

		Long itemAllocationId = asJson.getLong("itemAllocationId");
		if (itemAllocationId != null) {
			this.itemAllocaton = savedGameStateHolder.itemAllocations.get(itemAllocationId);
			if (this.itemAllocaton == null) {
				throw new InvalidSaveException("Could not find itemAllocation with ID " + itemAllocationId);
			}
		}

		Long liquidAllocationId = asJson.getLong("liquidAllocationId");
		if (liquidAllocationId != null) {
			this.liquidAllocation = savedGameStateHolder.liquidAllocations.get(liquidAllocationId);
			if (this.liquidAllocation == null) {
				throw new InvalidSaveException("Could not find liquidAllocation with ID " + liquidAllocationId);
			}
		}

	}

	public enum FoodAllocationType {

		LIQUID_CONTAINER,
		FURNITURE_INVENTORY,
		LOOSE_ITEM,
		REQUESTER_INVENTORY

	}



}
