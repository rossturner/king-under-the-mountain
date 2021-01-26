package technology.rocketjump.undermount.particles;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.jobs.model.JobTarget;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.ParticleRequestMessage;
import technology.rocketjump.undermount.messaging.types.ParticleUpdateMessage;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.rendering.camera.TileBoundingBox;

import java.util.Iterator;
import java.util.Optional;

import static technology.rocketjump.undermount.jobs.model.JobTarget.NULL_TARGET;

@Singleton
public class ParticleEffectUpdater implements Telegraph {

	private final ParticleEffectStore store;

	private TileBoundingBox currentBoundingBox;

	@Inject
	public ParticleEffectUpdater(ParticleEffectStore store, MessageDispatcher messageDispatcher) {
		this.store = store;

		messageDispatcher.addListener(this, MessageType.PARTICLE_REQUEST);
		messageDispatcher.addListener(this, MessageType.PARTICLE_UPDATE);
		messageDispatcher.addListener(this, MessageType.PARTICLE_RELEASE);
	}

	public void update(float deltaTime, TileBoundingBox boundingBox) {
		this.currentBoundingBox = boundingBox;

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

			if (!currentBoundingBox.contains(instance.getWorldPosition())) {
				store.remove(instance, iterator);
				continue;
			}

			if (instance.getGdxParticleEffect().isComplete()) {
				store.remove(instance, iterator);
			} else {
				instance.getGdxParticleEffect().update(deltaTime);
			}
		}

	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.PARTICLE_REQUEST: {
				handle((ParticleRequestMessage)msg.extraInfo);
				return true;
			}
			case MessageType.PARTICLE_UPDATE: {
				handle((ParticleUpdateMessage)msg.extraInfo);
				return true;
			}
			case MessageType.PARTICLE_RELEASE: {
				release((ParticleEffectInstance)msg.extraInfo);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void handle(ParticleRequestMessage particleRequestMessage) {
		if (particleRequestMessage.parentEntity.isPresent()) {
			Entity parentEntity = particleRequestMessage.parentEntity.get();
			Vector2 parentPosition = parentEntity.getLocationComponent().getWorldOrParentPosition();
			if (insideBounds(parentPosition)) {
				Optional<GameMaterial> relatedMaterial = Optional.ofNullable(particleRequestMessage.effectTarget.orElse(NULL_TARGET).getTargetMaterial());
				ParticleEffectInstance instance = store.create(particleRequestMessage.type, parentEntity, relatedMaterial);
				particleRequestMessage.callback.particleCreated(instance);
			} else {
				particleRequestMessage.callback.particleCreated(null);
			}
		} else if (particleRequestMessage.effectTarget.isPresent() && particleRequestMessage.effectTarget.get().type.equals(JobTarget.JobTargetType.TILE)) {
			// Attaching particle effect to tile
			MapTile targetTile = particleRequestMessage.effectTarget.get().getTile();
			Vector2 position = targetTile.getWorldPositionOfCenter();
			if (insideBounds(position)) {
				ParticleEffectInstance instance = store.create(particleRequestMessage.type, targetTile, Optional.of(targetTile.getFloor().getMaterial()));
				particleRequestMessage.callback.particleCreated(instance);
			} else {
				particleRequestMessage.callback.particleCreated(null);
			}
		} else {
			Logger.error("Not yet implemented - particles not attached to an entity");
			particleRequestMessage.callback.particleCreated(null);
		}
	}

	private void handle(ParticleUpdateMessage particleUpdateMessage) {
		// TODO update progress
	}

	private void release(ParticleEffectInstance effectInstance) {
		// This is used to stop an effect looping so it will die off at the end of current cycle
		effectInstance.getGdxParticleEffect().allowCompletion();
	}

	private boolean insideBounds(Vector2 position) {
		if (currentBoundingBox == null) {
			return false;
		} else {
			return currentBoundingBox.contains(position);
		}
	}

}
