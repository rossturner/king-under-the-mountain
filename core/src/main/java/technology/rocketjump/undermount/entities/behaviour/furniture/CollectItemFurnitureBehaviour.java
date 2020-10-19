package technology.rocketjump.undermount.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeWithMaterial;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobState;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.jobs.model.Profession;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RequestHaulingAllocationMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rooms.HaulingAllocation;

import java.util.ArrayList;
import java.util.List;

import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;

public class CollectItemFurnitureBehaviour extends FurnitureBehaviour {

	private List<ItemTypeWithMaterial> itemsToCollect = new ArrayList<>();
	private List<ItemTypeWithMaterial> inventoryAssignments = new ArrayList<>();
	private int maxNumItemStacks = 0;
	private List<Job> incomingHaulingJobs = new ArrayList<>();
	private Profession requiredProfession = null;
	private boolean allowDuplicates = false;
	private JobType haulingJobType;

	@Override
	public FurnitureBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		Logger.error(this.getClass().getSimpleName() + ".clone() not yet implemented");
		return new CollectItemFurnitureBehaviour();
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);
		incomingHaulingJobs.removeIf(job -> job.getJobState().equals(JobState.REMOVED));

		InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);

		// Only when no hauling jobs incoming, clear empty inventory assignments
		if (incomingHaulingJobs.isEmpty() && !inventoryAssignments.isEmpty()) {
			removeEmptyInventoryAssignments(gameContext, inventoryComponent);
		}

		// Try to create new assignments up to maxNumItemStacks
		if (inventoryAssignments.size() < maxNumItemStacks) {
			ItemTypeWithMaterial potentialItemTypeWithMaterial = itemsToCollect.get(gameContext.getRandom().nextInt(itemsToCollect.size()));
			if (allowDuplicates || !inventoryAssignments.contains(potentialItemTypeWithMaterial)) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION, new RequestHaulingAllocationMessage(
						parentEntity, parentEntity.getLocationComponent().getWorldOrParentPosition(), potentialItemTypeWithMaterial.getItemType(), potentialItemTypeWithMaterial.getMaterial(),
						false, null, null, allocation -> {
							if (allocation != null) {
								finaliseAllocation(potentialItemTypeWithMaterial, allocation);
							}
						}
				));
			}

		}
	}

	public boolean canAccept(Entity itemEntity) {
		if (inventoryAssignments.size() < maxNumItemStacks) {
			ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
			ItemTypeWithMaterial matching = getMatch(attributes);
			if (matching != null) {
				if (allowDuplicates || !inventoryAssignments.contains(matching)) {
					return true;
				}
			}
		}
		return false;
	}

	public void finaliseAllocation(ItemTypeWithMaterial potentialItemTypeWithMaterial, HaulingAllocation allocation) {
		inventoryAssignments.add(potentialItemTypeWithMaterial);
		// Create hauling job to haul assignment into inventory

		allocation.setTargetPositionType(HaulingAllocation.AllocationPositionType.FURNITURE);
		allocation.setTargetId(parentEntity.getId());
		allocation.setTargetPosition(toGridPoint(parentEntity.getLocationComponent().getWorldPosition()));

		Job haulingJob = new Job(haulingJobType);
		haulingJob.setTargetId(allocation.getHauledEntityId());
		haulingJob.setHaulingAllocation(allocation);
		haulingJob.setJobLocation(allocation.getSourcePosition());

		if (requiredProfession != null) {
			haulingJob.setRequiredProfession(requiredProfession);
		}

		incomingHaulingJobs.add(haulingJob);
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJob);
	}

	public ItemTypeWithMaterial getMatch(ItemEntityAttributes attributes) {
		for (ItemTypeWithMaterial itemTypeWithMaterial : itemsToCollect) {
			if (attributes.getItemType().equals(itemTypeWithMaterial.getItemType()) &&
					attributes.getMaterial(attributes.getItemType().getPrimaryMaterialType()).equals(itemTypeWithMaterial.getMaterial())) {
				return itemTypeWithMaterial;
			}
		}
		return null;
	}

	public void setItemsToCollect(List<ItemTypeWithMaterial> itemsToCollect) {
		this.itemsToCollect = itemsToCollect;
	}

	public void setMaxNumItemStacks(int maxNumItemStacks) {
		this.maxNumItemStacks = maxNumItemStacks;
	}

	private void removeEmptyInventoryAssignments(GameContext gameContext, InventoryComponent inventoryComponent) {
		List<ItemTypeWithMaterial> refreshedInventoryAssignments = new ArrayList<>();

		for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
			ItemTypeWithMaterial match = getMatch((ItemEntityAttributes) inventoryEntry.entity.getPhysicalEntityComponent().getAttributes());
			if (match != null) {
				refreshedInventoryAssignments.add(match);
			}
		}
		this.inventoryAssignments = refreshedInventoryAssignments;
	}

	public void setRequiredProfession(Profession requiredProfession) {
		this.requiredProfession = requiredProfession;
	}

	public void setAllowDuplicates(boolean allowDuplicates) {
		this.allowDuplicates = allowDuplicates;
	}

	public boolean getAllowDuplicates() {
		return allowDuplicates;
	}

	public List<ItemTypeWithMaterial> getItemsToCollect() {
		return itemsToCollect;
	}

	public void setHaulingJobType(JobType haulingJobType) {
		this.haulingJobType = haulingJobType;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		JSONArray itemsToCollectJson = new JSONArray();
		for (ItemTypeWithMaterial itemTypeWithMaterial : itemsToCollect) {
			JSONObject itemTypeWithMaterialJson = new JSONObject(true);
			itemTypeWithMaterial.writeTo(itemTypeWithMaterialJson, savedGameStateHolder);
			itemsToCollectJson.add(itemTypeWithMaterialJson);
		}
		asJson.put("itemsToCollect", itemsToCollectJson);

		JSONArray inventoryAssignmentsJson = new JSONArray();
		for (ItemTypeWithMaterial inventoryAssignment : inventoryAssignments) {
			JSONObject inventoryAssignmentJson = new JSONObject(true);
			inventoryAssignment.writeTo(inventoryAssignmentJson, savedGameStateHolder);
			inventoryAssignmentsJson.add(inventoryAssignmentJson);
		}
		asJson.put("inventoryAssignments", inventoryAssignmentsJson);

		asJson.put("maxNumItemStacks", maxNumItemStacks);

		if (!incomingHaulingJobs.isEmpty()) {
			JSONArray incomingHaulingJobsJson = new JSONArray();
			for (Job haulingJob : incomingHaulingJobs) {
				haulingJob.writeTo(savedGameStateHolder);
				incomingHaulingJobsJson.add(haulingJob.getJobId());
			}
			asJson.put("jobs", incomingHaulingJobsJson);
		}

		if (requiredProfession != null) {
			asJson.put("requiredProfession", requiredProfession.getName());
		}
		if (allowDuplicates) {
			asJson.put("allowDuplicates", true);
		}

		asJson.put("haulingJobType", haulingJobType.getName());
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		JSONArray itemsToCollectJson = asJson.getJSONArray("itemsToCollect");
		for (int cursor = 0; cursor < itemsToCollectJson.size(); cursor++) {
			ItemTypeWithMaterial itemTypeWithMaterial = new ItemTypeWithMaterial();
			itemTypeWithMaterial.readFrom(itemsToCollectJson.getJSONObject(cursor), savedGameStateHolder, relatedStores);
			this.itemsToCollect.add(itemTypeWithMaterial);
		}

		JSONArray inventoryAssignmentsJson = asJson.getJSONArray("inventoryAssignments");
		for (int cursor = 0; cursor < inventoryAssignmentsJson.size(); cursor++) {
			ItemTypeWithMaterial itemTypeWithMaterial = new ItemTypeWithMaterial();
			itemTypeWithMaterial.readFrom(inventoryAssignmentsJson.getJSONObject(cursor), savedGameStateHolder, relatedStores);
			this.inventoryAssignments.add(itemTypeWithMaterial);
		}

		this.maxNumItemStacks = asJson.getIntValue("maxNumItemStacks");

		JSONArray incomingHaulingJobsJson = asJson.getJSONArray("jobs");
		if (incomingHaulingJobsJson != null) {
			for (int cursor = 0; cursor < incomingHaulingJobsJson.size(); cursor++) {
				long jobId = incomingHaulingJobsJson.getLongValue(cursor);
				Job job = savedGameStateHolder.jobs.get(jobId);
				if (job == null) {
					throw new InvalidSaveException("Could not find job by ID " + jobId);
				} else {
					incomingHaulingJobs.add(job);
				}
			}
		}

		String requiredProfessionName = asJson.getString("requiredProfession");
		if (requiredProfessionName != null) {
			this.requiredProfession = relatedStores.professionDictionary.getByName(requiredProfessionName);
			if (this.requiredProfession == null) {
				throw new InvalidSaveException("Could not find profession by name " + requiredProfessionName);
			}
		}

		this.allowDuplicates = asJson.getBooleanValue("allowDuplicates");

		this.haulingJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("haulingJobType"));
		if (haulingJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("haulingJobType"));
		}
	}
}
