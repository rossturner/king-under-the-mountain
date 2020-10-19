package technology.rocketjump.undermount.entities.ai.goap;

import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.ai.goap.actions.Action;
import technology.rocketjump.undermount.entities.ai.goap.actions.ActionTransitions;
import technology.rocketjump.undermount.misc.Name;

import java.util.*;

/**
 * This class describes a series of Actions to accomplish a goal
 * E.g. eating, sleeping, working on a job
 */
public class Goal {

	@Name
	public final String name;
	public final String i18nDescription;
	public final Double expiryHours;
	public final long goalId;

	private List<GoalSelector> selectors = new LinkedList<>();

	private final List<Class<? extends Action>> initialActions = new ArrayList<>();
	private final Map<Class<? extends Action>, ActionTransitions> actionTransitionsMap = new HashMap<>();

	public Goal(String name, String i18nDescription, Double expiryHours) {
		this.name = name;
		this.i18nDescription = i18nDescription;
		this.expiryHours = expiryHours;
		this.goalId = SequentialIdGenerator.nextId();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Goal goal = (Goal) o;
		return goalId == goal.goalId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(goalId);
	}

	@Override
	public String toString() {
		return "Goal{" + name + ", goalId=" + goalId + '}';
	}

	public List<GoalSelector> getSelectors() {
		return selectors;
	}

	public void setSelectors(List<GoalSelector> selectors) {
		this.selectors = selectors;
	}

	public void add(Class<? extends Action> action, ActionTransitions transitions) {
		actionTransitionsMap.put(action, transitions);
	}

	public ActionTransitions getTransitions(Class<? extends Action> actionClass) {
		return actionTransitionsMap.get(actionClass);
	}

	public void addInitialAction(Class<? extends Action> initialAction) {
		initialActions.add(initialAction);
	}

	public Map<Class<? extends Action>, ActionTransitions> getAllTransitions() {
		return actionTransitionsMap;
	}

	public List<Class<? extends Action>> getInitialActions() {
		return initialActions;
	}
}
