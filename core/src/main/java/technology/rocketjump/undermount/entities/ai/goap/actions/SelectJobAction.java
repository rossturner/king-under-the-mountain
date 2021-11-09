package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.undermount.entities.ai.memory.Memory;
import technology.rocketjump.undermount.entities.ai.memory.MemoryType;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.WeaponSelectionComponent;
import technology.rocketjump.undermount.entities.components.humanoid.MemoryComponent;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.planning.JobAssignmentCallback;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobState;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.JobRequestMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.List;

import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.undermount.ui.views.EntitySelectedGuiView.hasSelectedWeaponAndAmmoInInventory;

public class SelectJobAction extends Action implements JobAssignmentCallback, InitialisableAction {

	private static final float MAX_TIME_TO_WAIT_SECONDS = 5f;

	private JobRequestMessage jobRequest;
	private float timeWaitingForJob = 0f;

	public SelectJobAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void init() {
		if (jobRequest != null) {
			jobRequest.setRequestingEntity(parent.parentEntity);
		}
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (jobRequest == null) {
			jobRequest = new JobRequestMessage(parent.parentEntity, gameContext.getGameClock(), this);
			parent.messageDispatcher.dispatchMessage(MessageType.JOB_REQUESTED, jobRequest);
		}

		timeWaitingForJob += deltaTime;
		if (timeWaitingForJob > MAX_TIME_TO_WAIT_SECONDS) {
			completionType = FAILURE;
			jobRequest.setCancelled(true);
			Logger.info("Timed out while waiting for job callback");
		}
	}

	@Override
	public CompletionType isCompleted(GameContext gameContext) throws SwitchGoalException {
		if (parent.getAssignedJob() != null) {
			if (parent.getAssignedJob().getType().getSwitchToSpecialGoal() != null) {
				parent.setAssignedHaulingAllocation(parent.getAssignedJob().getHaulingAllocation());
				parent.setLiquidAllocation(parent.getAssignedJob().getLiquidAllocation());
				throw new SwitchGoalException(parent.getAssignedJob().getType().getSwitchToSpecialGoal());
			}
		}
		return completionType;
	}

	@Override
	public void jobCallback(List<Job> potentialJobs, GameContext gameContext) {
		if (FAILURE.equals(completionType)) {
			return; // Don't accept any jobs when already failed
		}
		Job selectedJob = null;
		for (Job potentialJob : potentialJobs) {
			if (potentialJob.getJobState().equals(JobState.ASSIGNABLE) && potentialJob.getAssignedToEntityId() == null) {

				if (potentialJob.getRequiredItemType() != null && !jobUsesWorkstationTool(potentialJob)) {
					if (!haveInventoryItem(potentialJob.getRequiredItemType(), potentialJob.getRequiredItemMaterial(), gameContext.getGameClock())) {
						Memory itemRequiredMemory = new Memory(MemoryType.LACKING_REQUIRED_ITEM, gameContext.getGameClock());
						itemRequiredMemory.setRelatedItemType(potentialJob.getRequiredItemType());
						itemRequiredMemory.setRelatedMaterial(potentialJob.getRequiredItemMaterial()); // Might be null
						parent.parentEntity.getComponent(MemoryComponent.class).addShortTerm(itemRequiredMemory, gameContext.getGameClock());
						continue;
					}
				}

				if (potentialJob.getType().isRequiresWeapon()) {
					WeaponSelectionComponent weaponSelectionComponent = parent.parentEntity.getOrCreateComponent(WeaponSelectionComponent.class);
					if (!hasSelectedWeaponAndAmmoInInventory(parent.parentEntity, weaponSelectionComponent.getSelectedWeapon(), gameContext)) {
						continue;
					}
				}

				selectedJob = potentialJob;
				selectedJob.setAssignedToEntityId(parent.parentEntity.getId());
				parent.setAssignedJob(selectedJob);
				parent.messageDispatcher.dispatchMessage(MessageType.JOB_ASSIGNMENT_ACCEPTED, selectedJob);
				completionType = SUCCESS;
				break;
			}
		}
		if (selectedJob == null) {
			// No jobs found
			completionType = FAILURE;
		}
	}

	private boolean haveInventoryItem(ItemType itemTypeRequired, GameMaterial requiredItemMaterial, GameClock gameClock) {
		InventoryComponent inventoryComponent = parent.parentEntity.getComponent(InventoryComponent.class);
		if (requiredItemMaterial != null) {
			return inventoryComponent.findByItemTypeAndMaterial(itemTypeRequired, requiredItemMaterial, gameClock) != null;
		} else {
			return inventoryComponent.findByItemType(itemTypeRequired, gameClock) != null;
		}
	}

	private boolean jobUsesWorkstationTool(Job job) {
		if (job.getCraftingRecipe() != null) {
			return job.getCraftingRecipe().getCraftingType().isUsesWorkstationTool();
		} else {
			return job.getType().isUsesWorkstationTool();
		}
	}

	@Override
	public String getDescriptionOverrideI18nKey() {
		return "ACTION.SELECT_JOB.DESCRIPTION";
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (jobRequest != null) {
			jobRequest.writeTo(savedGameStateHolder);
			asJson.put("jobRequest", jobRequest.getRequestId());
		}

		if (timeWaitingForJob > 0f) {
			asJson.put("timeWaitingForJob", timeWaitingForJob);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		Long jobRequestId = asJson.getLong("jobRequest");
		if (jobRequestId != null) {
			this.jobRequest = savedGameStateHolder.jobRequests.get(jobRequestId);
			if (this.jobRequest == null) {
				throw new InvalidSaveException("Could not find job request by ID " + jobRequestId);
			}
			this.jobRequest.setCallback(this);
			// jobRequest requesterId set in init()
		}

		this.timeWaitingForJob = asJson.getFloatValue("timeWaitingForJob");
	}
}
