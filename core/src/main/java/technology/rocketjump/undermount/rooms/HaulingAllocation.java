package technology.rocketjump.undermount.rooms;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.components.ItemAllocation;
import technology.rocketjump.undermount.entities.components.LiquidAllocation;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.undermount.entities.model.EntityType.ITEM;

public class HaulingAllocation implements Persistable {

	private Long haulingAllocationId;

	private AllocationPositionType sourcePositionType;
	private GridPoint2 sourcePosition;
	private Long sourceContainerId;

	private EntityType hauledEntityType = ITEM;
	private Long hauledEntityId; // This is the "source" entity

	private AllocationPositionType targetPositionType;
	private GridPoint2 targetPosition;
	private Long targetId; // ID of whatever type of thing targetPositionType is

	private ItemAllocation itemAllocation; // When hauling an item, the allocation
	private LiquidAllocation liquidAllocation; // When item contains liquid, the liquid allocation

	public HaulingAllocation() {
		this.haulingAllocationId = SequentialIdGenerator.nextId();
	}

	public HaulingAllocation clone() {
		HaulingAllocation cloned = new HaulingAllocation();
		cloned.sourcePositionType = this.sourcePositionType;
		cloned.targetPositionType = this.targetPositionType;
		cloned.targetId = this.targetId;
		cloned.hauledEntityId = this.hauledEntityId;
		cloned.targetPosition = this.targetPosition;
		return cloned;
	}

	public GridPoint2 getTargetPosition() {
		return targetPosition;
	}

	public void setTargetPosition(GridPoint2 targetPosition) {
		this.targetPosition = targetPosition;
	}

	public Long getHauledEntityId() {
		return hauledEntityId;
	}

	public void setHauledEntityId(Long hauledEntityId) {
		this.hauledEntityId = hauledEntityId;
	}

	public AllocationPositionType getTargetPositionType() {
		return targetPositionType;
	}

	public void setTargetPositionType(AllocationPositionType targetPositionType) {
		this.targetPositionType = targetPositionType;
	}

	public AllocationPositionType getSourcePositionType() {
		return sourcePositionType;
	}

	public void setSourcePositionType(AllocationPositionType sourcePositionType) {
		this.sourcePositionType = sourcePositionType;
	}

	public GridPoint2 getSourcePosition() {
		return sourcePosition;
	}

	public void setSourcePosition(GridPoint2 sourceContainerPosition) {
		this.sourcePosition = sourceContainerPosition;
	}

	public Long getSourceContainerId() {
		return sourceContainerId;
	}

	public void setSourceContainerId(Long sourceContainerId) {
		this.sourceContainerId = sourceContainerId;
	}

	public Long getTargetId() {
		return targetId;
	}

	public void setTargetId(Long targetId) {
		this.targetId = targetId;
	}

	public Long getHaulingAllocationId() {
		return haulingAllocationId;
	}

	public EntityType getHauledEntityType() {
		return hauledEntityType;
	}

	public void setHauledEntityType(EntityType hauledEntityType) {
		this.hauledEntityType = hauledEntityType;
	}

	public ItemAllocation getItemAllocation() {
		return itemAllocation;
	}

	public void setItemAllocation(ItemAllocation itemAllocation) {
		this.itemAllocation = itemAllocation;
		this.hauledEntityId = itemAllocation.getTargetItemEntityId();
		itemAllocation.setRelatedHaulingAllocationId(this.haulingAllocationId);
	}

	public void setLiquidAllocation(LiquidAllocation liquidAllocation) {
		this.liquidAllocation = liquidAllocation;
	}

	public LiquidAllocation getLiquidAllocation() {
		return liquidAllocation;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		HaulingAllocation that = (HaulingAllocation) o;
		return haulingAllocationId.equals(that.haulingAllocationId);
	}

	@Override
	public int hashCode() {
		return haulingAllocationId.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(hauledEntityType).append(" ");
		if (itemAllocation != null) {
			sb.append(itemAllocation.toString());
		}
		sb.append(" -> ").append(targetPosition);
		return sb.toString();
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.haulingAllocations.containsKey(this.haulingAllocationId)) {
			return;
		}

		JSONObject asJson = new JSONObject(true);
		asJson.put("id", haulingAllocationId);
		if (sourcePositionType != null) {
			asJson.put("sourcePositionType", sourcePositionType.name());
		}
		if (sourcePosition != null) {
			asJson.put("sourcePosition", sourcePosition);
		}
		if (sourceContainerId != null) {
			asJson.put("sourceContainerId", sourceContainerId);
		}

		if (targetPositionType != null) {
			asJson.put("targetPositionType", targetPositionType.name());
		}
		if (targetPosition != null) {
			asJson.put("targetPosition", targetPosition);
		}
		if (targetId != null) {
			asJson.put("targetId", targetId);
		}

		if (!hauledEntityType.equals(ITEM)) {
			asJson.put("hauledType", hauledEntityType.name());
		}
		if (hauledEntityId != null) {
			asJson.put("hauledEntityId", hauledEntityId);
		}
		if (itemAllocation != null) {
			itemAllocation.writeTo(savedGameStateHolder);
			asJson.put("itemAllocationId", itemAllocation.getItemAllocationId());
		}
		if (liquidAllocation != null) {
			liquidAllocation.writeTo(savedGameStateHolder);
			asJson.put("liquidAllocationId", liquidAllocation.getLiquidAllocationId());
		}

		savedGameStateHolder.haulingAllocations.put(this.haulingAllocationId, this);
		savedGameStateHolder.haulingAllocationsJson.add(asJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries dictionaries) throws InvalidSaveException {
		this.haulingAllocationId = asJson.getLongValue("id");
		this.sourcePositionType = EnumParser.getEnumValue(asJson, "sourcePositionType", AllocationPositionType.class, null);
		JSONObject sourcePositionJson = asJson.getJSONObject("sourcePosition");
		if (sourcePositionJson != null) {
			this.sourcePosition = new GridPoint2(sourcePositionJson.getIntValue("x"), sourcePositionJson.getIntValue("y"));
		}
		this.sourceContainerId = asJson.getLong("sourceContainerId");

		this.targetPositionType = EnumParser.getEnumValue(asJson, "targetPositionType", AllocationPositionType.class, null);
		JSONObject targetPositionJson = asJson.getJSONObject("targetPosition");
		if (targetPositionJson != null) {
			this.targetPosition = new GridPoint2(targetPositionJson.getIntValue("x"), targetPositionJson.getIntValue("y"));
		}
		this.targetId = asJson.getLong("targetId");

		this.hauledEntityType = EnumParser.getEnumValue(asJson, "hauledType", EntityType.class, ITEM);
		this.hauledEntityId = asJson.getLong("hauledEntityId");

		Long itemAllocationId = asJson.getLong("itemAllocationId");
		if (itemAllocationId != null) {
			this.itemAllocation = savedGameStateHolder.itemAllocations.get(itemAllocationId);
			if (this.itemAllocation == null) {
				throw new InvalidSaveException("Could not find item allocation with ID: " + itemAllocationId);
			}
		}

		Long liquidAllocationId = asJson.getLong("liquidAllocationId");
		if (liquidAllocationId != null) {
			this.liquidAllocation  = savedGameStateHolder.liquidAllocations.get(liquidAllocationId);
			if (this.liquidAllocation == null) {
				throw new InvalidSaveException("Could not find liquid allocation with ID: " + liquidAllocationId);
			}
		}

		savedGameStateHolder.haulingAllocations.put(this.haulingAllocationId, this);
	}

	// Where the allocation is going  to or from
	public enum AllocationPositionType {
		ROOM, CONSTRUCTION, FURNITURE, FLOOR, ZONE
	}
}
