package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.ai.goap.actions.ItemTypeLookupCallback;
import technology.rocketjump.undermount.entities.model.EntityType;

public class LookupMessage {
    public final EntityType entityType;
    public final String typeName;
    public final ItemTypeLookupCallback callback;

    public LookupMessage(EntityType entityType, String typeName, ItemTypeLookupCallback callback) {
        this.entityType = entityType;
        this.typeName = typeName;
        this.callback = callback;
    }
}
