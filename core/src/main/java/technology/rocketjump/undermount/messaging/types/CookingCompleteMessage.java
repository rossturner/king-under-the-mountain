package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.cooking.model.CookingRecipe;
import technology.rocketjump.undermount.entities.model.Entity;

public class CookingCompleteMessage {
	public final Entity targetFurnitureEntity;
	public final CookingRecipe cookingRecipe;

	public CookingCompleteMessage(Entity targetFurnitureEntity, CookingRecipe cookingRecipe) {
		this.targetFurnitureEntity = targetFurnitureEntity;
		this.cookingRecipe = cookingRecipe;
	}
}
