package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.mapping.tile.MapTile;

public class RemoveDesignationMessage {

	private final MapTile targetTile;
	private final Entity targetEntity;

	public RemoveDesignationMessage(MapTile targetTile) {
		this.targetTile = targetTile;
		this.targetEntity = null;
	}

	public RemoveDesignationMessage(Entity targetEntity) {
		this.targetTile = null;
		this.targetEntity = targetEntity;
	}

	public MapTile getTargetTile() {
		return targetTile;
	}

	public Entity getTargetEntity() {
		return targetEntity;
	}

}
