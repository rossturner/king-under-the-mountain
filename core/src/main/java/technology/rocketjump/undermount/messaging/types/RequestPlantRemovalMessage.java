package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.jobs.model.JobPriority;

public class RequestPlantRemovalMessage {

	public final Entity plantEntityToRemove;
	public final GridPoint2 tileLocation;
	public final JobPriority jobPriority;
	public final JobCreatedCallback callback;

	public RequestPlantRemovalMessage(Entity plantEntityToRemove, GridPoint2 tileLocation, JobPriority jobPriority, JobCreatedCallback callback) {
		this.plantEntityToRemove = plantEntityToRemove;
		this.tileLocation = tileLocation;
		this.jobPriority = jobPriority;
		this.callback = callback;
	}

	public GridPoint2 getTileLocation() {
		return tileLocation;
	}

	public Entity getPlantEntityToRemove() {
		return plantEntityToRemove;
	}

}
