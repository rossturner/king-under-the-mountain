package technology.rocketjump.undermount.mapping.tile.underground;

import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileNeighbours;
import technology.rocketjump.undermount.mapping.tile.layout.TileLayout;
import technology.rocketjump.undermount.mapping.tile.layout.WallLayout;

import java.util.LinkedHashSet;
import java.util.Set;

@Singleton
public class PipeLayoutAtlas {

	int[] simplifiedLayoutArray = new int[256];
	Set<Integer> uniqueLayouts = new LinkedHashSet();

    public PipeLayoutAtlas() {
		Entity pipeEntity = new Entity();
        for (int layoutId = 0; layoutId < 256; layoutId++) {
            TileLayout layoutForId = new WallLayout(layoutId);

			TileNeighbours neighbours = new TileNeighbours();
			for (CompassDirection direction : CompassDirection.values()) {
				MapTile tileInDirection = new MapTile(0, 0,0, null, null);
				if (layoutForId.meaningfulTileInDirection(direction)) {
					UnderTile underTile = new UnderTile();
					underTile.setPipeEntity(pipeEntity);
					tileInDirection.setUnderTile(underTile);
				}
				neighbours.put(direction, tileInDirection);
			}

			PipeLayout pipeLayout = new PipeLayout(neighbours);
			uniqueLayouts.add(pipeLayout.getId());
		}
    }

    public int simplifyLayoutId(int originalLayoutId) {
        return simplifiedLayoutArray[originalLayoutId];
    }

	public Set<Integer> getUniqueLayouts() {
		return uniqueLayouts;
	}
}
