package technology.rocketjump.undermount.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.ItemAllocation;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.components.furniture.DecorationInventoryComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.*;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RequestCorpseMessage;
import technology.rocketjump.undermount.messaging.types.RequestHaulingMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rooms.HaulingAllocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static technology.rocketjump.undermount.entities.behaviour.furniture.CraftingStationBehaviour.getAnyNavigableWorkspace;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;

public class ButcheryStationBehaviour extends FurnitureBehaviour implements Prioritisable {

	private List<Job> haulingJobs = new ArrayList<>();
	private Job butcheryJob;

	private Profession requiredProfession = null;
	private JobType haulingJobType;
	private JobType butcheryJobType;

	@Override
	public FurnitureBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		Logger.error(this.getClass().getSimpleName() + ".clone() not yet implemented");
		return new ButcheryStationBehaviour();
	}

	@Override
	public void setPriority(JobPriority jobPriority) {
		super.setPriority(jobPriority);
		for (Job incomingHaulingJob : haulingJobs) {
			incomingHaulingJob.setJobPriority(jobPriority);
		}
		if (butcheryJob != null) {
			butcheryJob.setJobPriority(jobPriority);
		}
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);
		haulingJobs.removeIf(job -> job.getJobState().equals(JobState.REMOVED));
		if (butcheryJob != null && butcheryJob.getJobState().equals(JobState.REMOVED)) {
			butcheryJob = null;
		}

		if (parentEntity.isOnFire()) {
			haulingJobs.forEach(job -> messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, job));
			if (butcheryJob != null) {
				messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, butcheryJob);
			}
			return;
		}

		InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);

		if (haulingJobs.isEmpty() && inventoryComponent.isEmpty()) {
			// Try to create new incoming hauling assignment
			messageDispatcher.dispatchMessage(MessageType.FIND_BUTCHERABLE_UNALLOCATED_CORPSE, new RequestCorpseMessage(
					parentEntity, parentEntity.getLocationComponent().getWorldOrParentPosition(), entity -> {
				if (entity != null) {
					createIncomingHaulingJob(entity);
				}
			}));
		}

		// empty out item-type entities
		for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
			if (inventoryEntry.entity.getType().equals(EntityType.ITEM)) {
				ItemAllocationComponent itemAllocationComponent = inventoryEntry.entity.getComponent(ItemAllocationComponent.class);
				if (itemAllocationComponent.getNumUnallocated() > 0) {
					messageDispatcher.dispatchMessage(MessageType.REQUEST_ENTITY_HAULING, new RequestHaulingMessage(
							inventoryEntry.entity, parentEntity, true, priority, job -> haulingJobs.add(job)
					));
				}
			}
		}

		if (!inventoryComponent.isEmpty() && butcheryJob == null && haulingJobs.isEmpty()) {
			createButcheryJob(gameContext);
		}

		if (butcheryJob != null) {
			boolean butcheryJobIsNavigable = gameContext.getAreaMap().getTile(butcheryJob.getJobLocation()).isNavigable();
			if (!butcheryJobIsNavigable) {
				FurnitureLayout.Workspace navigableWorkspace = getAnyNavigableWorkspace(parentEntity, gameContext.getAreaMap());
				if (navigableWorkspace != null) {
					butcheryJob.setJobLocation(navigableWorkspace.getAccessedFrom());
					butcheryJob.setSecondaryLocation(navigableWorkspace.getLocation());
				}
			}
		}
	}

	private void createIncomingHaulingJob(Entity corpseEntity) {
		HaulingAllocation allocation = new HaulingAllocation();
		allocation.setHauledEntityId(corpseEntity.getId());
		allocation.setHauledEntityType(corpseEntity.getType());

		ItemAllocation itemAllocation = corpseEntity.getComponent(ItemAllocationComponent.class)
				.createAllocation(1, parentEntity, ItemAllocation.Purpose.DUE_TO_BE_HAULED);
		allocation.setItemAllocation(itemAllocation);

		allocation.setSourcePosition(toGridPoint(corpseEntity.getLocationComponent().getWorldPosition()));
		allocation.setSourcePositionType(HaulingAllocation.AllocationPositionType.FLOOR);

		allocation.setTargetPositionType(HaulingAllocation.AllocationPositionType.FURNITURE);
		allocation.setTargetId(parentEntity.getId());
		allocation.setTargetPosition(toGridPoint(parentEntity.getLocationComponent().getWorldPosition()));

		Job haulingJob = new Job(haulingJobType);
		haulingJob.setJobPriority(priority);
		haulingJob.setTargetId(allocation.getHauledEntityId());
		haulingJob.setHaulingAllocation(allocation);
		haulingJob.setJobLocation(allocation.getSourcePosition());

		if (requiredProfession != null) {
			haulingJob.setRequiredProfession(requiredProfession);
		}

		haulingJobs.add(haulingJob);
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJob);
	}

	private void createButcheryJob(GameContext gameContext) {
		FurnitureLayout.Workspace navigableWorkspace = getAnyNavigableWorkspace(parentEntity, gameContext.getAreaMap());
		Collection<Entity> decorationEntities = parentEntity.getComponent(DecorationInventoryComponent.class).getDecorationEntities();
		if (navigableWorkspace == null) {
			Logger.warn("Could not access workstation at " + parentEntity.getLocationComponent().getWorldPosition());
			return;
		}

		butcheryJob = new Job(butcheryJobType);
		butcheryJob.setJobPriority(priority);
		butcheryJob.setTargetId(parentEntity.getId());
		butcheryJob.setJobLocation(navigableWorkspace.getAccessedFrom());
		butcheryJob.setSecondaryLocation(navigableWorkspace.getLocation());
		if (!decorationEntities.isEmpty()) {
			butcheryJob.setRequiredItemType(((ItemEntityAttributes) decorationEntities.iterator().next().getPhysicalEntityComponent().getAttributes()).getItemType());
		}

		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, butcheryJob);
	}


	public void setRequiredProfession(Profession requiredProfession) {
		this.requiredProfession = requiredProfession;
	}

	public void setHaulingJobType(JobType haulingJobType) {
		this.haulingJobType = haulingJobType;
	}

	public void setButcheryJobType(JobType butcheryJobType) {
		this.butcheryJobType = butcheryJobType;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		if (!haulingJobs.isEmpty()) {
			JSONArray incomingHaulingJobsJson = new JSONArray();
			for (Job haulingJob : haulingJobs) {
				haulingJob.writeTo(savedGameStateHolder);
				incomingHaulingJobsJson.add(haulingJob.getJobId());
			}
			asJson.put("jobs", incomingHaulingJobsJson);
		}

		if (butcheryJob != null) {
			butcheryJob.writeTo(savedGameStateHolder);
			asJson.put("butcheryJob", butcheryJob.getJobId());
		}

		if (requiredProfession != null) {
			asJson.put("requiredProfession", requiredProfession.getName());
		}
		asJson.put("haulingJobType", haulingJobType.getName());
		asJson.put("butcheryJobType", butcheryJobType.getName());
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		JSONArray incomingHaulingJobsJson = asJson.getJSONArray("jobs");
		if (incomingHaulingJobsJson != null) {
			for (int cursor = 0; cursor < incomingHaulingJobsJson.size(); cursor++) {
				long jobId = incomingHaulingJobsJson.getLongValue(cursor);
				Job job = savedGameStateHolder.jobs.get(jobId);
				if (job == null) {
					throw new InvalidSaveException("Could not find job by ID " + jobId);
				} else {
					haulingJobs.add(job);
				}
			}
		}
		Long butcheryJobId = asJson.getLong("butcheryJob");
		if (butcheryJobId != null) {
			this.butcheryJob = savedGameStateHolder.jobs.get(butcheryJobId);
			if (butcheryJob == null) {
				throw new InvalidSaveException("Could not find butchery job by ID " + butcheryJobId);
			}
		}

		String requiredProfessionName = asJson.getString("requiredProfession");
		if (requiredProfessionName != null) {
			this.requiredProfession = relatedStores.professionDictionary.getByName(requiredProfessionName);
			if (this.requiredProfession == null) {
				throw new InvalidSaveException("Could not find profession by name " + requiredProfessionName);
			}
		}

		this.haulingJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("haulingJobType"));
		if (haulingJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("haulingJobType"));
		}

		this.butcheryJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("butcheryJobType"));
		if (butcheryJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("butcheryJobType"));
		}
	}

}
