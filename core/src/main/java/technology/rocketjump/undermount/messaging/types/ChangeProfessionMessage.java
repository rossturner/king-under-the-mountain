package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.jobs.model.Profession;

public class ChangeProfessionMessage {

	public final Entity entity;
	public final Profession professionToReplace;
	public final Profession newProfession;

	public ChangeProfessionMessage(Entity entity, Profession professionToReplace, Profession newProfession) {
		this.entity = entity;
		this.professionToReplace = professionToReplace;
		this.newProfession = newProfession;
	}
}
