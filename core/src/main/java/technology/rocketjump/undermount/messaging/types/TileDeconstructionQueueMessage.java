package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.mapping.tile.MapTile;

public class TileDeconstructionQueueMessage {
	public final MapTile parentTile;
	public final boolean deconstructionQueued;

	public TileDeconstructionQueueMessage(MapTile parentTile, boolean deconstructionQueued) {
		this.parentTile = parentTile;
		this.deconstructionQueued = deconstructionQueued;
	}
}
