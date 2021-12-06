package technology.rocketjump.undermount.entities.planning;

import com.badlogic.gdx.ai.msg.PriorityQueue;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.ai.pathfinding.Map2DCollection;
import technology.rocketjump.undermount.entities.ai.pathfinding.MapPathfindingNode;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileNeighbours;
import technology.rocketjump.undermount.messaging.async.BackgroundTaskResult;
import technology.rocketjump.undermount.messaging.types.PathfindingRequestMessage;
import technology.rocketjump.undermount.misc.VectorGraphPath;

import java.util.Map;
import java.util.concurrent.Callable;

public class PathfindingTask implements Callable<BackgroundTaskResult> {

	private final PathfindingCallback callback;
	private final MapTile originCell;
	private final MapTile destinationCell;
	private final Vector2 origin;
	private final Vector2 destination;

	private final TiledMap map;
	private final PriorityQueue<MapPathfindingNode> frontier = new PriorityQueue<>();
	private final Map2DCollection<MapPathfindingNode> explored;
	private final long relatedId;
	private final Entity parentEntity;

	public PathfindingTask(PathfindingRequestMessage requestMessage) {
		this.parentEntity = requestMessage.getRequestingEntity();
		this.callback = requestMessage.getCallback();
		this.map = requestMessage.getMap();
		this.explored = new Map2DCollection<>(map.getWidth());
		this.relatedId = requestMessage.getRelatedId();

		this.origin = requestMessage.getOrigin();
		this.originCell = map.getTile(origin);

		this.destination = requestMessage.getDestination();
		this.destinationCell = map.getTile(destination);
	}

	@Override
	public BackgroundTaskResult call() throws Exception {
		callback.pathfindingStarted(this);
		GraphPath<Vector2> path = new VectorGraphPath<>();

		if (originCell.getRegionId() != destinationCell.getRegionId() &&
				originCell.getRegionType().equals(MapTile.RegionType.GENERIC)) {
			// Different regions and current region is a normally accessible one,
			// So the destination is not accessible.

			// If the current region is a WALL or RIVER we'll still try to pathfind our way out
			// But if we're here, we're in two different regions and no path is possible
			callback.pathfindingComplete(path, relatedId);
			return BackgroundTaskResult.success();
		}

		if (!destinationCell.getRegionType().equals(MapTile.RegionType.GENERIC)) {
			// Can't pathfind to a wall or river type tile currently, just short circuit rather than flood fill the map
			callback.pathfindingComplete(path, relatedId);
			return BackgroundTaskResult.success();
		}

		if (originCell.equals(destinationCell)) {
			path.add(destination);
			callback.pathfindingComplete(path, relatedId);
			return BackgroundTaskResult.success();
		}

		TileNeighbours navigableNeighbours = filterToNavigable(map.getNeighbours(originCell.getTileX(), originCell.getTileY()));
		for (MapTile neighbourCell : navigableNeighbours.values()) {
			Vector2 tileWorldPosition = neighbourCell.getWorldPositionOfCenter();
			MapPathfindingNode node = new MapPathfindingNode(
					tileWorldPosition,
					getDistance(origin, tileWorldPosition),
					null,
					getDistance(tileWorldPosition, destination));
			frontier.add(node);
			explored.add(neighbourCell.getTileX(), neighbourCell.getTileY(), node);
		}

		while (frontier.size() > 0) {
			processNode(frontier.poll());
		}

		MapPathfindingNode nodeToNavigateVia = explored.get(destinationCell.getTileX(), destinationCell.getTileY());
		if (nodeToNavigateVia != null) {
			nodeToNavigateVia.setWorldPosition(destination);
		}
		while (nodeToNavigateVia != null) {
			path.add(nodeToNavigateVia.getWorldPosition());
			nodeToNavigateVia = nodeToNavigateVia.getPreviousNodeInPath();
		}
		path.reverse();
		callback.pathfindingComplete(path, relatedId);
		return BackgroundTaskResult.success();
	}

	private void processNode(MapPathfindingNode node) {
		MapTile nodeCell = map.getTile(node.getWorldPosition());
		if (nodeCell.equals(destinationCell)) {
			frontier.clear();
		} else {
			TileNeighbours neighbours = map.getNeighbours(nodeCell.getTileX(), nodeCell.getTileY());
			neighbours = filterToNavigable(neighbours);
			for (Map.Entry<CompassDirection, MapTile> neighboursEntry : neighbours.entrySet()) {
				float costToNeighbourTile = neighboursEntry.getKey().distance() * (1 / neighboursEntry.getValue().getFloor().getFloorType().getSpeedModifier());
				float newCostToGetHere = node.getCostToGetHere() + costToNeighbourTile;
				MapTile neighbourCell = neighboursEntry.getValue();
				MapPathfindingNode previouslyExploredNode = explored.get(neighbourCell.getTileX(), neighbourCell.getTileY());
				if (previouslyExploredNode == null || previouslyExploredNode.getCostToGetHere() > newCostToGetHere) {
					Vector2 positionOfNeighbourCell = neighbourCell.getWorldPositionOfCenter();
					MapPathfindingNode nextNode = new MapPathfindingNode(positionOfNeighbourCell, newCostToGetHere, node, getDistance(positionOfNeighbourCell, destination));
					explored.add(neighbourCell.getTileX(), neighbourCell.getTileY(),
							nextNode
					);
					frontier.add(nextNode);
				}

			}


		}

	}

	private TileNeighbours filterToNavigable(TileNeighbours tileNeighbours) {
		TileNeighbours filtered = new TileNeighbours();

		for (Map.Entry<CompassDirection, MapTile> compassDirectionMapCellEntry : tileNeighbours.entrySet()) {
			CompassDirection direction = compassDirectionMapCellEntry.getKey();
			MapTile cellInDirection = compassDirectionMapCellEntry.getValue();
			if (cellInDirection.isNavigable(parentEntity, originCell)) {
				if (direction.isDiagonal()) {
					// Needs both orthogonal neighbours to be navigable
					// Assuming that if there is a diagonal neighbour, both the orthogonal neighbours must not be null
					switch (direction) {
						case NORTH_EAST: {
							if (tileNeighbours.get(CompassDirection.NORTH).isNavigable(parentEntity, originCell) && tileNeighbours.get(CompassDirection.EAST).isNavigable(parentEntity, originCell)) {
								filtered.put(direction, cellInDirection);
							}
							break;
						}
						case NORTH_WEST: {
							if (tileNeighbours.get(CompassDirection.NORTH).isNavigable(parentEntity, originCell) && tileNeighbours.get(CompassDirection.WEST).isNavigable(parentEntity, originCell)) {
								filtered.put(direction, cellInDirection);
							}
							break;
						}
						case SOUTH_WEST: {
							if (tileNeighbours.get(CompassDirection.SOUTH).isNavigable(parentEntity, originCell) && tileNeighbours.get(CompassDirection.WEST).isNavigable(parentEntity, originCell)) {
								filtered.put(direction, cellInDirection);
							}
							break;
						}
						case SOUTH_EAST: {
							if (tileNeighbours.get(CompassDirection.SOUTH).isNavigable(parentEntity, originCell) && tileNeighbours.get(CompassDirection.EAST).isNavigable(parentEntity, originCell)) {
								filtered.put(direction, cellInDirection);
							}
							break;
						}
						default: {
							throw new RuntimeException("Diagonal direction is incorrect, thought " + direction.name() + " is diagonal");
						}
					}
				} else {
					filtered.put(direction, cellInDirection);
				}
			}
		}

		return filtered;
	}

	private float getDistance(Vector2 a, Vector2 b) {
		return b.cpy().sub(a).len();
	}

}
