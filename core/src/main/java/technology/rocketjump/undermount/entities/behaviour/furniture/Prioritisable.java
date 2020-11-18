package technology.rocketjump.undermount.entities.behaviour.furniture;

import technology.rocketjump.undermount.jobs.model.JobPriority;

public interface Prioritisable {

	JobPriority getPriority();

	void setPriority(JobPriority jobPriority);

}
