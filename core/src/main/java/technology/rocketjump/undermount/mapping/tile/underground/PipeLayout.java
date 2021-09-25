package technology.rocketjump.undermount.mapping.tile.underground;

import technology.rocketjump.undermount.mapping.tile.TileNeighbours;
import technology.rocketjump.undermount.mapping.tile.layout.TileLayout;

public class PipeLayout extends TileLayout {

	public PipeLayout(TileNeighbours neighbours) {
		super(neighbours, (tile, direction) -> {
			if (direction.isDiagonal()) {
				// diagonals do not affect pipes
				return false;
			} else {
				return tile.hasPipe();
			}
		});
	}

	public PipeLayout(int id) {
		super(id);
	}
}
