package technology.rocketjump.undermount.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.jobs.model.JobState;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.TransformFurnitureMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.undermount.entities.behaviour.furniture.CraftingStationBehaviour.getAnyNavigableWorkspace;

public class TransformUponJobCompletionFurnitureBehaviour extends FurnitureBehaviour implements OnJobCompletion, Prioritisable {

	private Job jobToComplete;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		super.init(parentEntity, messageDispatcher, gameContext);
		if (relatedJobTypes.size() != 1) {
			Logger.error("Expecting 1 related job type for " + this.getClass().getSimpleName());
		}
		if (relatedFurnitureTypes.size() != 1) {
			Logger.error("Expecting 1 related furniture type for " + this.getClass().getSimpleName());
		}
	}

	@Override
	public void setPriority(JobPriority jobPriority) {
		super.setPriority(jobPriority);
		if (jobToComplete != null) {
			jobToComplete.setJobPriority(priority);
		}
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);

		if (jobToComplete != null && jobToComplete.getJobState().equals(JobState.REMOVED)) {
			jobToComplete = null;
		}

		if (jobToComplete == null) {
			FurnitureLayout.Workspace navigableWorkspace = getAnyNavigableWorkspace(parentEntity, gameContext.getAreaMap());
			if (navigableWorkspace != null) {
				jobToComplete = new Job(relatedJobTypes.get(0));
				jobToComplete.setJobPriority(priority);
				jobToComplete.setTargetId(parentEntity.getId());
				jobToComplete.setJobLocation(navigableWorkspace.getAccessedFrom());
				messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, jobToComplete);
			}
		}


	}

	@Override
	public void jobCompleted(GameContext gameContext) {
		messageDispatcher.dispatchMessage(MessageType.TRANSFORM_FURNITURE_TYPE, new TransformFurnitureMessage(parentEntity, relatedFurnitureTypes.get(0)));
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);
		if (jobToComplete != null) {
			asJson.put("jobToComplete", jobToComplete.getJobId());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		Long jobId = asJson.getLong("jobToComplete");
		if (jobId != null) {
			jobToComplete = savedGameStateHolder.jobs.get(jobId);
			if (jobToComplete == null) {
				throw new InvalidSaveException("Could not find job with ID " + jobId);
			}
		}
	}

}
