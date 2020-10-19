package technology.rocketjump.undermount.mapping.tile;

import static technology.rocketjump.undermount.assets.model.FloorType.NULL_FLOOR;
import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;

public class NullMapTile extends MapTile {

	public NullMapTile(int tileX, int tileY) {
		super(0L, tileX, tileY, NULL_FLOOR, NULL_MATERIAL);
	}

	@Override
	public boolean isNavigable() {
		return false;
	}

	@Override
	public boolean isNavigable(MapTile startingPoint) {
		return false;
	}


}
