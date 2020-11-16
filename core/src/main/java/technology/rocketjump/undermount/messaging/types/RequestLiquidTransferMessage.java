package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.jobs.model.Profession;
import technology.rocketjump.undermount.materials.model.GameMaterial;

public class RequestLiquidTransferMessage {

	public final GameMaterial targetLiquidMaterial;
	public final boolean allowConstructedZones;
	public final Entity requesterEntity;
	public final Vector2 requesterPosition;
	public final ItemType liquidContainerItemType;
	public final JobCreatedCallback jobCreatedCallback;
	public final Profession requiredProfession;
	public final JobPriority jobPriority;

	public RequestLiquidTransferMessage(GameMaterial targetLiquidMaterial, boolean allowConstructedZones, Entity requesterEntity, Vector2 requesterPosition,
										ItemType liquidContainerItemType, Profession requiredProfession, JobPriority jobPriority, JobCreatedCallback jobCreatedCallback) {
		this.targetLiquidMaterial = targetLiquidMaterial;
		this.allowConstructedZones = allowConstructedZones;
		this.requesterEntity = requesterEntity;
		this.requesterPosition = requesterPosition;
		this.liquidContainerItemType = liquidContainerItemType;
		this.jobCreatedCallback = jobCreatedCallback;
		this.requiredProfession = requiredProfession;
		this.jobPriority = jobPriority;
	}
}
