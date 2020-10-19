package technology.rocketjump.undermount.entities.components;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.zones.ZoneTile;

import java.util.Objects;

import static technology.rocketjump.undermount.entities.components.LiquidAllocation.LiquidAllocationType.FROM_LIQUID_CONTAINER;
import static technology.rocketjump.undermount.entities.components.LiquidAllocation.LiquidAllocationType.FROM_RIVER;
import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;

/**
 * A liquid allocation represents a portion of liquid within a furniture container or other large source (i.e. river)
 * For items containing liquids (e.g. bucket, barrel) the entire item is allocated instead with an ItemAllocation
 */
public class LiquidAllocation implements Persistable {

	private float allocationAmount = 1f;
	private LiquidAllocationType type;
	private ZoneTile targetZoneTile; // TODO these can be removed for container-type allocations

	private Long liquidAllocationId;
	private Long targetContainerId;
	private Long requesterEntityId;
	private ItemAllocation.AllocationState state = ItemAllocation.AllocationState.ACTIVE;
	private GameMaterial liquidMaterial = NULL_MATERIAL;

	public LiquidAllocation() {

	}

	public static LiquidAllocation fromRiver(ZoneTile targetZoneTile, TiledMap map) {
		GameMaterial riverFloorMaterial = map.getTile(targetZoneTile.getTargetTile()).getFloor().getMaterial();
		return new LiquidAllocation(FROM_RIVER, targetZoneTile, 1f, riverFloorMaterial);
	}

	public LiquidAllocation(LiquidAllocationType type, ZoneTile targetZoneTile, float allocationAmount, GameMaterial liquidMaterial) {
		this.liquidAllocationId = SequentialIdGenerator.nextId();
		this.type = type;
		this.targetZoneTile = targetZoneTile;
		this.allocationAmount = allocationAmount;
		this.liquidMaterial = liquidMaterial;
	}

	public Long getTargetContainerId() {
		return targetContainerId;
	}

	public void setTargetContainerId(Long targetContainerId) {
		this.targetContainerId = targetContainerId;
	}

	public Long getLiquidAllocationId() {
		return liquidAllocationId;
	}

	public void setLiquidAllocationId(Long liquidAllocationId) {
		this.liquidAllocationId = liquidAllocationId;
	}

	public Long getRequesterEntityId() {
		return requesterEntityId;
	}

	public void setRequesterEntityId(Long requesterEntityId) {
		this.requesterEntityId = requesterEntityId;
	}

	public ItemAllocation.AllocationState getState() {
		return state;
	}

	public void setState(ItemAllocation.AllocationState state) {
		this.state = state;
	}

	public ZoneTile getTargetZoneTile() {
		return targetZoneTile;
	}

	public float getAllocationAmount() {
		return allocationAmount;
	}

	public GameMaterial getLiquidMaterial() {
		return liquidMaterial;
	}

	public void setLiquidMaterial(GameMaterial liquidMaterial) {
		this.liquidMaterial = liquidMaterial;
	}

	@Override
	public String toString() {
		return "LiquidAllocation{" +
				"allocationAmount=" + allocationAmount +
				", type=" + type +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LiquidAllocation that = (LiquidAllocation) o;
		return Objects.equals(liquidAllocationId, that.liquidAllocationId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(liquidAllocationId);
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.liquidAllocations.containsKey(this.liquidAllocationId)) {
			return;
		}

		JSONObject asJson = new JSONObject(true);
		asJson.put("id", liquidAllocationId);

		asJson.put("type", type.name());

		JSONObject tileJson = new JSONObject(true);
		targetZoneTile.writeTo(tileJson, savedGameStateHolder);
		asJson.put("tile", tileJson);

		if (allocationAmount != 1f) {
			asJson.put("amount", allocationAmount);
		}

		asJson.put("container", targetContainerId);
		asJson.put("requester", requesterEntityId);

		if (!state.equals(ItemAllocation.AllocationState.ACTIVE)) {
			asJson.put("state", state.name());
		}

		if (NULL_MATERIAL.equals(liquidMaterial)) {
			asJson.put("material", liquidMaterial.getMaterialName());
		}

		savedGameStateHolder.liquidAllocations.put(this.liquidAllocationId, this);
		savedGameStateHolder.liquidAllocationsJson.add(asJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.liquidAllocationId = asJson.getLong("id");

		this.type = EnumParser.getEnumValue(asJson, "type", LiquidAllocationType.class, FROM_LIQUID_CONTAINER);

		JSONObject tileJson = asJson.getJSONObject("tile");
		if (tileJson == null) {
			throw new InvalidSaveException("Could not load zonetile");
		} else {
			this.targetZoneTile = new ZoneTile();
			this.targetZoneTile.readFrom(tileJson, savedGameStateHolder, relatedStores);
		}

		allocationAmount = asJson.getFloatValue("amount");
		if (allocationAmount == 0f) {
			allocationAmount = 1f;
		}

		this.targetContainerId = asJson.getLongValue("container");
		this.requesterEntityId = asJson.getLongValue("requester");
		this.state = EnumParser.getEnumValue(asJson, "state", ItemAllocation.AllocationState.class, ItemAllocation.AllocationState.ACTIVE);

		if (asJson.containsKey("material")) {
			this.liquidMaterial = relatedStores.gameMaterialDictionary.getByName(asJson.getString("material"));
		}
		if (this.liquidMaterial == null) {
			liquidMaterial = NULL_MATERIAL;
		}

		savedGameStateHolder.liquidAllocations.put(this.liquidAllocationId, this);
	}

	public LiquidAllocationType getType() {
		return type;
	}

	public enum LiquidAllocationType {

		FROM_RIVER,
		FROM_LIQUID_CONTAINER,
		CRAFTING_ASSIGNMENT,
		PARENT_HAULING

	}

}
