package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.materials.model.GameMaterial;

public class PipeConstructionMessage {
	public final GridPoint2 tilePosition;
	public final GameMaterial material;

	public PipeConstructionMessage(GridPoint2 tilePosition, GameMaterial material) {
		this.tilePosition = tilePosition;
		this.material = material;
	}
}
