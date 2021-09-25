package technology.rocketjump.undermount.mapping;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.constants.ConstantsRepo;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.jobs.JobTypeDictionary;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.underground.PipeConstructionState;
import technology.rocketjump.undermount.messaging.MessageType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Collectors;

import static technology.rocketjump.undermount.mapping.tile.underground.PipeConstructionState.*;

@Singleton
public class PipeConstructionManager implements GameContextAware {

	private final MessageDispatcher messageDispatcher;
	private GameContext gameContext;
	private final JobType constructPipingJobType;
	private final JobType deconstructPipingJobType;
	private final JobStore jobStore;

	@Inject
	public PipeConstructionManager(ConstantsRepo constantsRepo, JobTypeDictionary jobTypeDictionary,
								   MessageDispatcher messageDispatcher, JobStore jobStore) {
		this.messageDispatcher = messageDispatcher;
		this.jobStore = jobStore;

		constructPipingJobType = jobTypeDictionary.getByName(constantsRepo.getSettlementConstants().getConstructPipingJobType());
		if (constructPipingJobType == null) {
			throw new RuntimeException("Could not find job with name " + constantsRepo.getSettlementConstants().getConstructPipingJobType() + " from " + constantsRepo.getSettlementConstants().getClass().getSimpleName());
		}

		deconstructPipingJobType = jobTypeDictionary.getByName(constantsRepo.getSettlementConstants().getDeconstructPipingJobType());
		if (deconstructPipingJobType == null) {
			throw new RuntimeException("Could not find job with name " + constantsRepo.getSettlementConstants().getDeconstructPipingJobType() + " from " + constantsRepo.getSettlementConstants().getClass().getSimpleName());
		}
	}

	public void pipeConstructionAdded(MapTile mapTile) {
		switchState(mapTile, READY_FOR_CONSTRUCTION);
	}

	public void pipeConstructionRemoved(MapTile mapTile) {
		switchState(mapTile, NONE);
	}

	public void pipeDeconstructionAdded(MapTile mapTile) {
		switchState(mapTile, PENDING_DECONSTRUCTION);
	}

	public void pipeDeconstructionRemoved(MapTile mapTile) {
		switchState(mapTile, NONE);
	}

	private void switchState(MapTile mapTile, PipeConstructionState newState) {
		PipeConstructionState oldState = mapTile.getOrCreateUnderTile().getPipeConstructionState();
		if (oldState.equals(newState)) {
			return;
		}

		if (oldState.equals(READY_FOR_CONSTRUCTION) || oldState.equals(PENDING_DECONSTRUCTION)) {
			// cancel outstanding job
			jobStore.getJobsAtLocation(mapTile.getTilePosition())
					.stream()
					.filter(j -> j.getType().equals(constructPipingJobType) || j.getType().equals(deconstructPipingJobType))
					.collect(Collectors.toList())// avoids ConcurrentModificationException
					.forEach(job -> {
						messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, job);
					});
		}

		mapTile.getOrCreateUnderTile().setPipeConstructionState(newState);

		if (newState.equals(READY_FOR_CONSTRUCTION)) {
			Job constructPipingJob = new Job(constructPipingJobType);
			constructPipingJob.setJobLocation(mapTile.getTilePosition());
			messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, constructPipingJob);
		} else if (newState.equals(PENDING_DECONSTRUCTION)) {
			Job deconstructPipingJob = new Job(deconstructPipingJobType);
			deconstructPipingJob.setJobLocation(mapTile.getTilePosition());
			messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, deconstructPipingJob);
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

}
