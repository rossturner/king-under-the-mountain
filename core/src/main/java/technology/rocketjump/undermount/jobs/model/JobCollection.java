package technology.rocketjump.undermount.jobs.model;

import technology.rocketjump.undermount.jobs.ProfessionDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static technology.rocketjump.undermount.jobs.ProfessionDictionary.NULL_PROFESSION;

/**
 * This class stores all the jobs of a certain state, as well as being responsible for setting JobState
 */
public class JobCollection {

	private final JobState collectionJobState;
	private Map<Profession, Map<Long, Job>> byProfession = new ConcurrentHashMap<>();
	private Map<Long, Job> byId = new ConcurrentHashMap<>();

	private int iterationCursor = 0;
	private List<Job> iterableCollection = emptyList;

	public JobCollection(JobState jobState, ProfessionDictionary professionDictionary) {
		this.collectionJobState = jobState;

		for (Profession profession : professionDictionary.getAll()) {
			byProfession.put(profession, new ConcurrentHashMap<>());
		}
		byProfession.put(NULL_PROFESSION, new ConcurrentHashMap<>());
	}

	public Map<Long, Job> getByProfession(Profession profession) {
		return byProfession.get(profession);
	}

	private static final List<Job> emptyList = new ArrayList<>();

	public List<Job> getAll() {
		return new ArrayList<>(byId.values());
	}

	public Job next() {
		if (iterationCursor >= iterableCollection.size()) {
			if (byId.size() > 0) {
				iterableCollection = new ArrayList<>(byId.values());
			} else {
				iterableCollection = emptyList;
			}
			iterationCursor = 0;
		}
		if (iterableCollection.isEmpty()) {
			return null;
		} else {
			Job job = iterableCollection.get(iterationCursor);
			iterationCursor++;
			return job;
		}
	}

	public void add(Job job) {
		job.setJobState(collectionJobState); // This isn't technically required, but included here as a guard against bad programming
		byProfession.get(job.getRequiredProfession()).put(job.getJobId(), job);
		byId.put(job.getJobId(), job);
	}

	public void remove(Job jobToRemove) {
		byProfession.get(jobToRemove.getRequiredProfession()).remove(jobToRemove.getJobId());
		byId.remove(jobToRemove.getJobId());
	}

	public int size() {
		return byId.size();
	}
}
