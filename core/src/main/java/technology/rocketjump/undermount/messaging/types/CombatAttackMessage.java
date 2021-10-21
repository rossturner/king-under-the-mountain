package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;

public class CombatAttackMessage {

	public final Entity attackerEntity;
	public final Entity defenderEntity;
	public final ItemType weaponItemType;
	public final ItemEntityAttributes ammoAttributes;

	public CombatAttackMessage(Entity attackerEntity, Entity defenderEntity, ItemType weaponItemType, ItemEntityAttributes ammoAttributes) {
		this.attackerEntity = attackerEntity;
		this.defenderEntity = defenderEntity;
		this.weaponItemType = weaponItemType;
		this.ammoAttributes = ammoAttributes;
	}
}
