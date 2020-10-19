package technology.rocketjump.undermount.entities.components.humanoid;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.*;

/**
 * This class is to keep track of current changes in a Settler's happiness
 */
public class HappinessComponent implements EntityComponent {

	public static final int MAX_HAPPINESS_VALUE = 100;
	public static final int MIN_HAPPINESS_VALUE = -100;
	private final Map<HappinessModifier, Double> timesToExpiry = new EnumMap<>(HappinessModifier.class);

	private int netModifier = 0;

	public void infrequentUpdate(double elapsedTime) {
		for (HappinessModifier happinessModifier : new HashSet<>(timesToExpiry.keySet())) {
			double currentExpiry = timesToExpiry.get(happinessModifier);
			double newExpiry = currentExpiry - elapsedTime;
			if (newExpiry < 0) {
				timesToExpiry.remove(happinessModifier);
			} else {
				timesToExpiry.put(happinessModifier, newExpiry);
			}
		}

		updateNetModifier();
	}

	public Set<HappinessModifier> currentModifiers() {
		return timesToExpiry.keySet();
	}

	/**
	 * Re-adding a HappinessModifier will reset the time to expiry
	 */
	public void add(HappinessModifier happinessModifier) {
		for (HappinessModifier existingModifier : currentModifiers()) {
			if (existingModifier.replaces.contains(happinessModifier)) {
				return;
			}
			if (existingModifier.replacedBy.contains(happinessModifier)) {
				timesToExpiry.remove(existingModifier);
			}
		}

		this.timesToExpiry.put(happinessModifier, happinessModifier.hoursToExpiry);

		updateNetModifier();
	}

	public int getNetModifier() {
		return netModifier;
	}

	private void updateNetModifier() {
		int updatedModifier = 0;
		for (HappinessModifier modifier : timesToExpiry.keySet()) {
			updatedModifier += modifier.modifierAmount;
		}
		updatedModifier = Math.min(MAX_HAPPINESS_VALUE, updatedModifier);
		updatedModifier = Math.max(MIN_HAPPINESS_VALUE, updatedModifier);

		this.netModifier = updatedModifier;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		HappinessComponent clone = new HappinessComponent();
		clone.timesToExpiry.putAll(timesToExpiry);
		return clone;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		JSONObject timeToExpiryJson = new JSONObject(true);
		for (Map.Entry<HappinessModifier, Double> entry : timesToExpiry.entrySet()) {
			timeToExpiryJson.put(entry.getKey().name(), entry.getValue());
		}
		asJson.put("expiry", timeToExpiryJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONObject timeToExpiryJson = asJson.getJSONObject("expiry");
		if (timeToExpiryJson == null) {
			throw new InvalidSaveException("Could not find expiry JSON in " + this.getClass().getSimpleName());
		}
		for (String modifierName : timeToExpiryJson.keySet()) {
			if (!EnumUtils.isValidEnum(HappinessModifier.class, modifierName)) {
				throw new InvalidSaveException("Could not parse " + HappinessModifier.class.getSimpleName() + " with name " + modifierName);
			}
			HappinessModifier modifier = HappinessModifier.valueOf(modifierName);
			Double expiryTime = timeToExpiryJson.getDoubleValue(modifierName);
			timesToExpiry.put(modifier, expiryTime);
		}

		updateNetModifier();
	}

	/**
	 * These probably want to be data-driven by race, so dwarves hate sleeping outside but elves do not
	 *
	 * Want to ensure that every instance is used somewhere
	 */
	public enum HappinessModifier {

		NEW_SETTLEMENT_OPTIMISM(40, 24.0 * 15),

		SAW_DEAD_BODY(-50, 24.0 * 3.0),
		CARRIED_DEAD_BODY(-55, 24.0 * 1.0),

		DRANK_FROM_RIVER(-5, 1.5),
		ATE_NICELY_PREPARED_FOOD(5, 3.0),

		VERY_TIRED(-25, 2.5),
		POISONED(-30, 2.5),
		VERY_HUNGRY(-30, 0.5),
		VERY_THIRSTY(-30, 0.5),
		DYING_OF_HUNGER(-50, 0.5),
		DYING_OF_THIRST(-50, 0.5),

		SLEPT_OUTSIDE(-20, 3.0),
		SLEPT_ON_GROUND(-20, 5.0),
		SLEPT_IN_SHARED_BEDROOM(-10, 5.0),
		SLEPT_IN_PRIVATE_BEDROOM(10, 5.0),
		SLEPT_IN_SMALL_BEDROOM(-5, 5.0),
		SLEPT_IN_LARGE_PRIVATE_BEDROOM(20, 5.0),

		DRANK_ALCOHOL(40, 8),
		ALCOHOL_WITHDRAWL(-30, 0.5);

		public final int modifierAmount;
		private final double hoursToExpiry;
		private final List<HappinessModifier> replaces = new ArrayList<>();
		private final List<HappinessModifier> replacedBy = new ArrayList<>();

		static {
			SLEPT_IN_SHARED_BEDROOM.replaces.add(SLEPT_IN_PRIVATE_BEDROOM);
			SLEPT_IN_SHARED_BEDROOM.replaces.add(SLEPT_IN_LARGE_PRIVATE_BEDROOM);
			SLEPT_IN_SMALL_BEDROOM.replaces.add(SLEPT_IN_PRIVATE_BEDROOM);
			CARRIED_DEAD_BODY.replaces.add(SAW_DEAD_BODY);
			SLEPT_IN_LARGE_PRIVATE_BEDROOM.replaces.add(SLEPT_IN_PRIVATE_BEDROOM);

			DYING_OF_HUNGER.replaces.add(VERY_HUNGRY);
			DYING_OF_THIRST.replaces.add(VERY_THIRSTY);

			DRANK_ALCOHOL.replaces.add(ALCOHOL_WITHDRAWL);

			for (HappinessModifier happinessModifier : HappinessModifier.values()) {
				for (HappinessModifier otherModifier : HappinessModifier.values()) {
					if (happinessModifier.equals(otherModifier)) {
						continue;
					}
					if (otherModifier.replaces.contains(happinessModifier)) {
						happinessModifier.replacedBy.add(otherModifier);
					}
				}
			}
		}


		HappinessModifier(int modifierAmount, double hoursToExpiry) {
			this.modifierAmount = modifierAmount;
			this.hoursToExpiry = hoursToExpiry;
		}

		public String getI18nKey() {
			return "HAPPINESS_MODIFIER."+name();
		}
	}
}
