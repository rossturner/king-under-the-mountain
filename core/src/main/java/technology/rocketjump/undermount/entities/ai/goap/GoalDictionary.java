package technology.rocketjump.undermount.entities.ai.goap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.undermount.entities.ai.goap.actions.Action;
import technology.rocketjump.undermount.entities.ai.goap.actions.ActionDictionary;
import technology.rocketjump.undermount.entities.ai.goap.actions.ActionTransitions;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class GoalDictionary {

	private final ActionDictionary actionDictionary;

	private final List<Goal> allGoals = new LinkedList<>();
	private final Map<String, Goal> byName = new HashMap<>();

	@Inject
	public GoalDictionary(ActionDictionary actionDictionary) throws IOException {
		this.actionDictionary = actionDictionary;
		File goalsFile = new File("assets/ai/goals.json");
		JSONArray goalArray = JSON.parseArray(FileUtils.readFileToString(goalsFile));

		for (int cursor = 0; cursor < goalArray.size(); cursor++) {
			JSONObject goalJson = goalArray.getJSONObject(cursor);
			Goal goal = parseGoal(goalJson);

			verify(goal);

			allGoals.add(goal);
			byName.put(goal.name, goal);
		}

		for (SpecialGoal specialGoal : SpecialGoal.values()) {
			Goal instance = byName.get(specialGoal.goalName);
			if (instance == null) {
				throw new RuntimeException("Could not find goal with name: " + specialGoal.goalName);
			}
			specialGoal.goalInstance = instance;
		}
	}

	public List<Goal> getAllGoals() {
		return allGoals;
	}

	Goal getByName(String name) {
		return byName.get(name);
	}

	private ObjectMapper objectMapper = new ObjectMapper();

	private Goal parseGoal(JSONObject goalJson) throws IOException {
		Boolean interruptedByCombat = goalJson.getBoolean("interruptedByCombat");
		if (interruptedByCombat == null) {
			interruptedByCombat = true;
		}
		Goal goal = new Goal(goalJson.getString("name"), goalJson.getString("i18nDescription"), goalJson.getDouble("expiryHours"), interruptedByCombat);

		List<GoalSelector> selectors = objectMapper.readValue(goalJson.getJSONArray("selectors").toJSONString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, GoalSelector.class));
		goal.setSelectors(selectors);

		JSONObject relationshipsJson = goalJson.getJSONObject("actionRelationships");
		for (String actionName : relationshipsJson.keySet()) {
			Class<? extends Action> actionClass = actionDictionary.getByName(actionName);

			ActionTransitions transitions = new ActionTransitions();

			JSONObject transitionsJson = relationshipsJson.getJSONObject(actionName);

			if (transitionsJson.containsKey("pass")) {
				JSONArray actionNames = transitionsJson.getJSONArray("pass");
				for (int cursor = 0; cursor < actionNames.size(); cursor++) {
					Class<? extends Action> successAction = actionDictionary.getByName(actionNames.getString(cursor));
					transitions.onSuccess.add(successAction);
				}
			}
			if (transitionsJson.containsKey("fail")) {
				JSONArray actionNames = transitionsJson.getJSONArray("fail");
				for (int cursor = 0; cursor < actionNames.size(); cursor++) {
					Class<? extends Action> failAction = actionDictionary.getByName(actionNames.getString(cursor));
					transitions.onFailure.add(failAction);
				}

			}

			goal.add(actionClass, transitions);
		}

		JSONArray initialActionNames = goalJson.getJSONArray("initialActions");
		if (initialActionNames == null || initialActionNames.isEmpty()) {
			throw new RuntimeException(goal.name + " does not have any initialActions specified");
		}
		for (int cursor = 0; cursor < initialActionNames.size(); cursor++) {
			Class<? extends Action> initialAction = actionDictionary.getByName(initialActionNames.getString(cursor));
			goal.addInitialAction(initialAction);
		}

		return goal;
	}

	/**
	 * This method is to ensure every Action mentioned has an entry in the goal's relationships
	 */
	private void verify(Goal goal) {
		Set<Class<? extends Action>> allActions = goal.getAllTransitions().keySet();

		for (Class<? extends Action> initialAction : goal.getInitialActions()) {
			if (!allActions.contains(initialAction)) {
				throw new RuntimeException(goal.name + " has an initial action " + initialAction.getSimpleName() + " which is not specified in actionRelationships");
			}
		}

		for (ActionTransitions transitions : goal.getAllTransitions().values()) {
			for (Class<? extends Action> successAction : transitions.onSuccess) {
				if (!allActions.contains(successAction)) {
					throw new RuntimeException(goal.name + " has a success action " + successAction.getSimpleName() + " which is not specified in actionRelationships");
				}
			}
			for (Class<? extends Action> failureAction : transitions.onFailure) {
				if (!allActions.contains(failureAction)) {
					throw new RuntimeException(goal.name + " has a failure action " + failureAction.getSimpleName() + " which is not specified in actionRelationships");
				}
			}
		}

		if (byName.containsKey(goal.name)) {
			throw new RuntimeException("Duplicate goal found with name: " + goal.name);
		}

	}

}
