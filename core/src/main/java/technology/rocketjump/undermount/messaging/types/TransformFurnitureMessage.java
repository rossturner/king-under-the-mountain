package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;

public class TransformFurnitureMessage {

	public final Entity furnitureEntity;
	public final FurnitureType transformToFurnitureType;

	public TransformFurnitureMessage(Entity furnitureEntity, FurnitureType transformToFurnitureType) {
		this.furnitureEntity = furnitureEntity;
		this.transformToFurnitureType = transformToFurnitureType;
	}

}
