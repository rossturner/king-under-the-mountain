package technology.rocketjump.undermount.entities.model.physical;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemHoldPosition;

public class AttachedEntity {

	public final Entity entity;
	public final ItemHoldPosition holdPosition;

	public AttachedEntity(Entity entity, ItemHoldPosition holdPosition) {
		this.holdPosition = holdPosition;
		this.entity = entity;
	}
}
