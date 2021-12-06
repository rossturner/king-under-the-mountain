package technology.rocketjump.undermount.particles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.jobs.model.JobTarget;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.ParticleEffectTypeCallback;
import technology.rocketjump.undermount.messaging.types.ParticleRequestMessage;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.rendering.camera.TileBoundingBox;

import java.util.Iterator;
import java.util.Optional;

import static technology.rocketjump.undermount.jobs.model.JobTarget.NULL_TARGET;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.particles.CustomEffectFactory.PROGRESS_BAR_EFFECT_TYPE_NAME;

@Singleton
public class ParticleEffectUpdater implements Telegraph, GameContextAware {

	private final ParticleEffectStore store;
	private final ParticleEffectTypeDictionary particleEffectTypeDictionary;

	private TileBoundingBox currentBoundingBox;
	private GameContext gameContext;
	private boolean cameraIsAtMaxZoom;

	@Inject
	public ParticleEffectUpdater(ParticleEffectStore store, MessageDispatcher messageDispatcher, ParticleEffectTypeDictionary particleEffectTypeDictionary) {
		this.store = store;
		this.particleEffectTypeDictionary = particleEffectTypeDictionary;

		messageDispatcher.addListener(this, MessageType.PARTICLE_REQUEST);
		messageDispatcher.addListener(this, MessageType.PARTICLE_RELEASE);
		messageDispatcher.addListener(this, MessageType.PARTICLE_FORCE_REMOVE);
		messageDispatcher.addListener(this, MessageType.GET_PROGRESS_BAR_EFFECT_TYPE);
	}

	public void update(float deltaTime, TileBoundingBox boundingBox, boolean cameraIsAtMaxZoom) {
		this.currentBoundingBox = boundingBox;
		this.cameraIsAtMaxZoom = cameraIsAtMaxZoom;

		Iterator<ParticleEffectInstance> iterator = store.getIterator();

		while (iterator.hasNext()) {
			ParticleEffectInstance instance = iterator.next();

			if (instance.getAttachedToEntity().isPresent()) {
				if (instance.getType().isAttachedToParent()) {
					instance.setPositionToParent();
				}
			} else {
				// Not yet implemented - updating effects not attached to an entity
			}

			if (!insideBounds(instance.getWorldPosition())) {
				store.remove(instance, iterator);
				continue;
			}

			if (instance.getWrappedInstance().isComplete()) {
				store.remove(instance, iterator);
			} else {

				if (gameContext.getGameClock().isPaused()) {
					if (instance.getType().isUnaffectedByPause()) {
						instance.getWrappedInstance().update(Gdx.graphics.getDeltaTime() * 0.3f);
					}
				} else {
					instance.getWrappedInstance().update(deltaTime);
				}
			}
		}

	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.PARTICLE_REQUEST: {
				handle((ParticleRequestMessage) msg.extraInfo);
				return true;
			}
			case MessageType.PARTICLE_RELEASE: {
				release((ParticleEffectInstance) msg.extraInfo);
				return true;
			}
			case MessageType.PARTICLE_FORCE_REMOVE: {
				ParticleEffectInstance instance = (ParticleEffectInstance) msg.extraInfo;
				store.remove(instance, null);
				return true;
			}
			case MessageType.GET_PROGRESS_BAR_EFFECT_TYPE: {
				ParticleEffectTypeCallback callback = (ParticleEffectTypeCallback) msg.extraInfo;
				callback.typeFound(particleEffectTypeDictionary.getByName(PROGRESS_BAR_EFFECT_TYPE_NAME));
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void handle(ParticleRequestMessage particleRequestMessage) {
		if (particleRequestMessage.type == null) {
			particleRequestMessage.type = particleEffectTypeDictionary.getByName(particleRequestMessage.typeName);
			if (particleRequestMessage.type == null) {
				Logger.error("Could not find particle effect type with name " + particleRequestMessage.typeName);
				return;
			}
		}

		Optional<Color> targetColor = Optional.ofNullable(particleRequestMessage.effectTarget.orElse(NULL_TARGET).getTargetColor());
		if (particleRequestMessage.getOverrideColor() != null) {
			targetColor = Optional.of(particleRequestMessage.getOverrideColor());
		}
		if (particleRequestMessage.parentEntity.isPresent()) {
			Entity parentEntity = particleRequestMessage.parentEntity.get();
			Vector2 parentPosition = parentEntity.getLocationComponent().getWorldOrParentPosition();
			if (insideBounds(parentPosition)) {
				ParticleEffectInstance instance = store.create(particleRequestMessage.type, parentEntity, targetColor);
				if (instance != null) {
					particleRequestMessage.callback.particleCreated(instance);
				}
			}
		} else if (particleRequestMessage.effectTarget.isPresent() && (particleRequestMessage.effectTarget.get().type.equals(JobTarget.JobTargetType.TILE) ||
				particleRequestMessage.effectTarget.get().type.equals(JobTarget.JobTargetType.ROOF))) {
			// Attaching particle effect to tile
			MapTile targetTile = particleRequestMessage.effectTarget.get().getTile();
			Vector2 position = targetTile.getWorldPositionOfCenter();
			if (insideBounds(position)) {
				ParticleEffectInstance instance = store.create(particleRequestMessage.type, targetTile, targetColor);
				if (instance != null) {
					targetTile.getParticleEffects().put(instance.getInstanceId(), instance);
					particleRequestMessage.callback.particleCreated(instance);
				}
			}

		} else if (particleRequestMessage.effectTarget.isPresent() && particleRequestMessage.effectTarget.get().type.equals(JobTarget.JobTargetType.ENTITY)) {
			// Target is entity but parentEntity is null, probably deleting parent entity, so attach to tile
			Vector2 position = particleRequestMessage.effectTarget.get().getEntity().getLocationComponent().getWorldOrParentPosition();
			MapTile targetTile = gameContext.getAreaMap().getTile(position);
			if (insideBounds(position)) {
				ParticleEffectInstance instance = store.create(particleRequestMessage.type, targetTile, targetColor);
				if (instance != null) {
					targetTile.getParticleEffects().put(instance.getInstanceId(), instance);
					particleRequestMessage.callback.particleCreated(instance);
				}
			}
		} else {
//			Logger.warn("Not yet implemented - particles not attached to an entity");
		}
	}

	private void release(ParticleEffectInstance effectInstance) {
		// This is used to stop an effect looping so it will die off at the end of current cycle
		effectInstance.getWrappedInstance().allowCompletion();
	}

	private boolean insideBounds(Vector2 position) {
		if (cameraIsAtMaxZoom) {
			return false;
		} else if (currentBoundingBox == null || position == null) {
			return false;
		} else {
			return currentBoundingBox.contains(toGridPoint(position));
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
