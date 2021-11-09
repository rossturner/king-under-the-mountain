package technology.rocketjump.undermount.jobs.model;

/**
 * This class is used to sort jobs by priority, then profession or not profession, then distance,
 * so further away job with a profession should be selected before a nearer job without a profession, of the same priority
 */
public class PotentialJob {

	public final Job job;
	public final float distance;

	public PotentialJob(Job job, float distance) {
		this.job = job;
		this.distance = distance;
	}
}
