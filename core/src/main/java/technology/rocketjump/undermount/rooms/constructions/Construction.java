package technology.rocketjump.undermount.rooms.constructions;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.undermount.entities.components.ItemAllocation;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.components.LiquidContainerComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.undermount.entities.tags.ConstructionOverrideTag;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rooms.HaulingAllocation;

import java.util.*;

import static technology.rocketjump.undermount.entities.components.ItemAllocation.Purpose.PLACED_FOR_CONSTRUCTION;
import static technology.rocketjump.undermount.entities.tags.ConstructionOverrideTag.ConstructionOverrideSetting.REQUIRES_EDIBLE_LIQUID;
import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;

public abstract class Construction implements Persistable {

	protected long constructionId;

	public long getId() {
		return constructionId;
	}

	public abstract Set<GridPoint2> getTileLocations();

	public abstract ConstructionType getConstructionType();

	protected ConstructionState state = ConstructionState.CLEARING_WORK_SITE;
	protected Job constructionJob;
	protected List<HaulingAllocation> incomingHaulingAllocations = new ArrayList<>();
	protected Map<GridPoint2, ItemAllocation> placedItemAllocations = new HashMap<>();
	protected GameMaterialType primaryMaterialType;
	protected List<QuantifiedItemTypeWithMaterial> requirements = new ArrayList<>(); // Note that material will be null initially
	protected Set<ConstructionOverrideTag.ConstructionOverrideSetting> constructionOverrideSettings = new HashSet<>();

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Construction that = (Construction) o;
		return getId() == that.getId();
	}

	@Override
	public int hashCode() {
		return (int) (getId() ^ (getId() >>> 32));
	}

	public abstract Entity getEntity();

	public void allocationCancelled(HaulingAllocation allocation) {
		incomingHaulingAllocations.remove(allocation);
	}

	public boolean isItemUsedInConstruction(Entity itemEntity) {
		ItemEntityAttributes itemAttributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
		for (QuantifiedItemTypeWithMaterial requirement : requirements) {
			if (requirement.getItemType().equals(itemAttributes.getItemType()) &&
					(requirement.getMaterial()) == null || (requirement.getMaterial() != null &&
							requirement.getMaterial().equals(itemAttributes.getMaterial(requirement.getMaterial().getMaterialType())))) {

				if (constructionOverrideSettings.contains(REQUIRES_EDIBLE_LIQUID)) {
					LiquidContainerComponent liquidContainerComponent = itemEntity.getComponent(LiquidContainerComponent.class);
					if (liquidContainerComponent == null || liquidContainerComponent.getTargetLiquidMaterial() == null) {
						return false;
					} else {
						return liquidContainerComponent.getTargetLiquidMaterial().isEdible() && liquidContainerComponent.getLiquidQuantity() > 0;
					}
				} else {
					return true;
				}
			}
		}
		return false;
	}

	public void newItemPlaced(HaulingAllocation haulingAllocation, Entity itemEntity) {
		this.allocationCancelled(haulingAllocation);

		ItemAllocationComponent itemAllocationComponent = itemEntity.getOrCreateComponent(ItemAllocationComponent.class);
		ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();

		ItemAllocation itemAllocation = itemAllocationComponent.createAllocation(attributes.getQuantity(), itemEntity, PLACED_FOR_CONSTRUCTION);
		this.placedItemAllocations.put(haulingAllocation.getTargetPosition(), itemAllocation);
	}

	public void placedItemQuantityIncreased(HaulingAllocation haulingAllocation, Entity existingItem) {
		this.allocationCancelled(haulingAllocation);

		ItemAllocationComponent itemAllocationComponent = existingItem.getOrCreateComponent(ItemAllocationComponent.class);
		ItemEntityAttributes attributes = (ItemEntityAttributes) existingItem.getPhysicalEntityComponent().getAttributes();

		ItemAllocation existingItemAllocation = itemAllocationComponent.getAllocationForPurpose(PLACED_FOR_CONSTRUCTION);
		// Need to check for null in case this is an item left over from a cancelled construction
		if (existingItemAllocation != null) {
			existingItemAllocation.setAllocationAmount(attributes.getQuantity());
		}

		// above itemAllocation should already exist in this.placedItemAllocations
	}

	public Job getConstructionJob() {
		return constructionJob;
	}

	public void setConstructionJob(Job constructionJob) {
		this.constructionJob = constructionJob;
	}

	public List<QuantifiedItemTypeWithMaterial> getRequirements() {
		return requirements;
	}

	public List<HaulingAllocation> getIncomingHaulingAllocations() {
		return incomingHaulingAllocations;
	}

	public Map<GridPoint2, ItemAllocation> getPlacedItemAllocations() {
		return placedItemAllocations;
	}

	public abstract GridPoint2 getPrimaryLocation();

	public GameMaterial getPrimaryMaterial() {
		for (QuantifiedItemTypeWithMaterial requirement : requirements) {
			GameMaterial material = requirement.getMaterial();
			if (material != null && material.getMaterialType().equals(primaryMaterialType)) {
				return material;
			}
		}
		return NULL_MATERIAL;
	}

	public GameMaterialType getPrimaryMaterialType() {
		return primaryMaterialType;
	}

	public ConstructionState getState() {
		return state;
	}

	public void setState(ConstructionState state) {
		this.state = state;
	}

	public boolean isAutoCompleted() {
		return false;
	}

	public Set<ConstructionOverrideTag.ConstructionOverrideSetting> getConstructionOverrideSettings() {
		return constructionOverrideSettings;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.constructions.containsKey(getId())) {
			return;
		}
		JSONObject asJson = new JSONObject(true);
		asJson.put("_type", getConstructionType().name());
		asJson.put("id", constructionId);

		if (!state.equals(ConstructionState.SELECTING_MATERIALS)) {
			asJson.put("state", state.name());
		}
		if (constructionJob != null) {
			constructionJob.writeTo(savedGameStateHolder);
			asJson.put("constructionJob", constructionJob.getJobId());
		}
		if (!incomingHaulingAllocations.isEmpty()) {
			JSONArray allocatedItemsJson = new JSONArray();
			for (HaulingAllocation allocatedItem : incomingHaulingAllocations) {
				allocatedItem.writeTo(savedGameStateHolder);
				allocatedItemsJson.add(allocatedItem.getHaulingAllocationId());
			}
			asJson.put("allocatedItems", allocatedItemsJson);
		}
		if (primaryMaterialType != null) {
			asJson.put("primaryMaterialType", primaryMaterialType.name());
		}

		if (!requirements.isEmpty()) {
			JSONArray requirementsJson = new JSONArray();
			for (QuantifiedItemTypeWithMaterial requirement : requirements) {
				JSONObject requirementJson = new JSONObject(true);
				requirement.writeTo(requirementJson, savedGameStateHolder);
				requirementsJson.add(requirementJson);
			}
			asJson.put("requirements", requirementsJson);
		}
		if (!placedItemAllocations.isEmpty()) {
			JSONArray itemAllocationsJson = new JSONArray();
			for (Map.Entry<GridPoint2, ItemAllocation> entry : placedItemAllocations.entrySet()) {
				JSONObject entryJson = new JSONObject(true);
				entryJson.put("position", JSONUtils.toJSON(entry.getKey()));
				entryJson.put("allocation", entry.getValue().getItemAllocationId());
			}
			asJson.put("placedItemAllocations", itemAllocationsJson);
		}

		if (!constructionOverrideSettings.isEmpty()) {
			JSONArray overrideSettingsJson = new JSONArray();
			for (ConstructionOverrideTag.ConstructionOverrideSetting overrideSetting : constructionOverrideSettings) {
				overrideSettingsJson.add(overrideSetting.name());
			}
			asJson.put("overrides", overrideSettingsJson);
		}


		savedGameStateHolder.constructions.put(getId(), this);
		savedGameStateHolder.constructionsJson.add(asJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.constructionId = asJson.getLongValue("id");
		if (constructionId == 0L) {
			throw new InvalidSaveException("Could not find construction ID");
		}
		this.state = EnumParser.getEnumValue(asJson, "state", ConstructionState.class, ConstructionState.SELECTING_MATERIALS);
		Long constructionJobId = asJson.getLong("constructionJob");
		if (constructionJobId != null) {
			this.constructionJob = savedGameStateHolder.jobs.get(constructionJobId);
			if (this.constructionJob == null) {
				throw new InvalidSaveException("Could not find job with ID " + constructionJobId);
			}
		}

		JSONArray allocateditemsJson = asJson.getJSONArray("allocatedItems");
		if (allocateditemsJson != null) {
			for (int cursor = 0; cursor < allocateditemsJson.size(); cursor++) {
				HaulingAllocation allocation = savedGameStateHolder.haulingAllocations.get(allocateditemsJson.getLongValue(cursor));
				if (allocation == null) {
					throw new InvalidSaveException("Could not find hauling allocation by ID " + allocateditemsJson.getLongValue(cursor));
				} else {
					this.incomingHaulingAllocations.add(allocation);
				}
			}
		}

		JSONArray requirementsJson = asJson.getJSONArray("requirements");
		if (requirementsJson != null) {
			for (int cursor = 0; cursor < requirementsJson.size(); cursor++) {
				JSONObject requirementJson = requirementsJson.getJSONObject(cursor);
				QuantifiedItemTypeWithMaterial requirement = new QuantifiedItemTypeWithMaterial();
				requirement.readFrom(requirementJson, savedGameStateHolder, relatedStores);
				this.requirements.add(requirement);
			}
		}

		JSONArray itemAllocationsjson = asJson.getJSONArray("placedItemAllocations");
		if (itemAllocationsjson != null) {
			for (int cursor = 0; cursor < itemAllocationsjson.size(); cursor++) {
				JSONObject entryJson = itemAllocationsjson.getJSONObject(cursor);
				Long itemAllocationId = entryJson.getLong("allocation");
				ItemAllocation itemAllocation = savedGameStateHolder.itemAllocations.get(itemAllocationId);
				if (itemAllocation != null) {
					GridPoint2 position = JSONUtils.gridPoint2(entryJson.getJSONObject("position"));
					this.placedItemAllocations.put(position, itemAllocation);
				} else {
					throw new InvalidSaveException("Could not find item allocation by ID "  + itemAllocationId);
				}
			}
		}

		JSONArray overrideSettingsJson = asJson.getJSONArray("overrides");
		if (overrideSettingsJson != null) {
			for (int cursor = 0; cursor < overrideSettingsJson.size(); cursor++) {
				ConstructionOverrideTag.ConstructionOverrideSetting override =
						EnumUtils.getEnum(ConstructionOverrideTag.ConstructionOverrideSetting.class, overrideSettingsJson.getString(cursor));
				if (override == null) {
					throw new InvalidSaveException("Could not find override setting by name " + overrideSettingsJson.getString(cursor));
				} else {
					constructionOverrideSettings.add(override);
				}
			}
		}

		this.primaryMaterialType = EnumParser.getEnumValue(asJson, "primaryMaterialType", GameMaterialType.class, null);

		savedGameStateHolder.constructions.put(getId(), this);
	}
}
