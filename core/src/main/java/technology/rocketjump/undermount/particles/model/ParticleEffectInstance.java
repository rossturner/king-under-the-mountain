package technology.rocketjump.undermount.particles.model;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.particles.custom_libgdx.ParticleEffect;

import java.util.Objects;
import java.util.Optional;

public class ParticleEffectInstance {

	private final long instanceId;
	private final ParticleEffectType type;
	private ParticleEffect gdxParticleEffect;
	private Optional<Entity> attachedToEntity;
	private Vector2 worldPosition = new Vector2();
	private Vector2 offsetFromWorldPosition = new Vector2();
	private boolean isActive = true;

	public ParticleEffectInstance(long instanceId, ParticleEffectType type, ParticleEffect gdxParticleEffect, Optional<Entity> attachedToEntity) {
		this.instanceId = instanceId;
		this.type = type;
		this.gdxParticleEffect = gdxParticleEffect;
		this.attachedToEntity = attachedToEntity;
	}

	public ParticleEffectType getType() {
		return type;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean active) {
		this.isActive = active;
	}

	public void setWorldPosition(Vector2 worldPosition) {
		this.worldPosition.set(worldPosition);
		updateGdxPosition();
	}

	public void setOffsetFromWorldPosition(Vector2 offsetFromWorldPosition) {
		this.offsetFromWorldPosition.set(offsetFromWorldPosition);
		updateGdxPosition();
	}

	private void updateGdxPosition() {
		gdxParticleEffect.setPosition(worldPosition.x + offsetFromWorldPosition.x, worldPosition.y + offsetFromWorldPosition.y);
	}

	public long getInstanceId() {
		return instanceId;
	}

	public ParticleEffect getGdxParticleEffect() {
		return gdxParticleEffect;
	}

	public Optional<Entity> getAttachedToEntity() {
		return attachedToEntity;
	}

	public Vector2 getWorldPosition() {
		return worldPosition;
	}

	public Vector2 getOffsetFromWorldPosition() {
		return offsetFromWorldPosition;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ParticleEffectInstance that = (ParticleEffectInstance) o;
		return instanceId == that.instanceId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(instanceId);
	}

	public void setPositionToParentEntity() {
		if (attachedToEntity.isPresent()) {
			this.setWorldPosition(attachedToEntity.get().getLocationComponent().getWorldOrParentPosition());
			this.getWorldPosition().add(0, attachedToEntity.get().getLocationComponent().getRadius());
		}
	}
}
