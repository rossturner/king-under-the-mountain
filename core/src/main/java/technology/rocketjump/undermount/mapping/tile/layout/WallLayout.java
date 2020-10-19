package technology.rocketjump.undermount.mapping.tile.layout;

import technology.rocketjump.undermount.mapping.tile.TileNeighbours;

public class WallLayout extends TileLayout {

    public WallLayout(TileNeighbours neighbours) {
        super(neighbours, (tile, direction) ->
                tile.hasWall() || (tile.hasDoorway() && direction.getYOffset() >= 0) // Don't count doorways below this Y level as they don't have North wall caps
        );
    }

    public WallLayout(int id) {
        super(id);
    }

    public static TileLayout fromString(String diagram) {
        return TileLayout.fromString(diagram, (tile, direction) -> tile.hasWall());
    }

}
