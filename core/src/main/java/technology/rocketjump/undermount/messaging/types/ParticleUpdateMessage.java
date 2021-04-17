package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;

public class ParticleUpdateMessage {

	private final ParticleEffectInstance instance;
	private final float progress;

	public ParticleUpdateMessage(ParticleEffectInstance effectInstance, float progress) {
		this.instance = effectInstance;
		this.progress = progress;
	}

}
