package technology.rocketjump.undermount.entities.components.humanoid;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.humanoid.status.StatusEffect;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StatusComponent implements ParentDependentEntityComponent {

	private Entity parentEntity;
	private MessageDispatcher messageDispatcher;
	private GameContext gameContext;

	private final Map<Class<? extends StatusEffect>, StatusEffect> byClassType = new HashMap<>();

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;
		this.gameContext = gameContext;

		for (StatusEffect statusEffect : byClassType.values()) {
			statusEffect.setParentEntity(parentEntity);
		}
	}

	public void infrequentUpdate(double elapsedTime) {
		for (StatusEffect statusEffect : new ArrayList<>(byClassType.values())) {
			statusEffect.infrequentUpdate(elapsedTime, gameContext, messageDispatcher);
		}
	}

	public void apply(StatusEffect statusEffect) {
		// Should not replace time on current status
		if (!byClassType.containsKey(statusEffect.getClass())) {
			statusEffect.setParentEntity(parentEntity);
			byClassType.put(statusEffect.getClass(), statusEffect);
		}
	}

	public void remove(Class<? extends StatusEffect> statusClass) {
		byClassType.remove(statusClass);
	}

	public boolean contains(Class<? extends StatusEffect> statusClass) {
		return byClassType.containsKey(statusClass);
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		throw new NotImplementedException("Not implemented: clone() in " + this.getClass().getSimpleName());
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!byClassType.isEmpty()) {
			JSONArray effectsJson = new JSONArray();
			for (StatusEffect statusEffect : byClassType.values()) {
				JSONObject effectJson = new JSONObject(true);
				effectJson.put("_class", statusEffect.getClass().getSimpleName());
				statusEffect.writeTo(effectJson, savedGameStateHolder);
				effectsJson.add(effectJson);
			}
			asJson.put("effects", effectsJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {

		JSONArray effectsJson = asJson.getJSONArray("effects");
		if (effectsJson != null) {
			for (int cursor = 0; cursor < effectsJson.size(); cursor++) {
				JSONObject effectJson = effectsJson.getJSONObject(cursor);
				Class<? extends StatusEffect> effectClass = relatedStores.statusEffectDictionary.getByName(effectJson.getString("_class"));
				try {
					StatusEffect statusEffect = effectClass.getDeclaredConstructor().newInstance();
					statusEffect.readFrom(effectJson, savedGameStateHolder, relatedStores);
					byClassType.put(statusEffect.getClass(), statusEffect);
				} catch (ReflectiveOperationException e) {
					throw new InvalidSaveException("Failed to instantiate StatusEffect " + effectClass.getSimpleName() + ", expecting no-arg constructor\n" + e.getMessage());
				}
			}
		}
	}

	public int count() {
		return byClassType.size();
	}

	public Collection<StatusEffect> getAll() {
		return byClassType.values();
	}
}
