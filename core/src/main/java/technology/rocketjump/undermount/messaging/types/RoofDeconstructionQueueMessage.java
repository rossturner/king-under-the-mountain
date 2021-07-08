package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.mapping.tile.MapTile;

public class RoofDeconstructionQueueMessage {
	public final MapTile parentTile;
	public final boolean roofDeconstructionQueued;

	public RoofDeconstructionQueueMessage(MapTile parentTile, boolean roofDeconstructionQueued) {
		this.parentTile = parentTile;
		this.roofDeconstructionQueued = roofDeconstructionQueued;
	}
}
