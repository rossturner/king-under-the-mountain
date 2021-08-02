package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.materials.model.GameMaterial;

public class OxidisationMessage {

	public final Entity targetEntity;
	public final GameMaterial oxidisedMaterial;

	public OxidisationMessage(Entity targetEntity, GameMaterial oxidisedMaterial) {
		this.targetEntity = targetEntity;
		this.oxidisedMaterial = oxidisedMaterial;
	}
}
