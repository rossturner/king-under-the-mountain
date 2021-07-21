package technology.rocketjump.undermount.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.common.collect.Lists;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.jobs.model.JobState;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RequestHaulingAllocationMessage;
import technology.rocketjump.undermount.messaging.types.TransformFurnitureMessage;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rooms.HaulingAllocation;
import technology.rocketjump.undermount.ui.i18n.I18nString;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nWord;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.undermount.entities.behaviour.furniture.InnoculationLogBehaviour.InnoculationLogState.*;
import static technology.rocketjump.undermount.jobs.model.JobState.REMOVED;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.ui.i18n.I18nTranslator.oneDecimalFormat;

public class InnoculationLogBehaviour extends FurnitureBehaviour implements Destructible, SelectableDescription, Prioritisable {

	private JobType haulingJobType;
	private JobType innoculationJobType;
	private InnoculationLogState state = WAITING_TO_ASSIGN_MUSHROOM_SPAWN;
	private double lastUpdateGameTime = 0;
	private double hoursSinceInnoculation = 0.0;
	private double hoursToFullGrowth;
	private Job incomingHaulingJob = null;
	private Job innoculationJob = null;
	private FurnitureType transformToFurnitureType;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		super.init(parentEntity, messageDispatcher, gameContext);
		GameClock gameClock = gameContext.getGameClock();
		lastUpdateGameTime = gameClock.getCurrentGameTime();
		hoursToFullGrowth = gameClock.HOURS_IN_DAY * gameClock.DAYS_IN_SEASON * 4; // one year in game time
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		for (Job outstandingJob : Arrays.asList(incomingHaulingJob, innoculationJob)) {
			if (outstandingJob != null && !outstandingJob.getJobState().equals(REMOVED)) {
				messageDispatcher.dispatchMessage(MessageType.JOB_CANCELLED, outstandingJob);
			}
		}
	}

	@Override
	public void setPriority(JobPriority jobPriority) {
		super.setPriority(jobPriority);
		if (incomingHaulingJob != null) {
			incomingHaulingJob.setJobPriority(jobPriority);
		}
		if (innoculationJob != null) {
			innoculationJob.setJobPriority(jobPriority);
		}
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext) {
		Map<String, I18nString> replacements = new HashMap<>();
		float progress = 100f * estimatedProgressToInnoculation();
		replacements.put("progress", new I18nWord("progress", oneDecimalFormat.format(progress)));
		return Lists.newArrayList(i18nTranslator.getTranslatedWordWithReplacements(state.descriptionI18nKey, replacements));
	}

	private float estimatedProgressToInnoculation() {
		return Math.min((float)hoursSinceInnoculation / (float)hoursToFullGrowth, 1f);
	}

	public ItemType getRelatedItemType() {
		return relatedItemTypes.get(0);
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);

		if (parentEntity.isOnFire()) {
			if (incomingHaulingJob != null) {
				messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, incomingHaulingJob);
				incomingHaulingJob = null;
			}
			if (innoculationJob != null) {
				messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, innoculationJob);
				innoculationJob = null;
			}
			return;
		}

		switch (state) {
			case WAITING_TO_ASSIGN_MUSHROOM_SPAWN: {
				// Try to requestAllocation mushroom spawn to this item
				messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION, new RequestHaulingAllocationMessage(
						parentEntity, parentEntity.getLocationComponent().getWorldOrParentPosition(), getRelatedItemType(), null,
						false, 1, null, allocation -> {
					if (allocation != null) {
						finaliseAllocation(allocation);
					}
				}
				));

				break;
			}
			case MUSHROOM_SPAWN_ASSIGNED: {
				if (incomingHaulingJob.getJobState().equals(JobState.REMOVED)) {
					incomingHaulingJob = null;
					this.state = WAITING_TO_ASSIGN_MUSHROOM_SPAWN;
				}

				// Also check for successful completion
				InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);
				if (!inventoryComponent.isEmpty()) {
					for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
						ItemEntityAttributes attributes = (ItemEntityAttributes) inventoryEntry.entity.getPhysicalEntityComponent().getAttributes();
						if (attributes.getItemType().equals(relatedItemTypes.get(0))) {
							createInnoculationJob();
						}
					}
				}
				break;
			}
			case WAITING_FOR_INNOCULATION_JOB: {
				// Job completion will trigger change to next state

				if (innoculationJob.getJobState().equals(JobState.REMOVED)) {
					innoculationJob = null;
					createInnoculationJob();
				}
				break;
			}
			case INNOCULATING: {
				innoculationJob = null;
				hoursSinceInnoculation += (gameContext.getGameClock().getCurrentGameTime() - lastUpdateGameTime);
				if (hoursSinceInnoculation > hoursToFullGrowth) {
					messageDispatcher.dispatchMessage(MessageType.TRANSFORM_FURNITURE_TYPE, new TransformFurnitureMessage(parentEntity, transformToFurnitureType));
					state = INNOCULATION_COMPLETE;
				}
				break;
			}
			case INNOCULATION_COMPLETE: {
				// Nothing to do
				break;
			}
		}


		lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
	}

	public void setState(InnoculationLogState state) {
		this.state = state;
	}

	public void finaliseAllocation(HaulingAllocation allocation) {
		// Create hauling job to haul assignment into inventory
		allocation.setTargetPositionType(HaulingAllocation.AllocationPositionType.FURNITURE);
		allocation.setTargetId(parentEntity.getId());
		allocation.setTargetPosition(toGridPoint(parentEntity.getLocationComponent().getWorldPosition()));

		Job haulingJob = new Job(haulingJobType);
		haulingJob.setJobPriority(priority);
		haulingJob.setTargetId(allocation.getHauledEntityId());
		haulingJob.setHaulingAllocation(allocation);
		haulingJob.setJobLocation(allocation.getSourcePosition());
		haulingJob.setRequiredProfession(innoculationJobType.getRequiredProfession());

		incomingHaulingJob = haulingJob;
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJob);

		state = MUSHROOM_SPAWN_ASSIGNED;
	}

	private void createInnoculationJob() {
		innoculationJob = new Job(innoculationJobType);
		innoculationJob.setJobPriority(priority);
		innoculationJob.setTargetId(parentEntity.getId());
		innoculationJob.setJobLocation(toGridPoint(parentEntity.getLocationComponent().getWorldPosition()));
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, innoculationJob);

		this.state = WAITING_FOR_INNOCULATION_JOB;
	}

	public InnoculationLogState getState() {
		return state;
	}

	public void setHaulingJobType(JobType haulingJobType) {
		this.haulingJobType = haulingJobType;
	}

	public void setInnoculationJobType(JobType innoculationJobType) {
		this.innoculationJobType = innoculationJobType;
	}

	public void setTransformToFurnitureType(FurnitureType transformToFurnitureType) {
		this.transformToFurnitureType = transformToFurnitureType;
	}

	public FurnitureType getTransformToFurnitureType() {
		return transformToFurnitureType;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);
		asJson.put("haulingJobType", haulingJobType.getName());
		asJson.put("innoculationJobType", innoculationJobType.getName());
		asJson.put("transformToFurnitureType", transformToFurnitureType.getName());
		if (!WAITING_TO_ASSIGN_MUSHROOM_SPAWN.equals(state)) {
			asJson.put("state", state.name());
		}
		asJson.put("lastUpdate", lastUpdateGameTime);
		if (hoursSinceInnoculation > 0) {
			asJson.put("hoursSinceInnoculation", hoursSinceInnoculation);
		}
		if (hoursToFullGrowth > 0) {
			asJson.put("hoursToFullGrowth", hoursToFullGrowth);
		}

		if (incomingHaulingJob != null) {
			incomingHaulingJob.writeTo(savedGameStateHolder);
			asJson.put("incomingHaulingJob", incomingHaulingJob.getJobId());
		}
		if (innoculationJob != null) {
			innoculationJob.writeTo(savedGameStateHolder);
			asJson.put("innoculationJob", innoculationJob.getJobId());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.haulingJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("haulingJobType"));
		if (haulingJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("haulingJobType"));
		}
		this.innoculationJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("innoculationJobType"));
		if (innoculationJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("innoculationJobType"));
		}
		this.transformToFurnitureType = relatedStores.furnitureTypeDictionary.getByName(asJson.getString("transformToFurnitureType"));
		if (this.transformToFurnitureType == null) {
			throw new InvalidSaveException("Could not find furniture type with name " + asJson.getString("transformToFurnitureType"));
		}
		this.state = EnumParser.getEnumValue(asJson, "state", InnoculationLogState.class, WAITING_TO_ASSIGN_MUSHROOM_SPAWN);
		this.lastUpdateGameTime = asJson.getDoubleValue("lastUpdate");
		this.hoursSinceInnoculation = asJson.getDoubleValue("hoursSinceInnoculation");
		this.hoursToFullGrowth = asJson.getDoubleValue("hoursToFullGrowth");

		Long incomingHaulingJobId = asJson.getLong("incomingHaulingJob");
		if (incomingHaulingJobId != null) {
			this.incomingHaulingJob = savedGameStateHolder.jobs.get(incomingHaulingJobId);
			if (this.incomingHaulingJob == null) {
				throw new InvalidSaveException("Could not find job with ID " + incomingHaulingJobId + " for " + this.getClass().getSimpleName());
			}
		}

		Long innoculationJobId = asJson.getLong("innoculationJob");
		if (innoculationJobId != null) {
			this.innoculationJob = savedGameStateHolder.jobs.get(innoculationJobId);
			if (this.innoculationJob == null) {
				throw new InvalidSaveException("Could not find job with ID " + innoculationJobId + " for " + this.getClass().getSimpleName());
			}
		}
	}

	public enum InnoculationLogState {

		WAITING_TO_ASSIGN_MUSHROOM_SPAWN("FURNITURE.DESCRIPTION.WAITING_FOR_MUSHROOM_SPAWN"),
		MUSHROOM_SPAWN_ASSIGNED("FURNITURE.DESCRIPTION.WAITING_FOR_MUSHROOM_SPAWN"),
		WAITING_FOR_INNOCULATION_JOB("FURNITURE.DESCRIPTION.WAITING_FOR_MUSHROOM_SPAWN"),
		INNOCULATING("FURNITURE.DESCRIPTION.MUSHROOM_INNOCULATION_IN_PROGRESS"),
		INNOCULATION_COMPLETE("FURNITURE.DESCRIPTION.MUSHROOM_INNOCULATION_IN_PROGRESS");

		public final String descriptionI18nKey;

		InnoculationLogState(String descriptionI18nKey) {
			this.descriptionI18nKey = descriptionI18nKey;
		}
	}
}
