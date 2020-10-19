package technology.rocketjump.undermount.mapping.tile.layout;

import technology.rocketjump.undermount.mapping.tile.TileNeighbours;

import static technology.rocketjump.undermount.rooms.constructions.ConstructionType.DOORWAY_CONSTRUCTION;
import static technology.rocketjump.undermount.rooms.constructions.ConstructionType.WALL_CONSTRUCTION;

public class WallConstructionLayout extends TileLayout {

    public WallConstructionLayout(TileNeighbours neighbours) {
        super(neighbours, (tile, direction) ->
                tile.hasWall() || (tile.hasDoorway() && direction.getYOffset() >= 0) || // Don't count doorways below this Y level as they don't have North wall caps
                // Also check for wall or doorway constructions as these affect construction layout, but not real wall layout
                (tile.hasConstruction() && tile.getConstruction().getConstructionType().equals(WALL_CONSTRUCTION)) ||
                (tile.hasConstruction() && tile.getConstruction().getConstructionType().equals(DOORWAY_CONSTRUCTION) && direction.getYOffset() >= 0)
        );
    }

    public WallConstructionLayout(int id) {
        super(id);
    }

}
