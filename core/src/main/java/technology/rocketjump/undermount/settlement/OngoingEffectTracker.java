package technology.rocketjump.undermount.settlement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.rendering.ScreenWriter;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;

@Singleton
public class OngoingEffectTracker implements GameContextAware, Updatable {

	private final Map<OngoingEffectType, Map<Long, Entity>> byEffectType = new HashMap<>();

	private final ScreenWriter screenWriter;

	@Inject
	public OngoingEffectTracker(ScreenWriter screenWriter) {

		this.screenWriter = screenWriter;
	}

	public void entityAdded(Entity entity) {
		OngoingEffectAttributes attributes = (OngoingEffectAttributes) entity.getPhysicalEntityComponent().getAttributes();

		byEffectType.computeIfAbsent(attributes.getType(), a -> new HashMap<>()).put(entity.getId(), entity);
	}

	public void entityRemoved(Entity entity) {
		OngoingEffectAttributes attributes = (OngoingEffectAttributes) entity.getPhysicalEntityComponent().getAttributes();
		byEffectType.getOrDefault(attributes.getType(), emptyMap()).remove(entity.getId());
		if (byEffectType.get(attributes.getType()).isEmpty()) {
			byEffectType.remove(attributes.getType());
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {

	}

	@Override
	public void clearContextRelatedState() {
		byEffectType.clear();
	}

	@Override
	public void update(float deltaTime) {
		for (Map.Entry<OngoingEffectType, Map<Long, Entity>> entry : byEffectType.entrySet()) {
			screenWriter.printLine(entry.getKey().getName() + ": " + entry.getValue().size());
		}

	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}
}
