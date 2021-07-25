package technology.rocketjump.undermount.entities.ai.goap.actions;

import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobState;
import technology.rocketjump.undermount.messaging.MessageType;

import java.util.List;

import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class RequeueSameJobTypeAction extends SelectJobAction {
	public RequeueSameJobTypeAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void jobCallback(List<Job> potentialJobs, GameContext gameContext) {
		Job currentJob = parent.getAssignedJob();
		if (currentJob == null) {
			completionType = FAILURE;
		}

		if (FAILURE.equals(completionType)) {
			return; // Don't accept any jobs when already failed
		}
		Job selectedJob = null;
		for (Job potentialJob : potentialJobs) {
			if (potentialJob.getJobState().equals(JobState.ASSIGNABLE) && potentialJob.getAssignedToEntityId() == null) {

				if (potentialJob.getType().equals(currentJob.getType())) {
					parent.messageDispatcher.dispatchMessage(MessageType.JOB_ASSIGNMENT_CANCELLED, currentJob);

					selectedJob = potentialJob;
					selectedJob.setAssignedToEntityId(parent.parentEntity.getId());
					parent.setAssignedJob(selectedJob);
					parent.messageDispatcher.dispatchMessage(MessageType.JOB_ASSIGNMENT_ACCEPTED, selectedJob);
					completionType = SUCCESS;
					break;
				}
			}
		}
		if (selectedJob == null) {
			// No jobs found
			completionType = FAILURE;
		}
	}
}
