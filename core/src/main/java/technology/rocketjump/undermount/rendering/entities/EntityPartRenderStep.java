package technology.rocketjump.undermount.rendering.entities;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.assets.entities.model.SpriteDescriptor;
import technology.rocketjump.undermount.entities.model.Entity;

import static technology.rocketjump.undermount.rendering.entities.EntityRenderer.PIXELS_PER_TILE;

public class EntityPartRenderStep {

	private final Vector2 offsetFromEntity;
	private final SpriteDescriptor spriteDescriptor;
	private final Entity entity;

	private Entity otherEntity;

	public EntityPartRenderStep(SpriteDescriptor spriteDescriptor, Vector2 parentWorldPosition, Entity entity) {
		this.entity = entity;
		// FIXME parentWorldPosition is updated here, and the updated value is what's required for children in EntityRenderer
		// FIXME so it is NOT cloned when passed in but is updated by reference
		if (spriteDescriptor != null) {
			this.offsetFromEntity = parentWorldPosition.add(spriteDescriptor.getOffsetPixels().cpy().scl(1f / PIXELS_PER_TILE));
		} else {
			this.offsetFromEntity = parentWorldPosition.cpy();
		}
		this.spriteDescriptor = spriteDescriptor;
	}

	public Vector2 getOffsetFromEntity() {
		return offsetFromEntity;
	}

	public SpriteDescriptor getSpriteDescriptor() {
		return spriteDescriptor;
	}

	public boolean isAnotherEntity() {
		return otherEntity != null;
	}

	public Entity getOtherEntity() {
		return otherEntity;
	}

	public void setOtherEntity(Entity otherEntity) {
		this.otherEntity = otherEntity;
	}

	public Entity getEntity() {
		return entity;
	}
}
