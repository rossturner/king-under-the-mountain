package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rooms.RoomTile;
import technology.rocketjump.undermount.rooms.RoomType;
import technology.rocketjump.undermount.rooms.constructions.WallConstruction;

import java.util.List;
import java.util.Map;

public class WallsPlacementMessage {

	public final List<WallConstruction> wallConstructions;

	public WallsPlacementMessage(List<WallConstruction> wallConstructions) {
		this.wallConstructions = wallConstructions;
	}
}
