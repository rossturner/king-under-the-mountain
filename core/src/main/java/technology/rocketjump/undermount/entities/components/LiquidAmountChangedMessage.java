package technology.rocketjump.undermount.entities.components;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.materials.model.GameMaterial;

public class LiquidAmountChangedMessage {

	public final Entity parentEntity;
	public final GameMaterial liquidMaterial;
	public final float oldQuantity;
	public final float newQuantity;

	public LiquidAmountChangedMessage(Entity parentEntity, GameMaterial liquidMaterial, float oldQuantity, float newQuantity) {
		this.parentEntity = parentEntity;
		this.liquidMaterial = liquidMaterial;
		this.oldQuantity = oldQuantity;
		this.newQuantity = newQuantity;
	}
}
