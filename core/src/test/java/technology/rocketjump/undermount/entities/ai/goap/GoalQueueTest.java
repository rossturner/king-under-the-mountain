package technology.rocketjump.undermount.entities.ai.goap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.environment.GameClock;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static technology.rocketjump.undermount.entities.ai.goap.GoalPriority.*;
import static technology.rocketjump.undermount.entities.ai.goap.ScheduleCategory.NOURISHMENT;
import static technology.rocketjump.undermount.entities.ai.goap.ScheduleCategory.WORK;

@RunWith(MockitoJUnitRunner.class)
public class GoalQueueTest {

	private GoalQueue goalQueue;
	@Mock
	private Goal mockGoal;
	@Mock
	private GameClock mockClock;

	@Before
	public void setUp() throws Exception {
		goalQueue = new GoalQueue();
	}

	@Test
	public void add_replacesLowerPriorityGoals() throws Exception {
		QueuedGoal lowerPriority = new QueuedGoal(mockGoal, NOURISHMENT, WANT_NORMAL, mockClock);
		QueuedGoal higherPriority = new QueuedGoal(mockGoal, NOURISHMENT, NEED_URGENT, mockClock);

		goalQueue.add(lowerPriority);
		goalQueue.add(higherPriority);

		assertThat(goalQueue.size()).isEqualTo(1);
		assertThat(goalQueue.peekNextGoal(asList(NOURISHMENT)).getGoal()).isEqualTo(mockGoal);
		assertThat(goalQueue.peekNextGoal(asList(NOURISHMENT)).getPriority()).isEqualTo(NEED_URGENT);
	}

	@Test
	public void add_ignoresLowerPriority() {
		QueuedGoal lowerPriority = new QueuedGoal(mockGoal, NOURISHMENT, WANT_NORMAL, mockClock);
		QueuedGoal higherPriority = new QueuedGoal(mockGoal, NOURISHMENT, NEED_URGENT, mockClock);

		goalQueue.add(higherPriority);
		goalQueue.add(lowerPriority);

		assertThat(goalQueue.size()).isEqualTo(1);
		assertThat(goalQueue.peekNextGoal(asList(NOURISHMENT)).getGoal()).isEqualTo(mockGoal);
		assertThat(goalQueue.peekNextGoal(asList(NOURISHMENT)).getPriority()).isEqualTo(NEED_URGENT);

		QueuedGoal result = goalQueue.popNextGoal(asList(WORK));

		assertThat(result).isNull();
		assertThat(goalQueue.size()).isEqualTo(1);

		result = goalQueue.popNextGoal(asList(NOURISHMENT));

		assertThat(result).isEqualTo(higherPriority);
		assertThat(goalQueue.size()).isEqualTo(0);
	}

	@Test
	public void popNextGoal_returnsPriorityOrder() {
		goalQueue.add(new QueuedGoal(new Goal("first", "i18nDescription", null, false, false), NOURISHMENT, WANT_NORMAL, mockClock));
		goalQueue.add(new QueuedGoal(new Goal("other", "i18nDescription", null, false, false), NOURISHMENT, WANT_URGENT, mockClock));
		goalQueue.add(new QueuedGoal(new Goal("yet another", "i18nDescription", null, false, false), NOURISHMENT, WANT_NORMAL, mockClock));

		QueuedGoal result = goalQueue.popNextGoal(asList(NOURISHMENT));

		assertThat(result.getGoal().name).isEqualTo("other");
		assertThat(goalQueue.size()).isEqualTo(2);
	}

	@Test
	public void goalsWithSamePriority_returnedInInsertionOrder() {
		Goal first = new Goal("first", null, null, false, false);
		Goal third = new Goal("third", null, null, false, false);
		Goal second = new Goal("second", null, null, false, false);

		goalQueue.add(new QueuedGoal(first, ScheduleCategory.WORK, JOB_NORMAL, mockClock));
		goalQueue.add(new QueuedGoal(second, ScheduleCategory.WORK, JOB_NORMAL, mockClock));
		goalQueue.add(new QueuedGoal(third, ScheduleCategory.WORK, JOB_NORMAL, mockClock));

		QueuedGoal retrievedFirst = goalQueue.popNextGoal(asList(WORK));
		QueuedGoal retrievedSecond = goalQueue.popNextGoal(asList(WORK));
		QueuedGoal retrievedThird = goalQueue.popNextGoal(asList(WORK));

		assertThat(retrievedFirst.getGoal()).isEqualTo(first);
		assertThat(retrievedSecond.getGoal()).isEqualTo(second);
		assertThat(retrievedThird.getGoal()).isEqualTo(third);
	}

	@Test
	public void goalsWithExpiredTime_areRemoved() {
		when(mockClock.getCurrentGameTime()).thenReturn(10.0);

		Goal first = new Goal("first", null, 1.0, false, false);
		Goal second = new Goal("second", null, null, false, false);

		goalQueue.add(new QueuedGoal(first, ScheduleCategory.WORK, JOB_NORMAL, mockClock));
		goalQueue.add(new QueuedGoal(second, ScheduleCategory.WORK, JOB_NORMAL, mockClock));

		goalQueue.removeExpiredGoals(mockClock);
		assertThat(goalQueue.size()).isEqualTo(2);

		when(mockClock.getCurrentGameTime()).thenReturn(10.5);

		goalQueue.removeExpiredGoals(mockClock);
		assertThat(goalQueue.size()).isEqualTo(2);

		when(mockClock.getCurrentGameTime()).thenReturn(11.5);

		goalQueue.removeExpiredGoals(mockClock);
		assertThat(goalQueue.size()).isEqualTo(1);
		assertThat(goalQueue.peekNextGoal(asList(WORK)).getGoal()).isEqualTo(second);
	}

}