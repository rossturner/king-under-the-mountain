package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.DeathReason;
import technology.rocketjump.undermount.entities.model.physical.creature.status.StatusEffect;

public class StatusMessage {

	public final Entity entity;
	public final Class<? extends StatusEffect> statusClass;
	public final DeathReason deathReason;

	public StatusMessage(Entity entity, Class<? extends StatusEffect> statusClass, DeathReason deathReason) {
		this.entity = entity;
		this.statusClass = statusClass;
		this.deathReason = deathReason;
	}

}
