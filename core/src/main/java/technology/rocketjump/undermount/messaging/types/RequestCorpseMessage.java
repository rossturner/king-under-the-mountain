package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.model.Entity;

public class RequestCorpseMessage {

	public final Entity requestingEntity;
	public final Vector2 requesterPosition;
	public final CorpseFoundCallback callback;

	public RequestCorpseMessage(Entity requestingEntity, Vector2 requesterPosition, CorpseFoundCallback callback) {
		this.requestingEntity = requestingEntity;
		this.requesterPosition = requesterPosition;
		this.callback = callback;
	}

	public interface CorpseFoundCallback {

		void corpseFound(Entity entity);

	}
}
