package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.jobs.model.JobTarget;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;

import java.util.Optional;

public class ParticleRequestMessage {

	public final ParticleEffectType type;
	public final Optional<Entity> parentEntity;
	public final Optional<JobTarget> jobTarget;
	public final ParticleCreationCallback callback;

	public ParticleRequestMessage(ParticleEffectType type, Optional<Entity> parentEntity, Optional<JobTarget> jobTarget, ParticleCreationCallback callback) {
		this.type = type;
		this.parentEntity = parentEntity;
		this.jobTarget = jobTarget;
		this.callback = callback;
	}

	public interface ParticleCreationCallback {

		void particleCreated(ParticleEffectInstance instance);

	}
}
