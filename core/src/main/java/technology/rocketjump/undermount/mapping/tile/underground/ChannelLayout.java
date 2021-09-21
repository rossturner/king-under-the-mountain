package technology.rocketjump.undermount.mapping.tile.underground;

import technology.rocketjump.undermount.mapping.tile.TileNeighbours;
import technology.rocketjump.undermount.mapping.tile.layout.TileLayout;

public class ChannelLayout extends TileLayout {

	public ChannelLayout(TileNeighbours neighbours) {
		super(neighbours, (tile, direction) ->
				tile.hasChannel()
		);
	}

	public ChannelLayout(int id) {
		super(id);
	}
}
