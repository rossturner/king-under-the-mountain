package technology.rocketjump.undermount.particles.model;

import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.model.Entity;

import java.util.Optional;

public class ParticleEffectInstance {

	private long instanceId;
	private ParticleEffectType type;
	private ParticleEffect gdxParticleEffect;
	private Optional<Entity> attachedToEntity;
	private Vector2 worldPosition;
	private Vector2 offsetFromWorldPosition;

	public ParticleEffectType getType() {
		return type;
	}
}
