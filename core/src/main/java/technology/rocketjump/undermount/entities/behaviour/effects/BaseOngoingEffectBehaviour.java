package technology.rocketjump.undermount.entities.behaviour.effects;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.audio.model.ActiveSoundEffect;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.components.furniture.FurnitureParticleEffectsComponent;
import technology.rocketjump.undermount.entities.components.humanoid.SteeringComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.JobTarget;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.ParticleRequestMessage;
import technology.rocketjump.undermount.messaging.types.RequestSoundMessage;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class BaseOngoingEffectBehaviour implements BehaviourComponent, Destructible {

	private Entity parentEntity;
	private MessageDispatcher messageDispatcher;

	private final AtomicReference<ParticleEffectInstance> currentParticleEffect = new AtomicReference<>(null);
	private ActiveSoundEffect activeSoundEffect;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;
	}

	@Override
	public BaseOngoingEffectBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		BaseOngoingEffectBehaviour cloned = new BaseOngoingEffectBehaviour();
		cloned.init(parentEntity, messageDispatcher, gameContext);
		return cloned;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		ParticleEffectInstance currentEffectInstance = currentParticleEffect.get();
		OngoingEffectAttributes attributes = (OngoingEffectAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();

		if (currentEffectInstance != null && !currentEffectInstance.isActive()) {
			messageDispatcher.dispatchMessage(MessageType.PARTICLE_RELEASE, currentEffectInstance);
			currentParticleEffect.set(null);
			currentEffectInstance = null;
		}

		if (currentEffectInstance == null) {
			ParticleEffectType particleEffectType = attributes.getType().getParticleEffectType();
			messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(
					particleEffectType, Optional.of(parentEntity), Optional.empty(), (particle) -> currentParticleEffect.set(particle)
			));
		}

		if (attributes.getType().getPlaySoundAsset() != null) {
			if (activeSoundEffect != null) {
				if (activeSoundEffect.completed()) {
					activeSoundEffect = null;
				}
			}

			if (activeSoundEffect == null) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(
						attributes.getType().getPlaySoundAsset(), parentEntity, (sound) -> {
					activeSoundEffect = sound;
				}));
			}
		}

		FurnitureParticleEffectsComponent particleEffectsComponent = parentEntity.getComponent(FurnitureParticleEffectsComponent.class);
		if (particleEffectsComponent != null) {
			particleEffectsComponent.triggerProcessingEffects(Optional.of(new JobTarget(parentEntity)));
		}

	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		ParticleEffectInstance effectInstance = currentParticleEffect.get();
		if (effectInstance != null) {
			messageDispatcher.dispatchMessage(MessageType.PARTICLE_RELEASE, effectInstance);
			currentParticleEffect.set(null);
		}
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {

	}


	@Override
	public SteeringComponent getSteeringComponent() {
		return null;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return true;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return false;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// Nothing to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// Nothing to read
	}
}
