package technology.rocketjump.undermount.particles;

import com.badlogic.gdx.graphics.Color;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;

import java.util.*;

@Singleton
public class ParticleEffectStore implements GameContextAware {

	private static final List<ParticleEffectInstance> EMPTY_LIST = new LinkedList<>();
	private final Map<Long, ParticleEffectInstance> byInstanceId = new HashMap<>();
	private final Map<Long, List<ParticleEffectInstance>> byRelatedEntityId = new HashMap<>();

	private final ParticleEffectFactory factory;

	@Inject
	public ParticleEffectStore(ParticleEffectFactory factory) {
		this.factory = factory;
	}

	public ParticleEffectInstance create(ParticleEffectType particleEffectType, Entity parentEntity, Optional<Color> relatedMaterialColor) {
		ParticleEffectInstance instance = factory.create(particleEffectType, Optional.of(parentEntity), Optional.empty(), relatedMaterialColor);
		if (instance == null) {
			return null;
		}

		byInstanceId.put(instance.getInstanceId(), instance);
		byRelatedEntityId.computeIfAbsent(parentEntity.getId(), (a) -> new ArrayList<>()).add(instance);

		return instance;
	}

	public ParticleEffectInstance create(ParticleEffectType particleEffectType, MapTile parentTile, Optional<Color> relatedMaterialColor) {
		ParticleEffectInstance instance = factory.create(particleEffectType, Optional.empty(), Optional.of(parentTile), relatedMaterialColor);
		if (instance == null) {
			return null;
		}

		byInstanceId.put(instance.getInstanceId(), instance);

		return instance;
	}

	public List<ParticleEffectInstance> getParticlesAttachedToEntity(Entity entity) {
		return byRelatedEntityId.getOrDefault(entity.getId(), EMPTY_LIST);
	}

	public Iterator<ParticleEffectInstance> getIterator() {
		return byInstanceId.values().iterator();
	}

	@Override
	public void onContextChange(GameContext gameContext) {

	}

	@Override
	public void clearContextRelatedState() {
		byInstanceId.clear();
		byRelatedEntityId.clear();
	}

	public void remove(ParticleEffectInstance instance, Iterator<ParticleEffectInstance> instanceIdIterator) {
		instance.setActive(false);
		instanceIdIterator.remove();
		if (instance.getAttachedToEntity().isPresent()) {
			long entityId = instance.getAttachedToEntity().get().getId();
			List<ParticleEffectInstance> attachedToEntity = byRelatedEntityId.getOrDefault(entityId, EMPTY_LIST);
			attachedToEntity.remove(instance);
			if (attachedToEntity.isEmpty()) {
				byRelatedEntityId.remove(entityId);
			}
		}
		if (instance.getAttachedToTile().isPresent()) {
			MapTile mapTile = instance.getAttachedToTile().get();
			mapTile.getParticleEffects().remove(instance.getInstanceId());
		}
	}
}
