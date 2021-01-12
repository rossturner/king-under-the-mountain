package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.jobs.model.JobTarget;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;

import java.util.Optional;

public class ParticleRequestMessage {

	public ParticleRequestMessage(ParticleEffectType type, Optional<Entity> parentEntity, Optional<JobTarget> jobTarget, ParticleCreationCallback callback) {

	}

	public interface ParticleCreationCallback {

		void particleCreated(ParticleEffectInstance instance);

	}
}
