package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.mapping.tile.MapTile;

import java.util.Set;

public class RoofCollapseMessage {

	public final Set<MapTile> tilesToCollapseConstructedRoofing;

	public RoofCollapseMessage(Set<MapTile> tilesToCollapseConstructedRoofing) {
		this.tilesToCollapseConstructedRoofing = tilesToCollapseConstructedRoofing;
	}
}
