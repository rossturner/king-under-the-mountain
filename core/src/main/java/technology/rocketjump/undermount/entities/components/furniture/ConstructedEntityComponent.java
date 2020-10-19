package technology.rocketjump.undermount.entities.components.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.tags.DeceasedContainerTag;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class ConstructedEntityComponent implements ParentDependentEntityComponent {

	private Entity parentEntity;
	private Job deconstructionJob = null;
	private boolean isAutoConstructed = false;

	public boolean isBeingDeconstructed() {
		return deconstructionJob != null;
	}

	public void setDeconstructionJob(Job job) {
		deconstructionJob = job;
	}

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		ConstructedEntityComponent cloned = new ConstructedEntityComponent();
		cloned.deconstructionJob = null;
		cloned.isAutoConstructed = this.isAutoConstructed;
		return cloned;
	}

	public boolean isAutoConstructed() {
		return isAutoConstructed;
	}

	public void setAutoConstructed(boolean autoConstructed) {
		isAutoConstructed = autoConstructed;
	}

	public boolean canBeDeconstructed() {
		if (parentEntity.getType().equals(EntityType.FURNITURE)) {
			FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
			if (attributes.getAssignedToEntityId() != null && parentEntity.getTag(DeceasedContainerTag.class) != null) {
				// Can't deconstruct deceased containers that are assigned
				return false;
			} else {
				// Currently everything else can be deconstructed
				return true;
			}
		} else {
			Logger.warn("Not yet implemented: canBeDeconstructed() for " + parentEntity.getType());
			return true;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (deconstructionJob != null) {
			deconstructionJob.writeTo(savedGameStateHolder);
			asJson.put("deconstructionJobId", deconstructionJob.getJobId());
		}

		if (isAutoConstructed) {
			asJson.put("autoConstructed", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		Long deconstructionJobId = asJson.getLong("deconstructionJobId");
		if (deconstructionJobId != null) {
			this.deconstructionJob = savedGameStateHolder.jobs.get(deconstructionJobId);
			if (this.deconstructionJob == null) {
				throw new InvalidSaveException("Could not find job with ID " + deconstructionJobId);
			}
		}

		this.isAutoConstructed = asJson.getBooleanValue("autoConstructed");
	}
}
