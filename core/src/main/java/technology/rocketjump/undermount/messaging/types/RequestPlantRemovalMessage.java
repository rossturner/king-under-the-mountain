package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.entities.model.Entity;

public class RequestPlantRemovalMessage {

	public final Entity plantEntityToRemove;
	public final GridPoint2 tileLocation;
	public final JobCreatedCallback callback;

	public RequestPlantRemovalMessage(Entity plantEntityToRemove, GridPoint2 tileLocation, JobCreatedCallback callback) {
		this.plantEntityToRemove = plantEntityToRemove;
		this.tileLocation = tileLocation;
		this.callback = callback;
	}

	public GridPoint2 getTileLocation() {
		return tileLocation;
	}

	public Entity getPlantEntityToRemove() {
		return plantEntityToRemove;
	}

}
