package technology.rocketjump.undermount.settlement;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.components.furniture.ConstructedEntityComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.tags.DeceasedContainerTag;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.jobs.JobTypeDictionary;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rooms.HaulingAllocation;

import static technology.rocketjump.undermount.entities.behaviour.furniture.CraftingStationBehaviour.getAnyNavigableWorkspace;
import static technology.rocketjump.undermount.entities.model.EntityType.CREATURE;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.rooms.HaulingAllocation.AllocationPositionType.FLOOR;
import static technology.rocketjump.undermount.rooms.HaulingAllocation.AllocationPositionType.FURNITURE;

/**
 * This class is responsible for assigning and moving corpses to graves
 */
@Singleton
public class GraveManager implements Updatable {

	private static final float TIME_BETWEEN_INFREQUENT_UPDATE_SECONDS = 3.4f;

	private final MessageDispatcher messageDispatcher;
	private final FurnitureTracker furnitureTracker;
	private final SettlerTracker settlerTracker;
	private final JobStore jobStore;
	private final JobType haulingJobType;

	private GameContext gameContext;
	private float timeSinceLastUpdate = 0f;

	@Inject
	public GraveManager(MessageDispatcher messageDispatcher, FurnitureTracker furnitureTracker,
						SettlerTracker settlerTracker, JobStore jobStore, JobTypeDictionary jobTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.furnitureTracker = furnitureTracker;
		this.settlerTracker = settlerTracker;
		this.jobStore = jobStore;
		this.haulingJobType = jobTypeDictionary.getByName("HAULING");
	}

	@Override
	public void update(float deltaTime) {
		timeSinceLastUpdate += deltaTime;
		if (gameContext != null && timeSinceLastUpdate > TIME_BETWEEN_INFREQUENT_UPDATE_SECONDS) {
			timeSinceLastUpdate = 0f;

			for (Entity deceased : settlerTracker.getDead()) {
				if (deceased.getLocationComponent().getContainerEntity() == null && !hasCorpseHaulingJob(deceased) && isUnallocated(deceased)) {
					Entity deceasedContainer = findAvailableDeceasedContainer();

					if (deceasedContainer == null) {
						// No graves/sarcophagi available, try again next update
						break;
					} else {
						assignAndCreateHaulingJob(deceased, deceasedContainer);
					}
				}
			}
		}
	}

	private boolean isUnallocated(Entity deceased) {
		ItemAllocationComponent itemAllocationComponent = deceased.getComponent(ItemAllocationComponent.class);
		return  itemAllocationComponent == null || itemAllocationComponent.getNumUnallocated() > 0;
	}

	private boolean hasCorpseHaulingJob(Entity deceased) {
		GridPoint2 location = toGridPoint(deceased.getLocationComponent().getWorldOrParentPosition());
		if (location == null) {
			// FIXME When does this happen? Returning true for now so nothing else tries to access this corpse
			return true;
		}

		for (Job jobAtLocation : jobStore.getJobsAtLocation(location)) {
			if (jobAtLocation.getType().equals(haulingJobType) && jobAtLocation.getHaulingAllocation().getHauledEntityType().equals(CREATURE)) {
				return true;
			}
		}
		return false;
	}

	private Entity findAvailableDeceasedContainer() {
		for (Entity deceasedContainer : furnitureTracker.findByTag(DeceasedContainerTag.class, true)) {
			ConstructedEntityComponent constructedEntityComponent = deceasedContainer.getComponent(ConstructedEntityComponent.class);
			if (constructedEntityComponent != null && constructedEntityComponent.isBeingDeconstructed()) {
				continue;
			}
			if (getAnyNavigableWorkspace(deceasedContainer, gameContext.getAreaMap()) != null) {
				return deceasedContainer;
			}
		}
		return null;
	}

	private void assignAndCreateHaulingJob(Entity deceased, Entity deceasedContainer) {

		GridPoint2 deceasedPosition = toGridPoint(deceased.getLocationComponent().getWorldPosition());
		MapTile deceasedTile = gameContext.getAreaMap().getTile(deceasedPosition);
		if (deceasedTile == null || !deceasedTile.isNavigable()) {
			// Skip while not navigable
			return;
		}

		FurnitureEntityAttributes furnitureAttributes = (FurnitureEntityAttributes) deceasedContainer.getPhysicalEntityComponent().getAttributes();
		furnitureAttributes.setAssignedToEntityId(deceased.getId());

		HaulingAllocation allocation = new HaulingAllocation();
		// Assuming always collecting from floor
		allocation.setSourcePosition(deceasedPosition);
		allocation.setSourcePositionType(FLOOR);

		allocation.setHauledEntityType(CREATURE);
		allocation.setHauledEntityId(deceased.getId());

		allocation.setTargetId(deceasedContainer.getId());
		allocation.setTargetPositionType(FURNITURE);
		allocation.setTargetPosition(toGridPoint(deceasedContainer.getLocationComponent().getWorldOrParentPosition()));


		Job haulingJob = new Job(haulingJobType);
		haulingJob.setJobPriority(JobPriority.HIGHER);// Might prefer to set priority based on grave room priority
		haulingJob.setTargetId(allocation.getHauledEntityId());
		haulingJob.setHaulingAllocation(allocation);
		haulingJob.setJobLocation(allocation.getSourcePosition());
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJob);
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		gameContext = null;
		timeSinceLastUpdate = 0f;
	}
}
