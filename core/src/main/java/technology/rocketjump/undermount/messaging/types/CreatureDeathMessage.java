package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.DeathReason;

public class CreatureDeathMessage {

	public final Entity deceased;
	public final DeathReason reason;

	public CreatureDeathMessage(Entity deceased, DeathReason reason) {
		this.deceased = deceased;
		if (reason == null) {
			this.reason = DeathReason.UNKNOWN;
		} else {
			this.reason = reason;
		}
	}

}
