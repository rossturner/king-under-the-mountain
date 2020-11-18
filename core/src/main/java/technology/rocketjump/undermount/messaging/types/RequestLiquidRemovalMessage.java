package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.jobs.model.JobPriority;

public class RequestLiquidRemovalMessage {

	public final Entity requesterEntity;
	public final GridPoint2 workspaceLocation;
	public final float quantity;
	public final ItemType containerItemType;
	public final JobCreatedCallback callback;
	public final JobPriority jobPriority;

	public RequestLiquidRemovalMessage(Entity requesterEntity, GridPoint2 workspaceLocation, float quantity, ItemType containerItemType, JobPriority jobPriority, JobCreatedCallback callback) {
		this.requesterEntity = requesterEntity;
		this.workspaceLocation = workspaceLocation;
		this.quantity = quantity;
		this.containerItemType = containerItemType;
		this.callback = callback;
		this.jobPriority = jobPriority;
	}
}
