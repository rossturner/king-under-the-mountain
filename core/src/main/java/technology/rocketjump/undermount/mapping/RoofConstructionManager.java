package technology.rocketjump.undermount.mapping;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.constants.ConstantsRepo;
import technology.rocketjump.undermount.entities.tags.SupportsRoofTag;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.jobs.JobTypeDictionary;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.roof.RoofConstructionState;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RoofCollapseMessage;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

import static technology.rocketjump.undermount.mapping.tile.roof.RoofConstructionState.*;

@Singleton
public class RoofConstructionManager implements GameContextAware {

	private static final int EXPLORED_TOO_MANY = 300;
	private final MessageDispatcher messageDispatcher;
	private GameContext gameContext;
	private final int ROOF_SUPPORT_MAX_DISTANCE;
	private final JobType constructRoofingJobType;
	private final JobType deconstructRoofingJobType;
	private final JobStore jobStore;

	@Inject
	public RoofConstructionManager(ConstantsRepo constantsRepo, JobTypeDictionary jobTypeDictionary,
								   MessageDispatcher messageDispatcher, JobStore jobStore) {
		this.messageDispatcher = messageDispatcher;
		ROOF_SUPPORT_MAX_DISTANCE = constantsRepo.getWorldConstants().getRoofSupportMaxDistance();
		this.jobStore = jobStore;

		constructRoofingJobType = jobTypeDictionary.getByName(constantsRepo.getSettlementConstants().getConstructRoofingJobType());
		if (constructRoofingJobType == null) {
			throw new RuntimeException("Could not find job with name " + constantsRepo.getSettlementConstants().getConstructRoofingJobType() + " from " + constantsRepo.getSettlementConstants().getClass().getSimpleName());
		}

		deconstructRoofingJobType = jobTypeDictionary.getByName(constantsRepo.getSettlementConstants().getDeconstructRoofingJobType());
		if (deconstructRoofingJobType == null) {
			throw new RuntimeException("Could not find job with name " + constantsRepo.getSettlementConstants().getDeconstructRoofingJobType() + " from " + constantsRepo.getSettlementConstants().getClass().getSimpleName());
		}
	}

	public void roofConstructionAdded(MapTile mapTile) {
		switchState(mapTile, RoofConstructionState.PENDING);

		if (!withinRangeOfSupport(mapTile, false)) {
			switchState(mapTile, TOO_FAR_FROM_SUPPORT);
		} else {
			// is in range of support
			if (hasAdjacentRoof(mapTile) || containsSupport(mapTile)) {
				switchState(mapTile, RoofConstructionState.READY_FOR_CONSTRUCTION);
			} else {
				switchState(mapTile, RoofConstructionState.NO_ADJACENT_ROOF);
			}
		}
	}

	public void roofConstructionRemoved(MapTile mapTile) {
		switchState(mapTile, NONE);
	}

	public void roofDeconstructionAdded(MapTile mapTile) {
		switchState(mapTile, PENDING_DECONSTRUCTION);
	}

	public void roofConstructed(MapTile mapTile) {
		for (CompassDirection direction : CompassDirection.CARDINAL_DIRECTIONS) {
			MapTile tile = gameContext.getAreaMap().getTile(mapTile.getTileX() + direction.getXOffset(), mapTile.getTileY() + direction.getYOffset());
			if (tile != null && tile.getRoof().getConstructionState().equals(NO_ADJACENT_ROOF)) {
				switchState(tile, READY_FOR_CONSTRUCTION);
			}
		}
	}

	public void supportConstructed(MapTile mapTile) {
		for (int yCursor = mapTile.getTileY()-ROOF_SUPPORT_MAX_DISTANCE; yCursor <= mapTile.getTileY()+ROOF_SUPPORT_MAX_DISTANCE; yCursor++) {
			for (int xCursor = mapTile.getTileX()-ROOF_SUPPORT_MAX_DISTANCE; xCursor <= mapTile.getTileX()+ROOF_SUPPORT_MAX_DISTANCE; xCursor++) {
				MapTile tile = gameContext.getAreaMap().getTile(xCursor, yCursor);
				if (tile != null && tile.getRoof().getState().equals(TileRoofState.OPEN) && tile.getRoof().getConstructionState().equals(TOO_FAR_FROM_SUPPORT)) {
					switchState(tile, NO_ADJACENT_ROOF);
					if (hasAdjacentRoof(tile) || (yCursor == mapTile.getTileY() && xCursor == mapTile.getTileX())) {
						switchState(tile, READY_FOR_CONSTRUCTION);
					}
				}
			}
		}
	}

	public void roofDeconstructed(MapTile mapTile) {
		// need to check there are no not-attached sections of roofing
		for (CompassDirection direction : CompassDirection.CARDINAL_DIRECTIONS) {
			Set<MapTile> frontier = new LinkedHashSet<>();
			Set<MapTile> explored = new LinkedHashSet<>();

			MapTile initialTile = gameContext.getAreaMap().getTile(mapTile.getTileX() + direction.getXOffset(), mapTile.getTileY() + direction.getYOffset());
			if (initialTile != null && initialTile.getRoof().getState().equals(TileRoofState.CONSTRUCTED)) {
				frontier.add(initialTile);

				boolean supportFound = false;

				while (!frontier.isEmpty()) {
					Iterator<MapTile> iterator = frontier.iterator();
					MapTile currentTile = iterator.next();
					iterator.remove();
					explored.add(currentTile);

					if (containsSupport(currentTile) || currentTile.hasWall()) {
						supportFound = true;
						break;
					}

					for (CompassDirection cardinalDirection : CompassDirection.CARDINAL_DIRECTIONS) {
						MapTile nextTile = gameContext.getAreaMap().getTile(currentTile.getTileX() + cardinalDirection.getXOffset(), currentTile.getTileY() + cardinalDirection.getYOffset());
						if (nextTile != null && !nextTile.getRoof().getState().equals(TileRoofState.OPEN) && !explored.contains(nextTile)) {
							frontier.add(nextTile);
						}
					}
				}

				if (!supportFound) {
					messageDispatcher.dispatchMessage(MessageType.ROOF_COLLAPSE, new RoofCollapseMessage(explored));
				}
			}

		}
	}

	public void supportDeconstructed(MapTile mapTile) {
		Set<MapTile> roofTilesWithoutSupport = new HashSet<>();

		for (int yCursor = mapTile.getTileY()-ROOF_SUPPORT_MAX_DISTANCE; yCursor <= mapTile.getTileY()+ROOF_SUPPORT_MAX_DISTANCE; yCursor++) {
			for (int xCursor = mapTile.getTileX()-ROOF_SUPPORT_MAX_DISTANCE; xCursor <= mapTile.getTileX()+ROOF_SUPPORT_MAX_DISTANCE; xCursor++) {
				MapTile tile = gameContext.getAreaMap().getTile(xCursor, yCursor);
				if (tile != null && tile.getRoof().getState().equals(TileRoofState.CONSTRUCTED) && !withinRangeOfSupport(tile, true)) {
					roofTilesWithoutSupport.add(tile);
				}
			}
		}

		if (!roofTilesWithoutSupport.isEmpty()) {
			messageDispatcher.dispatchMessage(MessageType.ROOF_COLLAPSE, new RoofCollapseMessage(roofTilesWithoutSupport));
		}
	}

	private void switchState(MapTile mapTile, RoofConstructionState newState) {
		RoofConstructionState oldState = mapTile.getRoof().getConstructionState();
		if (oldState.equals(newState)) {
			return;
		}

		if (oldState.equals(READY_FOR_CONSTRUCTION) || oldState.equals(PENDING_DECONSTRUCTION)) {
			// cancel outstanding job
			jobStore.getJobsAtLocation(mapTile.getTilePosition())
					.stream()
					.filter(j -> j.getType().equals(constructRoofingJobType) || j.getType().equals(deconstructRoofingJobType))
					.collect(Collectors.toList())// avoids ConcurrentModificationException
					.forEach(job -> {
						messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, job);
					});
		}

		mapTile.getRoof().setConstructionState(newState);

		if (newState.equals(RoofConstructionState.READY_FOR_CONSTRUCTION)) {
			Job constructRoofingJob = new Job(constructRoofingJobType);
			constructRoofingJob.setJobLocation(mapTile.getTilePosition());
			messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, constructRoofingJob);
		} else if (newState.equals(PENDING_DECONSTRUCTION)) {
			Job deconstructRoofingJob = new Job(deconstructRoofingJobType);
			deconstructRoofingJob.setJobLocation(mapTile.getTilePosition());
			messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, deconstructRoofingJob);
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

	private boolean hasAdjacentRoof(MapTile mapTile) {
		for (CompassDirection direction : CompassDirection.CARDINAL_DIRECTIONS) {
			MapTile tile = gameContext.getAreaMap().getTile(mapTile.getTileX() + direction.getXOffset(), mapTile.getTileY() + direction.getYOffset());
			if (tile != null && !tile.getRoof().getState().equals(TileRoofState.OPEN)) {
				return true;
			}
		}
		return false;
	}

	private boolean withinRangeOfSupport(MapTile mapTile, boolean requireContinuousRoof) {
		for (int yCursor = mapTile.getTileY()-ROOF_SUPPORT_MAX_DISTANCE; yCursor <= mapTile.getTileY()+ROOF_SUPPORT_MAX_DISTANCE; yCursor++) {
			for (int xCursor = mapTile.getTileX()-ROOF_SUPPORT_MAX_DISTANCE; xCursor <= mapTile.getTileX()+ROOF_SUPPORT_MAX_DISTANCE; xCursor++) {
				MapTile tile = gameContext.getAreaMap().getTile(xCursor, yCursor);
				if (tile != null && (tile.hasWall() || containsSupport(tile))) {
					if (!requireContinuousRoof || continuousRoofBetween(mapTile, tile)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean continuousRoofBetween(MapTile tileA, MapTile tileB) {
		Deque<MapTile> frontier = new ArrayDeque<>();
		Set<MapTile> explored = new HashSet<>();

		frontier.add(tileA);

		while (!frontier.isEmpty()) {
			MapTile currentTile = frontier.removeFirst();

			if (currentTile.equals(tileB)) {
				return true;
			}

			explored.add(currentTile);

			if (explored.size() > EXPLORED_TOO_MANY) {
				return true;
			}

			for (CompassDirection direction : CompassDirection.CARDINAL_DIRECTIONS) {
				MapTile nextTile = gameContext.getAreaMap().getTile(currentTile.getTilePosition().x + direction.getXOffset(), currentTile.getTilePosition().y + direction.getYOffset());
				if (!explored.contains(nextTile) && !frontier.contains(nextTile) && !nextTile.getRoof().getState().equals(TileRoofState.OPEN)) {
					frontier.add(nextTile);
				}
			}

		}

		return false;
	}

	private boolean containsSupport(MapTile tile) {
		return tile.getEntities().stream().anyMatch(e -> e.getTag(SupportsRoofTag.class) != null);
	}
}
