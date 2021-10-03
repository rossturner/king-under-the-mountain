package technology.rocketjump.undermount.entities.behaviour.effects;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.audio.model.ActiveSoundEffect;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.components.furniture.FurnitureParticleEffectsComponent;
import technology.rocketjump.undermount.entities.components.humanoid.SteeringComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.JobTarget;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.ParticleRequestMessage;
import technology.rocketjump.undermount.messaging.types.RequestSoundMessage;
import technology.rocketjump.undermount.messaging.types.RequestSoundStopMessage;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.utils.HexColors;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static technology.rocketjump.undermount.entities.behaviour.effects.BaseOngoingEffectBehaviour.OngoingEffectState.*;

public class BaseOngoingEffectBehaviour implements BehaviourComponent, Destructible {

	protected Entity parentEntity;
	protected MessageDispatcher messageDispatcher;

	protected final AtomicReference<ParticleEffectInstance> currentParticleEffect = new AtomicReference<>(null);
	protected ActiveSoundEffect activeSoundEffect;

	protected OngoingEffectState state = OngoingEffectState.STARTING;
	protected float stateDuration;

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
		OngoingEffectAttributes attributes = (OngoingEffectAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		Entity containerEntity = parentEntity.getLocationComponent().getContainerEntity();
		if (state.equals(STARTING) && containerEntity != null && containerEntity.getType().equals(EntityType.CREATURE)) {
			nextState(gameContext);
		}

		stateDuration += deltaTime;
		if (stateDuration > attributes.getType().getStates().get(state).getDuration()) {
			nextState(gameContext);
		}
		if (this.state == null) {
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, parentEntity);
			return;
		}

		ParticleEffectInstance currentEffectInstance = currentParticleEffect.get();

		if (currentEffectInstance != null && !currentEffectInstance.isActive()) {
			messageDispatcher.dispatchMessage(MessageType.PARTICLE_RELEASE, currentEffectInstance);
			currentParticleEffect.set(null);
			currentEffectInstance = null;
		}

		if (currentEffectInstance == null && state.equals(ACTIVE)) {
			ParticleEffectType particleEffectType = attributes.getType().getParticleEffectType();
			Entity entityToAttachParticleTo = parentEntity;
			if (containerEntity != null) {
				entityToAttachParticleTo = containerEntity;
			}

			messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(
					particleEffectType, Optional.of(entityToAttachParticleTo), Optional.empty(), (particle) -> {
				particle.getWrappedInstance().setTint(HexColors.get(attributes.getType().getInitialColor()));
				currentParticleEffect.set(particle);
			}));
		}

		if (attributes.getType().getPlaySoundAsset() != null && !STARTING.equals(state)) {
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
			Optional<JobTarget> particleTarget = Optional.empty();
			if (containerEntity != null) {
				particleTarget = Optional.of(new JobTarget(containerEntity));
			} else if (parentEntity.getLocationComponent().getWorldPosition() != null) {
				particleTarget = Optional.of(new JobTarget(gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition())));
			}
			particleEffectsComponent.triggerProcessingEffects(particleTarget);
		}

	}

	protected void nextState(GameContext gameContext) {
		switch (state) {
			case STARTING:
				this.state = ACTIVE;
				break;
			case ACTIVE:
				this.state = FADING;
				break;
			case FADING:
				this.state = null;
				break;
		}
		this.stateDuration = 0f;
	}

	public boolean shouldNotificationApply(GameContext gameContext) {
		return true;
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		ParticleEffectInstance effectInstance = currentParticleEffect.get();
		if (effectInstance != null) {
			messageDispatcher.dispatchMessage(MessageType.PARTICLE_RELEASE, effectInstance);
			currentParticleEffect.set(null);
		}

		if (activeSoundEffect != null) {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_STOP_SOUND_LOOP,
					new RequestSoundStopMessage(activeSoundEffect.getAsset(), parentEntity.getId()));
			activeSoundEffect = null;
		}

		FurnitureParticleEffectsComponent particleEffectsComponent = parentEntity.getComponent(FurnitureParticleEffectsComponent.class);
		if (particleEffectsComponent != null) {
			particleEffectsComponent.releaseParticles();
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

	public enum OngoingEffectState {

		STARTING,
		ACTIVE,
		FADING

	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!state.equals(STARTING)) {
			asJson.put("state", state);
		}
		asJson.put("stateDuration", stateDuration);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.state = EnumParser.getEnumValue(asJson, "state", OngoingEffectState.class, STARTING);
		this.stateDuration = asJson.getFloatValue("stateDuration");
	}
}
