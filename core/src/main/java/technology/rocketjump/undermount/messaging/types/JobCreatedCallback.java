package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.jobs.model.Job;

public interface JobCreatedCallback {

	public void jobCreated(Job job);

}
