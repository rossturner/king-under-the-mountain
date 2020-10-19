package technology.rocketjump.undermount.entities.ai.goap;

public enum GoalPriority {

	LOWEST(1),
	WANT_NORMAL(2),
	WANT_URGENT(3),
	JOB_NORMAL(4),
	JOB_URGENT(5),
	NEED_NORMAL(6),
	NEED_URGENT(7),
	HIGHEST(8);

	public final int priorityRank;

	GoalPriority(int priority) {
		this.priorityRank = priority;
	}
}
