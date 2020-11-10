package technology.rocketjump.undermount.jobs;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.jobs.model.JobState;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.JobRequestMessage;

import java.util.*;

/**
 * This class deals with dishing out jobs to entities requesting them
 */
@Singleton
public class JobRequestHandler implements Updatable, Telegraph, Disposable {

	private final MessageDispatcher messageDispatcher;
	private final JobStore jobStore;

	private GameContext gameContext;

	@Inject
	public JobRequestHandler(MessageDispatcher messageDispatcher, JobStore jobStore) {
		this.messageDispatcher = messageDispatcher;
		this.jobStore = jobStore;

		messageDispatcher.addListener(this, MessageType.JOB_REQUESTED);
	}

	@Override
	public void dispose() {
		messageDispatcher.removeListener(this, MessageType.JOB_REQUESTED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.JOB_REQUESTED: {
				JobRequestMessage jobRequestMessage = (JobRequestMessage) msg.extraInfo;
				gameContext.getJobRequestQueue().addLast(jobRequestMessage);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	@Override
	public void update(float deltaTime) {
		int outstandingRequests = gameContext.getJobRequestQueue().size();
		int requestsToProcess = Math.max(2, outstandingRequests / 2); // either 2 or half of outstanding requests

		for (int processed = 0; processed < requestsToProcess; processed++) {
			if (!gameContext.getJobRequestQueue().isEmpty()) {
				JobRequestMessage jobRequest = gameContext.getJobRequestQueue().pop();
				if (!jobRequest.isCancelled()) {
					handle(jobRequest);
				}
			}
		}
	}

	private boolean handle(JobRequestMessage jobRequestMessage) {
		ProfessionsComponent professionsComponent = jobRequestMessage.getRequestingEntity().getComponent(ProfessionsComponent.class);
		Vector2 entityWorldPosition = jobRequestMessage.getRequestingEntity().getLocationComponent().getWorldPosition();
		GridPoint2 requesterLocation = new GridPoint2((int)Math.floor(entityWorldPosition.x), (int)Math.floor(entityWorldPosition.y));

		Map<JobPriority, List<Job>> jobsByPriority = new EnumMap<>(JobPriority.class);

		for (ProfessionsComponent.QuantifiedProfession professionToFindJobFor : professionsComponent.getActiveProfessions()) {
			Collection<Job> byProfession = jobStore.getCollectionByState(JobState.ASSIGNABLE).getByProfession(professionToFindJobFor.getProfession()).values();
			if (byProfession.isEmpty()) {
				continue;
			}

			Map<Float, Job> jobsByDistance = new TreeMap<>();

			for (Job currentJob : byProfession) {
				if (currentJob.getAssignedToEntityId() == null) {
					float distanceToJob = currentJob.getJobLocation().dst2(requesterLocation);
					jobsByDistance.put(distanceToJob, currentJob);
				}
			}

			for (Job job : jobsByDistance.values()) {
				jobsByPriority.computeIfAbsent(job.getJobPriority(), a -> new ArrayList<>()).add(job);
			}
		}

		List<Job> potentialJobs = new ArrayList<>();
		for (JobPriority jobPriority : JobPriority.values()) {
			if (jobsByPriority.containsKey(jobPriority)) {
				potentialJobs.addAll(jobsByPriority.get(jobPriority));
			}
		}

		// FIXME Should maybe prioritise jobs that need equipment so they are worked on when a settler has the item,
		// rather than picking up the item and then going and working on something else
		jobRequestMessage.getCallback().jobCallback(potentialJobs, gameContext);
		return true;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

	@Override
	public boolean runWhilePaused() {
		return true;
	}
}
