package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.undermount.mapping.tile.MapTile;

public class MechanismPlacementMessage {
	public final MapTile mapTile;
	public final MechanismType mechanismType;

	public MechanismPlacementMessage(MapTile mapTile, MechanismType mechanismType) {
		this.mapTile = mapTile;
		this.mechanismType = mechanismType;
	}
}
