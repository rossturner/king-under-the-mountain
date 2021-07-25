package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.tags.Tag;

import java.util.List;

public class LookupItemTypesByTagClassMessage {
	public final Class<? extends Tag> tagClass;
	public final LookupItemTypesCallback callback;

	public LookupItemTypesByTagClassMessage(Class<? extends Tag> tagClass, LookupItemTypesCallback callback) {
		this.tagClass = tagClass;
		this.callback = callback;
	}

	public interface LookupItemTypesCallback {

		void itemTypesFound(List<ItemType> itemTypes);

	}
}
