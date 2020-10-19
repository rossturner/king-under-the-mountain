package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.materials.model.GameMaterial;

public class ChangeFloorMessage {

	public final GridPoint2 targetLocation;
	public final FloorType newFloorType;
	public final GameMaterial newMaterial;

	public ChangeFloorMessage(GridPoint2 targetLocation, FloorType newFloorType, GameMaterial newMaterial) {
		this.targetLocation = targetLocation;
		this.newFloorType = newFloorType;
		this.newMaterial = newMaterial;
	}
}
