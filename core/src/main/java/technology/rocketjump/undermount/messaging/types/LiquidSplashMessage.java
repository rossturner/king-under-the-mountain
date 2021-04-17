package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.materials.model.GameMaterial;

public class LiquidSplashMessage {

	public final Entity targetEntity;
	public final GameMaterial liquidMaterial;

	public LiquidSplashMessage(Entity targetEntity, GameMaterial liquidMaterial) {
		this.targetEntity = targetEntity;
		this.liquidMaterial = liquidMaterial;
	}
}
