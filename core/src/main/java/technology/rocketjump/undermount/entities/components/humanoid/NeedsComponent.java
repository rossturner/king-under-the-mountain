package technology.rocketjump.undermount.entities.components.humanoid;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.ai.goap.EntityNeed;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.status.Exhausted;
import technology.rocketjump.undermount.entities.model.physical.creature.status.VeryHungry;
import technology.rocketjump.undermount.entities.model.physical.creature.status.VeryThirsty;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.StatusMessage;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static technology.rocketjump.undermount.entities.ai.goap.EntityNeed.*;
import static technology.rocketjump.undermount.entities.model.physical.creature.Consciousness.AWAKE;

public class NeedsComponent implements EntityComponent {

	public static final double MAX_NEED_VALUE = 100;
	public static final double MIN_NEED_VALUE = -10;
	private Map<EntityNeed, Double> needValues = new EnumMap<>(EntityNeed.class);

	private static final double SLEEP_HOURS_TO_FULLY_RESTED = 12;
	private static final double AWAKE_HOURS_UNTIL_UNCONSCIOUS = 30;
	private static final double HOURS_TO_STARVING_FROM_FULL = 32;
	private static final double HOURS_TO_DEHYDRATED_FROM_QUENCHED = 30;

	public NeedsComponent() {

	}

	public NeedsComponent(List<EntityNeed> needs, Random random) {
		for (EntityNeed entityNeed : needs) {
			// Start off with needs from 70 to
			double initialValue = 70 + (random.nextDouble() * 30);
			needValues.put(entityNeed, initialValue);
		}
	}

	private NeedsComponent(Map<EntityNeed, Double> other) {
		needValues.putAll(other);
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		return new NeedsComponent(this.needValues);
	}

	public void update(double elapsedGameHours, Entity parentEntity, MessageDispatcher messageDispatcher) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();

		if (needValues.containsKey(SLEEP)) {
			Double currentSleepValue = needValues.get(EntityNeed.SLEEP);

			// TODO MODDING data-drive this
			if (AWAKE.equals(attributes.getConsciousness())) {
				// Assume a dwarf can go from fully rested to desperately needing sleep in... 40 hours?
				double decrementAmount = (elapsedGameHours / AWAKE_HOURS_UNTIL_UNCONSCIOUS) * MAX_NEED_VALUE;
				needValues.put(EntityNeed.SLEEP, Math.max(currentSleepValue - decrementAmount, MIN_NEED_VALUE));
			} else {
				// Asleep or unconscious, let's say 10 hours is enough to go from urgently needing sleep to well rested
				double incrementAmount = (elapsedGameHours / SLEEP_HOURS_TO_FULLY_RESTED) * MAX_NEED_VALUE;
				needValues.put(EntityNeed.SLEEP, Math.min(currentSleepValue + incrementAmount, MAX_NEED_VALUE));
			}

			if (getValue(SLEEP) <= 0) {
				messageDispatcher.dispatchMessage(MessageType.APPLY_STATUS, new StatusMessage(parentEntity, Exhausted.class, null));
			}
		}

		if (needValues.containsKey(FOOD)) {
			updateNeed(elapsedGameHours, FOOD, HOURS_TO_STARVING_FROM_FULL);

			if (getValue(FOOD) <= 0) {
				messageDispatcher.dispatchMessage(MessageType.APPLY_STATUS, new StatusMessage(parentEntity, VeryHungry.class, null));
			}
		}

		if (needValues.containsKey(DRINK)) {
			updateNeed(elapsedGameHours, EntityNeed.DRINK, HOURS_TO_DEHYDRATED_FROM_QUENCHED);

			if (getValue(DRINK) <= 0) {
				messageDispatcher.dispatchMessage(MessageType.APPLY_STATUS, new StatusMessage(parentEntity, VeryThirsty.class, null));
			}
		}
	}

	private void updateNeed(double elapsedGameHours, EntityNeed need, double hoursToFullyEmpty) {
		Double currentValue = needValues.get(need);
		double changeAmount = (elapsedGameHours / hoursToFullyEmpty) * MAX_NEED_VALUE;
		Double newValue = Math.max(currentValue - changeAmount, MIN_NEED_VALUE);
		needValues.put(need, newValue);
	}

	public Double getValue(EntityNeed need) {
		return needValues.get(need);
	}

	public void setValue(EntityNeed need, double value) {
		needValues.put(need, Math.max(Math.min(value, MAX_NEED_VALUE), MIN_NEED_VALUE));
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!needValues.isEmpty()) {
			JSONArray needsJson = new JSONArray();
			for (Map.Entry<EntityNeed, Double> entry : needValues.entrySet()) {
				JSONObject needJson = new JSONObject(true);
				needJson.put("need", entry.getKey());
				needJson.put("value", entry.getValue());
				needsJson.add(needJson);
			}
			asJson.put("needs", needsJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONArray needsJson = asJson.getJSONArray("needs");
		if (needsJson != null) {
			for (int cursor = 0; cursor < needsJson.size(); cursor++) {
				JSONObject needJson = needsJson.getJSONObject(cursor);
				this.needValues.put(
						EnumParser.getEnumValue(needJson, "need", EntityNeed.class, null),
						needJson.getDoubleValue("value")
				);
			}
		}
	}

	public boolean has(EntityNeed need) {
		return needValues.containsKey(need);
	}
}
