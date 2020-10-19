package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.model.Entity;

public class EntityPositionChangedMessage {

	public final Entity movingEntity;
	public final Vector2 oldPosition;
	public final Vector2 newPosition;

	public EntityPositionChangedMessage(Entity movingEntity, Vector2 oldPosition, Vector2 newPosition) {
		this.movingEntity = movingEntity;
		this.oldPosition = oldPosition;
		this.newPosition = newPosition;
	}

}
