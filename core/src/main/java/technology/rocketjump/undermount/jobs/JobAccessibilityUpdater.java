package technology.rocketjump.undermount.jobs;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.EntityStore;
import technology.rocketjump.undermount.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobState;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileNeighbours;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static technology.rocketjump.undermount.jobs.model.JobState.INACCESSIBLE;

/**
 * This class is mostly responsible for switching potentially accessible jobs to assignable jobs
 */
@Singleton
public class JobAccessibilityUpdater implements Updatable {

	public static final float TIME_BETWEEN_INACCESSIBLE_RETRIES = 3.143f;

	private final JobStore jobStore;
	private final EntityStore entityStore;

	private GameContext gameContext;
	private float timeSinceLastInaccessibleUpdate = 0f;

	@Inject
	public JobAccessibilityUpdater(JobStore jobStore, EntityStore entityStore) {
		this.jobStore = jobStore;
		this.entityStore = entityStore;
	}

	/**
	 * This method works through one potentially accessible job per frame
	 * @param deltaTime
	 */
	@Override
	public void update(float deltaTime) {
		if (gameContext != null) {
			timeSinceLastInaccessibleUpdate += deltaTime;
			if (timeSinceLastInaccessibleUpdate > TIME_BETWEEN_INACCESSIBLE_RETRIES) {
				timeSinceLastInaccessibleUpdate = 0f;

				Job inaccessibleJob = jobStore.getCollectionByState(INACCESSIBLE).next();
				if (inaccessibleJob != null) {
					jobStore.switchState(inaccessibleJob, JobState.POTENTIALLY_ACCESSIBLE);
				}
			}

			checkNextPotentiallyAccessible();
		}
	}

	private void checkNextPotentiallyAccessible() {
		Job potentiallyAccessibleJob = jobStore.getCollectionByState(JobState.POTENTIALLY_ACCESSIBLE).next();
		if (potentiallyAccessibleJob == null) {
			// No outstanding potentially accessible jobs
			return;
		}
		Entity assignableEntity = getEntityToPathfindFrom(potentiallyAccessibleJob);
		if (assignableEntity == null) {
			// No entities to assign to
			return;
		}
		Vector2 entityWorldPosition = assignableEntity.getLocationComponent().getWorldOrParentPosition();

		List<GridPoint2> jobLocations = new ArrayList<>();


		if (potentiallyAccessibleJob.getType().isAccessedFromAdjacentTile()) {
			TileNeighbours jobNeighbourTiles = gameContext.getAreaMap().getOrthogonalNeighbours(potentiallyAccessibleJob.getJobLocation().x, potentiallyAccessibleJob.getJobLocation().y);
			for (CompassDirection compassDirection : jobNeighbourTiles.keySet()) {
				if (!jobNeighbourTiles.get(compassDirection).isNavigable()) {
					jobNeighbourTiles.remove(compassDirection);
				}
			}
			if (jobNeighbourTiles.isEmpty()) {
				// None of the adjacent tiles were accessible, so this job is actually inaccessible now
				jobStore.switchState(potentiallyAccessibleJob, INACCESSIBLE);
				return;
			} else {
				for (MapTile mapTile : jobNeighbourTiles.values()) {
					jobLocations.add(mapTile.getTilePosition());
				}
				Collections.shuffle(jobLocations);
			}
		} else {
			jobLocations.add(potentiallyAccessibleJob.getJobLocation());
		}

		if (isLocationNavigable(jobLocations, entityWorldPosition)) {
			jobStore.switchState(potentiallyAccessibleJob, JobState.ASSIGNABLE);
		} else {
			jobStore.switchState(potentiallyAccessibleJob, JobState.INACCESSIBLE);
		}
	}

	@Override
	public boolean runWhilePaused() {
		return true;
	}

	private Entity getEntityToPathfindFrom(Job job) {
		List<Entity> candidates = new ArrayList<>();
		for (Entity jobAssignableEntity : entityStore.getJobAssignableEntities()) {
			ProfessionsComponent professionsComponent = jobAssignableEntity.getComponent(ProfessionsComponent.class);
			if (professionsComponent != null && professionsComponent.hasActiveProfession(job.getRequiredProfession())) {
				candidates.add(jobAssignableEntity);
			}
		}

		if (candidates.isEmpty()) {
			return null;
		} else {
			return candidates.get(gameContext.getRandom().nextInt(candidates.size()));
		}
	}

	private boolean isLocationNavigable(List<GridPoint2> locations, Vector2 entityWorldPosition) {
		if (locations.isEmpty()) {
			return false;
		} else {
			GridPoint2 locationToTry = locations.get(gameContext.getRandom().nextInt(locations.size()));

			MapTile originTile = gameContext.getAreaMap().getTile(entityWorldPosition);
			MapTile targetTile = gameContext.getAreaMap().getTile(locationToTry);

			// Just checking if job is in same region
			return originTile != null && targetTile != null && originTile.getRegionId() == targetTile.getRegionId();
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
	}

}
