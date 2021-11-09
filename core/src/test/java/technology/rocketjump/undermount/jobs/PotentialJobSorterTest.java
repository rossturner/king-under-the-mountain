package technology.rocketjump.undermount.jobs;

import org.fest.assertions.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.jobs.model.*;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;
import static technology.rocketjump.undermount.jobs.ProfessionDictionary.NULL_PROFESSION;

@RunWith(MockitoJUnitRunner.class)
public class PotentialJobSorterTest {

	@Mock
	private JobType noProfessionJobType;
	@Mock
	private JobType requiredProfessionJobType;
	@Mock
	private Profession mockProfession;

	private PotentialJobSorter potentialJobSorter;

	@Before
	public void setup() {
		potentialJobSorter = new PotentialJobSorter();

		when(requiredProfessionJobType.getRequiredProfession()).thenReturn(mockProfession);
		when(noProfessionJobType.getRequiredProfession()).thenReturn(NULL_PROFESSION);
	}

	@Test
	public void sortsJobsByHighestPriorityFirst() {
		List<PotentialJob> potentialJobs = new ArrayList<>(List.of(
				priority(JobPriority.NORMAL),
				priority(JobPriority.HIGHEST),
				priority(JobPriority.LOWER)
		));

		potentialJobs.sort(potentialJobSorter);

		Assertions.assertThat(potentialJobs.get(0).job.getJobPriority().equals(JobPriority.HIGHEST));
		Assertions.assertThat(potentialJobs.get(1).job.getJobPriority().equals(JobPriority.NORMAL));
		Assertions.assertThat(potentialJobs.get(2).job.getJobPriority().equals(JobPriority.LOWER));
	}

	@Test
	public void sortsJobsByHavingProfessionOverDistance() {
		List<PotentialJob> potentialJobs = new ArrayList<>(List.of(
				job(noProfessionJobType, 1f),
				job(requiredProfessionJobType, 3f),
				priority(JobPriority.HIGHER),
				job(noProfessionJobType, 2f)
		));


		potentialJobs.sort(potentialJobSorter);

		Assertions.assertThat(potentialJobs.get(0).job.getJobPriority().equals(JobPriority.HIGHER));
		Assertions.assertThat(potentialJobs.get(1).job.getType().equals(requiredProfessionJobType));
		Assertions.assertThat(potentialJobs.get(2).job.getType().equals(noProfessionJobType));
		Assertions.assertThat(potentialJobs.get(3).job.getType().equals(noProfessionJobType));

		Assertions.assertThat(potentialJobs.get(2).distance).isEqualTo(1f);
		Assertions.assertThat(potentialJobs.get(3).distance).isEqualTo(2f);
	}

	private PotentialJob priority(JobPriority jobPriority) {
		Job job = new Job(noProfessionJobType);
		return new PotentialJob(job, 1f);
	}

	private PotentialJob job(JobType jobType, float distance) {
		return new PotentialJob(new Job(jobType), distance);
	}

}