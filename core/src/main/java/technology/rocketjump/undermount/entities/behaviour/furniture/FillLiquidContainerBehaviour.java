package technology.rocketjump.undermount.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.components.LiquidContainerComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.jobs.model.JobState;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.JobCreatedCallback;
import technology.rocketjump.undermount.messaging.types.RequestLiquidTransferMessage;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;

import static technology.rocketjump.undermount.jobs.ProfessionDictionary.NULL_PROFESSION;
import static technology.rocketjump.undermount.jobs.model.JobState.REMOVED;

/**
 * This furniture behaviour is used to create jobs which will fill the parent furniture's LiquidContainerComponent with the specified liquid
 */
public class FillLiquidContainerBehaviour extends FurnitureBehaviour implements Destructible, JobCreatedCallback, Prioritisable {

	protected List<Job> outstandingJobs = new ArrayList<>();

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		super.init(parentEntity, messageDispatcher, gameContext);
		LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);
		if (liquidContainerComponent == null) {
			Logger.error(this.getClass().getSimpleName() + " does not have a " + LiquidContainerComponent.class.getSimpleName());
		} else if (liquidContainerComponent.getTargetLiquidMaterial() == null) {
			Logger.error(this.getClass().getSimpleName() + " does not have a specified target liquid material");
		}
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		for (Job outstandingJob : outstandingJobs) {
			if (!outstandingJob.getJobState().equals(REMOVED)) {
				messageDispatcher.dispatchMessage(MessageType.JOB_CANCELLED, outstandingJob);
			}
		}
	}

	@Override
	public void setPriority(JobPriority jobPriority) {
		super.setPriority(jobPriority);
		for (Job job : outstandingJobs) {
			job.setJobPriority(jobPriority);
		}
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);
		LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);
		if (liquidContainerComponent != null && liquidContainerComponent.getTargetLiquidMaterial() != null) {
			clearCompletedJobs();
			// if num outstanding jobs + current liquid amount < max, try to create job to fill container
			if (liquidContainerComponent.getLiquidQuantity() + (outstandingJobs.size() * relatedContainerCapacity()) < liquidContainerComponent.getMaxLiquidCapacity() - 1f) {
				// Create new job
				messageDispatcher.dispatchMessage(MessageType.REQUEST_LIQUID_TRANSFER, new RequestLiquidTransferMessage(
						liquidContainerComponent.getTargetLiquidMaterial(), false, parentEntity,
						parentEntity.getLocationComponent().getWorldPosition(), relatedItemTypes.get(0), NULL_PROFESSION, priority, this));
			}
		}
	}

	private int relatedContainerCapacity() {
		return relatedContainerCapacity(relatedItemTypes.get(0));
	}

	public static int relatedContainerCapacity(ItemType itemType) {
		// FIXME this isn't particularly data-driven or safe
		List<String> args = itemType.getTags().get("LIQUID_CONTAINER");
		return Integer.valueOf(args.get(0));
	}

	private void clearCompletedJobs() {
		outstandingJobs.removeIf(job -> job.getJobState().equals(JobState.REMOVED));
	}

	@Override
	public void jobCreated(Job job) {
		if (job != null) {
			outstandingJobs.add(job);
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		if (!outstandingJobs.isEmpty()) {
			JSONArray jobsJson = new JSONArray();
			for (Job outstandingJob : outstandingJobs) {
				outstandingJob.writeTo(savedGameStateHolder);
				jobsJson.add(outstandingJob.getJobId());
			}
			asJson.put("jobs", jobsJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		JSONArray jobsJson = asJson.getJSONArray("jobs");
		if (jobsJson != null) {
			for (int cursor = 0; cursor < jobsJson.size(); cursor++) {
				Job job = savedGameStateHolder.jobs.get(jobsJson.getLong(cursor));
				if (job == null) {
					throw new InvalidSaveException("Could not find job with ID " + jobsJson.getLong(cursor));
				} else {
					outstandingJobs.add(job);
				}
			}
		}
	}
}
