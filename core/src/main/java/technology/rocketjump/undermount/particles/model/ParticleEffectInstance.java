package technology.rocketjump.undermount.particles.model;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.particles.custom_libgdx.ParticleEffect;

import java.util.Objects;
import java.util.Optional;

public class ParticleEffectInstance {

	private final long instanceId;
	private final ParticleEffectType type;
	private final ParticleEffect wrappedInstance;
	private final Optional<Entity> attachedToEntity;
	private final Optional<MapTile> attachedToTile;
	private final Vector2 worldPosition = new Vector2();
	private final Vector2 offsetFromWorldPosition;
	private boolean isActive = true;

	public ParticleEffectInstance(long instanceId, ParticleEffectType type, ParticleEffect wrappedInstance, Entity attachedToEntity) {
		this.instanceId = instanceId;
		this.type = type;
		this.wrappedInstance = wrappedInstance;
		this.attachedToEntity = Optional.of(attachedToEntity);
		if (type.getOffsetFromParentEntity() != null) {
			this.offsetFromWorldPosition = type.getOffsetFromParentEntity().cpy();
		} else {
			this.offsetFromWorldPosition = new Vector2();
		}
		this.attachedToTile = Optional.empty();
	}

	public ParticleEffectInstance(long instanceId, ParticleEffectType type, ParticleEffect wrappedInstance, MapTile attachedToTile) {
		this.instanceId = instanceId;
		this.type = type;
		this.wrappedInstance = wrappedInstance;
		this.attachedToEntity = Optional.empty();
		if (type.getOffsetFromParentEntity() != null) {
			this.offsetFromWorldPosition = type.getOffsetFromParentEntity().cpy();
		} else {
			this.offsetFromWorldPosition = new Vector2();
		}
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
		wrappedInstance.setPosition(worldPosition.x + offsetFromWorldPosition.x, worldPosition.y + offsetFromWorldPosition.y);
	}

	public long getInstanceId() {
		return instanceId;
	}

	public ParticleEffect getWrappedInstance() {
		return wrappedInstance;
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
			this.setWorldPosition(getWorldPosition().cpy().add(type.getOffsetFromParentEntity()));
		}
	}

}
