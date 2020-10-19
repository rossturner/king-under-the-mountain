package technology.rocketjump.undermount.entities.ai.goap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * This class determines when a goal should be added to an entity's GoalQueue
 */
public class GoalSelector {

	public final List<GoalSelectionCondition> conditions;
	public final GoalPriority priority;
	public final ScheduleCategory scheduleCategory;

	@JsonCreator
	public GoalSelector(
			@JsonProperty("conditions") List<GoalSelectionCondition> conditions,
			@JsonProperty("priority") GoalPriority priority,
			@JsonProperty("scheduleCategory") ScheduleCategory scheduleCategory) {
		this.conditions = conditions;
		this.priority = priority;
		this.scheduleCategory = scheduleCategory;
	}
}
