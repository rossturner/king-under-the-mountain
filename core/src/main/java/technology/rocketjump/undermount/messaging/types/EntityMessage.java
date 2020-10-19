package technology.rocketjump.undermount.messaging.types;

public class EntityMessage {

	private final long entityId;

	public EntityMessage(long entityId) {
		this.entityId = entityId;
	}

	public long getEntityId() {
		return entityId;
	}
}
