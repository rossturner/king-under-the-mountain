package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;

public class ChangeWeaponSelectionMessage {

	public final Entity entity;
	public final ItemType selectedWeaponType;

	public ChangeWeaponSelectionMessage(Entity entity, ItemType selectedWeaponType) {
		this.entity = entity;
		this.selectedWeaponType = selectedWeaponType;
	}
}
