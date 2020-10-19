package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.rooms.constructions.Construction;

public class TransformConstructionMessage {

	public final Construction construction;
	public final FurnitureType transformToFurnitureType;

	public TransformConstructionMessage(Construction construction, FurnitureType transformToFurnitureType) {
		this.construction = construction;
		this.transformToFurnitureType = transformToFurnitureType;
	}

}
