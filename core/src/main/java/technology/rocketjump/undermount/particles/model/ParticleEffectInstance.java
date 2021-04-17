package technology.rocketjump.undermount.particles.model;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.particles.custom_libgdx.ParticleEffect;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

public class ParticleEffectInstance {

	private final long instanceId;
	private final ParticleEffectType type;
	private final ParticleEffect gdxParticleEffect;
	private final Optional<Entity> attachedToEntity;
	private final Optional<MapTile> attachedToTile;
	private Vector2 worldPosition = new Vector2();
	private Vector2 offsetFromWorldPosition = new Vector2();
	private boolean isActive = true;

	public ParticleEffectInstance(long instanceId, ParticleEffectType type, ParticleEffect gdxParticleEffect, Entity attachedToEntity) {
		this.instanceId = instanceId;
		this.type = type;
		this.gdxParticleEffect = gdxParticleEffect;
		this.attachedToEntity = Optional.of(attachedToEntity);
		this.attachedToTile = Optional.empty();
	}

	public ParticleEffectInstance(long instanceId, ParticleEffectType type, ParticleEffect gdxParticleEffect, MapTile attachedToTile) {
		this.instanceId = instanceId;
		this.type = type;
		this.gdxParticleEffect = gdxParticleEffect;
		this.attachedToEntity = Optional.empty();
		this.attachedToTile = Optional.of(attachedToTile);
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

	public Optional<MapTile> getAttachedToTile() {
		return attachedToTile;
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

	public void setPositionToParent() {
		if (attachedToEntity.isPresent()) {
			this.setWorldPosition(attachedToEntity.get().getLocationComponent().getWorldOrParentPosition());
		} else if (attachedToTile.isPresent()) {
			this.setWorldPosition(attachedToTile.get().getWorldPositionOfCenter());
		}
		if (type.getOffsetFromParentEntity() != null) {
			this.getWorldPosition().add(type.getOffsetFromParentEntity());
		}
	}

	public static class YDepthEntityComparator implements Comparator<ParticleEffectInstance> {
		@Override
		public int compare(ParticleEffectInstance o1, ParticleEffectInstance o2) {
			float o1Position = o1.worldPosition.y;
			float o2Position = o2.worldPosition.y;
			return (int)(100000f * (o2Position - o1Position));
		}
	}

}
