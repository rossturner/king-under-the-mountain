package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobState;

public class JobStateMessage {

	public final Job job;
	public final JobState newState;

	public JobStateMessage(Job job, JobState newState) {
		this.job = job;
		this.newState = newState;
	}
}
