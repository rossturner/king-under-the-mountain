package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent;
import technology.rocketjump.undermount.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.humanoid.EquippedItemComponent;
import technology.rocketjump.undermount.entities.tags.ItemUsageSoundTag;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.*;
import technology.rocketjump.undermount.particles.custom_libgdx.ProgressBarEffect;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent.HappinessModifier.WORKED_IN_ENCLOSED_ROOM;
import static technology.rocketjump.undermount.entities.model.EntityType.FURNITURE;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;

public class WorkOnJobAction extends Action {

	private boolean activeSoundTriggered;
	private boolean furnitureInUseNotified;

	public WorkOnJobAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		Optional<Entity> targetFurniture = getTargetFurniture(parent.getAssignedJob(), gameContext);

		if (completionType == null && inPositionToWorkOnJob()) {
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

			if (!furnitureInUseNotified) {
				if (targetFurniture.isPresent()) {
					furnitureInUseNotified = true;
					parent.messageDispatcher.dispatchMessage(MessageType.FURNITURE_IN_USE, targetFurniture.get());
				}
			}

			MapTile currentTile = gameContext.getAreaMap().getTile(parent.parentEntity.getLocationComponent().getWorldPosition());
			if (currentTile != null && currentTile.hasRoom() && currentTile.getRoomTile().getRoom().isFullyEnclosed()) {
				HappinessComponent happinessComponent = parent.parentEntity.getComponent(HappinessComponent.class);
				if (happinessComponent != null) {
					happinessComponent.add(WORKED_IN_ENCLOSED_ROOM);
				}
			}

			Action This = this;

			parent.messageDispatcher.dispatchMessage(MessageType.GET_PROGRESS_BAR_EFFECT_TYPE, (ParticleEffectTypeCallback) progressBarType -> {
				if (spawnedParticles.stream().noneMatch(p -> p.getType().equals(progressBarType))) {
					parent.messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(
							progressBarType,
							Optional.of(parent.parentEntity),
							Optional.empty(),
							instance -> {
						This.spawnedParticles.add(instance);
					}));
				}
			});

			List<ParticleEffectType> relatedParticleEffectTypes = getRelatedParticleEffectTypes();
			if (relatedParticleEffectTypes != null) {
				for (ParticleEffectType particleEffectType : relatedParticleEffectTypes) {
					if (spawnedParticles.stream()
							.noneMatch(p -> p.getType().equals(particleEffectType))) {
						parent.messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(
								particleEffectType,
								Optional.of(parent.parentEntity),
								Optional.ofNullable(assignedJob.getTargetOfJob(gameContext)
								), instance -> {
							This.spawnedParticles.add(instance);
						}));
					}
				}
			}

			float workCompletionFraction = Math.min(assignedJob.getWorkDoneSoFar() / assignedJob.getTotalWorkToDo(), 1f);

			spawnedParticles.removeIf(p -> p == null || !p.isActive());

			Iterator<ParticleEffectInstance> particleIterator = spawnedParticles.iterator();
			while (particleIterator.hasNext()) {
				ParticleEffectInstance spawnedParticle = particleIterator.next();
				if (spawnedParticle.getWrappedInstance() instanceof ProgressBarEffect) {
					((ProgressBarEffect)spawnedParticle.getWrappedInstance()).setProgress(workCompletionFraction);
				}
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

			if (furnitureInUseNotified && targetFurniture.isPresent()) {
				parent.messageDispatcher.dispatchMessage(MessageType.FURNITURE_NO_LONGER_IN_USE, targetFurniture.get());
			}

			if (activeSoundTriggered) {
				SoundAsset jobSoundAsset = getJobSoundAsset();
				if (jobSoundAsset != null && jobSoundAsset.isLooping()) {
					parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_STOP_SOUND_LOOP, new RequestSoundStopMessage(jobSoundAsset, parent.parentEntity.getId()));
				}
			}
		}
	}

	@Override
	public void actionInterrupted(GameContext gameContext) {
		if (furnitureInUseNotified) {

		}
		completionType = CompletionType.FAILURE;
	}

	private Optional<Entity> getTargetFurniture(Job assignedJob, GameContext gameContext) {
		if (assignedJob.getTargetId() != null) {
			Entity targetEntity = gameContext.getEntities().get(assignedJob.getTargetId());
			if (targetEntity != null && targetEntity.getType().equals(FURNITURE)) {
				return Optional.of(targetEntity);
			}
		}
		return Optional.empty();
	}

	private List<ParticleEffectType> getRelatedParticleEffectTypes() {
		if (parent.getAssignedJob().getCraftingRecipe() != null) {
			return parent.getAssignedJob().getCraftingRecipe().getCraftingType().getParticleEffectTypes();
		} else {
			return parent.getAssignedJob().getType().getWorkOnJobParticleEffectTypes();
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
		if (furnitureInUseNotified) {
			asJson.put("furnitureInUseNotified", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.activeSoundTriggered = asJson.getBooleanValue("soundTriggered");
		this.furnitureInUseNotified = asJson.getBooleanValue("furnitureInUseNotified");
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
