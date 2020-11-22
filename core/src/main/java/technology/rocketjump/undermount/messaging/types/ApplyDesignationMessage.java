package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.designation.TileDesignation;
import technology.rocketjump.undermount.ui.GameInteractionMode;

public class ApplyDesignationMessage {

	private final MapTile targetTile;
	private final TileDesignation designationToApply;
	private final GameInteractionMode interactionMode;

	public ApplyDesignationMessage(MapTile targetTile, TileDesignation designationToApply, GameInteractionMode interactionMode) {
		this.targetTile = targetTile;
		this.designationToApply = designationToApply;
		this.interactionMode = interactionMode;
	}

	public MapTile getTargetTile() {
		return targetTile;
	}

	public TileDesignation getDesignationToApply() {
		return designationToApply;
	}

	public GameInteractionMode getInteractionMode() {
		return interactionMode;
	}
}
