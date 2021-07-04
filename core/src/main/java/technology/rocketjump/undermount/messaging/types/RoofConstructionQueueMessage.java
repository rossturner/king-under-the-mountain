package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.mapping.tile.MapTile;

public class RoofConstructionQueueMessage {
	public final MapTile parentTile;
	public final boolean roofConstructionQueued;

	public RoofConstructionQueueMessage(MapTile parentTile, boolean roofConstructionQueued) {
		this.parentTile = parentTile;
		this.roofConstructionQueued = roofConstructionQueued;
	}
}
