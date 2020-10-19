package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.designation.TileDesignation;

public class ApplyDesignationMessage {

	private final MapTile targetTile;
	private final TileDesignation designationToApply;

	public ApplyDesignationMessage(MapTile targetTile, TileDesignation designationToApply) {
		this.targetTile = targetTile;
		this.designationToApply = designationToApply;
	}

	public MapTile getTargetTile() {
		return targetTile;
	}

	public TileDesignation getDesignationToApply() {
		return designationToApply;
	}
}
