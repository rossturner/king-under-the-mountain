package technology.rocketjump.undermount.mapping;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobState;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.MapVertex;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.JobStateMessage;
import technology.rocketjump.undermount.messaging.types.RemoveDesignationMessage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import static technology.rocketjump.undermount.mapping.tile.TileExploration.*;

@Singleton
public class ExplorationMessageHandler implements Telegraph, GameContextAware {

	private final MessageDispatcher messageDispatcher;
	private final JobStore jobStore;
	private GameContext gameContext;

	@Inject
	public ExplorationMessageHandler(MessageDispatcher messageDispatcher, JobStore jobStore) {
		this.messageDispatcher = messageDispatcher;
		this.jobStore = jobStore;

		messageDispatcher.addListener(this, MessageType.WALL_REMOVED);
		messageDispatcher.addListener(this, MessageType.FLOOD_FILL_EXPLORATION);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.WALL_REMOVED: {
				GridPoint2 tileLocation = (GridPoint2) msg.extraInfo;
				floodFillExploration(tileLocation, false);
				return true;
			}
			case MessageType.FLOOD_FILL_EXPLORATION: {
				GridPoint2 tileLocation = (GridPoint2) msg.extraInfo;
				floodFillExploration(tileLocation, true);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void floodFillExploration(GridPoint2 tileLocation, boolean includeOtherRegions) {
		MapTile initialTile = gameContext.getAreaMap().getTile(tileLocation);
		Deque<MapTile> frontier = new ArrayDeque<>();
		Set<MapTile> explored = new HashSet<>();
		frontier.add(initialTile);

		while (!frontier.isEmpty()) {
			MapTile currentTile = frontier.pop();
			explored.add(currentTile);
			setToExplored(currentTile, tileLocation);

			for (MapTile neighbour : gameContext.getAreaMap().getNeighbours(currentTile.getTileX(), currentTile.getTileY()).values()) {
				if (!explored.contains(neighbour)) {
					if (currentTile.hasWall()) {
						if (neighbour.hasWall() && neighbour.getExploration().equals(UNEXPLORED)) {
							neighbour.setExploration(PARTIAL);
						}
					} else {
						// If this is a floor tile in a different region, do not add it to frontier so diagonally-different regions are not explored
						if (!frontier.contains(neighbour) && !neighbour.getExploration().equals(EXPLORED)) {
							if (neighbour.hasWall() || includeOtherRegions || neighbour.getRegionId() == initialTile.getRegionId()) {
								frontier.add(neighbour);
							}
						}
					}
				}
			}
		}

	}

	private void setToExplored(MapTile currentTile, GridPoint2 initialTilePosition) {
		currentTile.setExploration(EXPLORED);
		for (MapVertex mapVertex : gameContext.getAreaMap().getVertexNeighboursOfCell(currentTile).values()) {
			mapVertex.setExplorationVisibility(1f);
		}
		if (currentTile.getDesignation() != null) {
			if (currentTile.hasWall()) {
				for (Job job : jobStore.getJobsAtLocation(currentTile.getTilePosition())) {
					if (job.getJobState().equals(JobState.INACCESSIBLE) && !isAdjacent(currentTile.getTilePosition(), initialTilePosition)) {
						messageDispatcher.dispatchMessage(MessageType.JOB_STATE_CHANGE, new JobStateMessage(job, JobState.POTENTIALLY_ACCESSIBLE));
					}
				}
			} else {
				messageDispatcher.dispatchMessage(MessageType.REMOVE_DESIGNATION, new RemoveDesignationMessage(currentTile));
			}
		}
	}

	private boolean isAdjacent(GridPoint2 a, GridPoint2 b) {
		return Math.abs(a.x - b.x) <= 1 && Math.abs(a.y - b.y) <= 1;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
