package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.designation.TileDesignation;

public class RemoveDesignationMessage {

	private final MapTile targetTile;
	private final TileDesignation designationToRemove;

	public RemoveDesignationMessage(MapTile targetTile, TileDesignation designationToRemove) {
		this.targetTile = targetTile;
		this.designationToRemove = designationToRemove;
	}

	public MapTile getTargetTile() {
		return targetTile;
	}

	public TileDesignation getDesignationToRemove() {
		return designationToRemove;
	}
}
