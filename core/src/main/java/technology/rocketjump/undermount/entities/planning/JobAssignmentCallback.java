package technology.rocketjump.undermount.entities.planning;

import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.Job;

import java.util.List;

public interface JobAssignmentCallback {

	void jobCallback(List<Job> potentialJobs, GameContext gameContext);

}
