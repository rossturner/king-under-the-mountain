package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.humanoid.DeathReason;

public class HumanoidDeathMessage {

	public final Entity deceased;
	public final DeathReason reason;

	public HumanoidDeathMessage(Entity deceased, DeathReason reason) {
		this.deceased = deceased;
		this.reason = reason;
	}

}
