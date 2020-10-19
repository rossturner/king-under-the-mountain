package technology.rocketjump.undermount.entities.ai.goap;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import static technology.rocketjump.undermount.entities.ai.goap.ScheduleCategory.ANY;

public class GoalQueue implements ChildPersistable {

	private Queue<QueuedGoal> priorityQueue = new PriorityQueue<>();

	public void add(QueuedGoal goalToQueue) {
		// Don't add if any existing goals of the same or higher priority
		if (priorityQueue.stream().anyMatch(queuedGoal ->
				queuedGoal.getGoal().goalId == goalToQueue.getGoal().goalId &&
						queuedGoal.getPriority().priorityRank >= goalToQueue.getPriority().priorityRank)) {
			return;
		}
		// Remove any of the same goal with a lower priority
		priorityQueue.removeIf(queuedGoal ->
				queuedGoal.getGoal().goalId == goalToQueue.getGoal().goalId &&
						queuedGoal.getPriority().priorityRank < goalToQueue.getPriority().priorityRank
		);

		priorityQueue.add(goalToQueue);
	}

	public QueuedGoal peekNextGoal(List<ScheduleCategory> applicableCategories) {
		for (QueuedGoal queuedGoal : priorityQueue) {
			if (applicableCategories.contains(queuedGoal.getCategory()) || queuedGoal.getCategory().equals(ANY)) {
				return queuedGoal;
			}
		}
		return null;
	}

	public QueuedGoal popNextGoal(List<ScheduleCategory> applicableCategories) {
		QueuedGoal next = peekNextGoal(applicableCategories);
		if (next != null) {
			priorityQueue.removeIf(goal -> goal.equals(next));
		}
		return next;
	}

	public void removeExpiredGoals(GameClock gameClock) {
		final double currentGameTime = gameClock.getCurrentGameTime();
		priorityQueue.removeIf(queuedGoal -> queuedGoal.getExpiryTime() < currentGameTime);
	}

	public int size() {
		return priorityQueue.size();
	}

	public void clear() {
		priorityQueue.clear();
	}

	public boolean isEmpty() {
		return priorityQueue.isEmpty();
	}

	@Override
	public String toString() {
		List<String> queuedGoalDescriptors = new ArrayList<>();
		for (QueuedGoal queuedGoal : priorityQueue) {
			queuedGoalDescriptors.add(queuedGoal.getGoal().name + ":" + queuedGoal.getCategory() + ":" + queuedGoal.getPriority());
		}
		return StringUtils.join(queuedGoalDescriptors, ", ");
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!priorityQueue.isEmpty()) {
			JSONArray queueJson = new JSONArray();
			for (QueuedGoal queuedGoal : priorityQueue) {
				JSONObject queuedGoalJson = new JSONObject(true);
				queuedGoal.writeTo(queuedGoalJson, savedGameStateHolder);
				queueJson.add(queuedGoalJson);
			}
			asJson.put("queue", queueJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONArray queueJson = asJson.getJSONArray("queue");
		if (queueJson != null) {
			for (int cursor = 0; cursor < queueJson.size(); cursor++) {
				JSONObject queuedGoalJson = queueJson.getJSONObject(cursor);
				QueuedGoal queuedGoal = new QueuedGoal();
				queuedGoal.readFrom(queuedGoalJson, savedGameStateHolder, relatedStores);
				priorityQueue.add(queuedGoal);
			}
		}
	}
}
