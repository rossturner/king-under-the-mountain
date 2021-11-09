package technology.rocketjump.undermount.jobs;

import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.PotentialJob;

import java.util.Comparator;

import static technology.rocketjump.undermount.jobs.ProfessionDictionary.NULL_PROFESSION;

/**
 * This comparator sorts by priority, then having or not having a required profession, then distance
 */
public class PotentialJobSorter implements Comparator<PotentialJob> {

	@Override
	public int compare(PotentialJob o1, PotentialJob o2) {
		if (!o1.job.getJobPriority().equals(o2.job.getJobPriority())) {
			return o1.job.getJobPriority().compareTo(o2.job.getJobPriority());
		} else if (isNullProfession(o1.job) && !isNullProfession(o2.job)) {
			return 1;
		} else if (!isNullProfession(o1.job) && isNullProfession(o2.job)) {
			return -1;
		} else {
			return (int)((o1.distance - o2.distance) * 1000f);
		}
	}

	private boolean isNullProfession(Job job) {
		return job.getRequiredProfession() == null || job.getRequiredProfession().equals(NULL_PROFESSION);
	}

}
