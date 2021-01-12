package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.undermount.entities.model.physical.humanoid.EquippedItemComponent;
import technology.rocketjump.undermount.entities.tags.ItemUsageSoundTag;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.*;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.Optional;

import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;

public class WorkOnJobAction extends Action {

	private boolean activeSoundTriggered;

	public WorkOnJobAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		// May have already been interrupted
		if (completionType != null && completionType.equals(FAILURE)) {
			return;
		}

		if (inPositionToWorkOnJob()) {
			Job assignedJob = parent.getAssignedJob();
			float skillLevel = 0f;
			ProfessionsComponent professionsComponent = parent.parentEntity.getComponent(ProfessionsComponent.class);
			if (professionsComponent != null) {
				skillLevel = professionsComponent.getSkillLevel(assignedJob.getRequiredProfession());
			}
			float timeModifier = Math.min(1f, skillLevel * 1.5f);
			float workDone = deltaTime * timeModifier;
			assignedJob.applyWorkDone(workDone);
			if (!activeSoundTriggered) {
				SoundAsset jobSoundAsset = getJobSoundAsset();
				if (jobSoundAsset != null) {
					parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(jobSoundAsset, parent.parentEntity.getId(), parent.parentEntity.getLocationComponent().getWorldOrParentPosition()));
				}
				activeSoundTriggered = true;
			}

			if (assignedJob.getType().getWorkOnJobParticleEffectType() != null) {
				if (spawnedParticles.stream()
						.filter(p -> p.getType().equals(assignedJob.getType().getWorkOnJobParticleEffectType()))
						.findAny().isEmpty()) {
					Action This = this;
					parent.messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(
							assignedJob.getType().getWorkOnJobParticleEffectType(),
							Optional.of(parent.parentEntity),
							Optional.ofNullable(assignedJob.getTargetOfJob(gameContext)
							), instance -> {
						if (instance != null) {
							This.spawnedParticles.add(instance);
						}
					}));
				}
			}

			float workCompletionFraction = Math.min(assignedJob.getWorkDoneSoFar() / assignedJob.getTotalWorkToDo(), 1f);

			for (ParticleEffectInstance spawnedParticle : spawnedParticles) {
				parent.messageDispatcher.dispatchMessage(MessageType.PARTICLE_UPDATE, new ParticleUpdateMessage(spawnedParticle, workCompletionFraction));
			}


			if (assignedJob.getTotalWorkToDo() <= assignedJob.getWorkDoneSoFar()) {
				parent.messageDispatcher.dispatchMessage(MessageType.JOB_COMPLETED, new JobCompletedMessage(assignedJob, professionsComponent, parent.parentEntity));
				completionType = SUCCESS;
			}

		} else {
			completionType = FAILURE;
		}

		if (completionType != null) {
			// finished
			if (activeSoundTriggered) {
				SoundAsset jobSoundAsset = getJobSoundAsset();
				if (jobSoundAsset != null && jobSoundAsset.isLooping()) {
					parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_STOP_SOUND_LOOP, new RequestSoundStopMessage(jobSoundAsset, parent.parentEntity.getId()));
				}
			}
		}
	}

	public SoundAsset getJobSoundAsset() {
		Job assignedJob = parent.getAssignedJob();
		EquippedItemComponent equippedItemComponent = parent.parentEntity.getComponent(EquippedItemComponent.class);
		ItemUsageSoundTag itemUsageSoundTag = null;
		if (equippedItemComponent != null && equippedItemComponent.getEquippedItem() != null) {
			itemUsageSoundTag = equippedItemComponent.getEquippedItem().getTag(ItemUsageSoundTag.class);
		}

		if (itemUsageSoundTag != null && itemUsageSoundTag.getSoundAsset() != null) {
			return itemUsageSoundTag.getSoundAsset();
		} else if (assignedJob.getCookingRecipe() != null && assignedJob.getCookingRecipe().getActiveSoundAsset() != null) {
			return assignedJob.getCookingRecipe().getActiveSoundAsset();
		} else if (assignedJob.getType().getActiveSoundAsset() != null) {
			return assignedJob.getType().getActiveSoundAsset();
		} else {
			return null;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (activeSoundTriggered) {
			asJson.put("soundTriggered", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.activeSoundTriggered = asJson.getBooleanValue("soundTriggered");
	}

	private boolean inPositionToWorkOnJob() {
		if (parent.getAssignedJob() == null) {
			return false;
		} else if (parent.getAssignedJob().getType().isAccessedFromAdjacentTile()) {
			// Tile distance must be one
			return toGridPoint(parent.parentEntity.getLocationComponent().getWorldPosition()).dst2(parent.getAssignedJob().getJobLocation()) <= 1f;
		} else {
			return toGridPoint(parent.parentEntity.getLocationComponent().getWorldPosition()).equals(parent.getAssignedJob().getJobLocation());
		}
	}
}
