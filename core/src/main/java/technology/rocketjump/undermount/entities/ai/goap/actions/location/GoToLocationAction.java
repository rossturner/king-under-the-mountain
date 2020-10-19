package technology.rocketjump.undermount.entities.ai.goap.actions.location;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.ai.goap.actions.Action;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.humanoid.SteeringComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.LocationComponent;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HaulingComponent;
import technology.rocketjump.undermount.entities.planning.PathfindingCallback;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.PathfindingRequestMessage;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rooms.HaulingAllocation;

import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.undermount.entities.behaviour.furniture.CraftingStationBehaviour.getNearestNavigableWorkspace;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.misc.VectorUtils.toVector;

public class GoToLocationAction extends Action implements PathfindingCallback {

	public static final float WAYPOINT_TOLERANCE = 0.5f;
	public static final float DESTINATION_TOLERANCE = 0.15f;
	public static final float MAX_TIME_TO_WAIT = 5f;

	protected boolean pathfindingRequested;
	private GraphPath<Vector2> path;
	private float timeWaitingForPath;
	private int pathCursor = 0;

	protected Vector2 overrideLocation;

	public GoToLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (!pathfindingRequested) {
			Vector2 destination = selectDestination(gameContext);
			if (destination == null) {
				// Might have already set completionType to success for some cases e.g. going to own entity inventory
				if (completionType == null) {
					completionType = FAILURE; // Nowhere to navigate to
				}
				return;
			}
			PathfindingRequestMessage pathfindingRequestMessage = new PathfindingRequestMessage(
					parent.parentEntity.getLocationComponent().getWorldPosition(),
					destination, gameContext.getAreaMap(), this, parent.parentEntity.getId());

			parent.messageDispatcher.dispatchMessage(MessageType.PATHFINDING_REQUEST, pathfindingRequestMessage);
			pathfindingRequested = true;
		} else if (path == null) {
			// Waiting for path
			timeWaitingForPath += deltaTime;
			if (timeWaitingForPath > MAX_TIME_TO_WAIT) {
				completionType = FAILURE;
			}
		} else {
			// Path found
			followPath(gameContext);


			checkForCompletion();
		}
	}

	private void followPath(GameContext gameContext) {
		SteeringComponent steeringComponent = parent.parentEntity.getBehaviourComponent().getSteeringComponent();
		LocationComponent locationComponent = parent.parentEntity.getLocationComponent();

		if (pathCursor >= path.getCount()) {
			// Now out of bounds, this shouldn't happen but was reported in a crash log
			// Perhaps path has been re-assigned when pathfinding cancelled after some timeout?
			completionType = FAILURE;
			return;
		}
		Vector2 destination = path.get(path.getCount() - 1);
		Vector2 nextPathNode = path.get(pathCursor);

		steeringComponent.setDestination(destination);
		steeringComponent.setNextWaypoint(nextPathNode);

		MapTile nextTile = gameContext.getAreaMap().getTile(nextPathNode);
		MapTile destinationTile = gameContext.getAreaMap().getTile(destination);
		if (!nextTile.isNavigable() || !destinationTile.isNavigable()) {
			// The next tile is no longer navigable - a wall or something must have been placed there
			completionType = FAILURE;
			return;
		}

		if (Math.abs(locationComponent.getWorldPosition().x - nextPathNode.x) < WAYPOINT_TOLERANCE &&
				Math.abs(locationComponent.getWorldPosition().y - nextPathNode.y) < WAYPOINT_TOLERANCE) {
			if (pathCursor + 1 < path.getCount()) {
				// Still more nodes to follow
				pathCursor++;
			}
		}
	}

	private void checkForCompletion() {
		if (path.getCount() == 0) {
			completionType = FAILURE;
		} else {
			Vector2 destination = path.get(path.getCount() - 1);
			Vector2 worldPosition = parent.parentEntity.getLocationComponent().getWorldPosition();

			boolean arrivedAtDestination = (Math.abs(worldPosition.x - destination.x) < DESTINATION_TOLERANCE &&
					Math.abs(worldPosition.y - destination.y) < DESTINATION_TOLERANCE);
			if (arrivedAtDestination) {
				parent.parentEntity.getBehaviourComponent().getSteeringComponent().destinationReached();
				completionType = SUCCESS;
			}
		}
	}

	protected Vector2 selectDestination(GameContext gameContext) {
		if (overrideLocation != null) {
			return overrideLocation;
		} else if (parent.getAssignedHaulingAllocation() != null) {
			// This is a hauling-type job
			if (currentlyCarryingItemFromAllocation()) {
				return calculatePosition(parent.getAssignedHaulingAllocation().getTargetPositionType(),
						parent.getAssignedHaulingAllocation().getTargetPosition(),
						parent.getAssignedHaulingAllocation().getTargetId(), gameContext); // FIXME this should be targetContainerId rather than targetId, but its not yet implemented yet
			} else if (parent.getAssignedHaulingAllocation().getSourcePosition() != null) {
				if (parent.getAssignedHaulingAllocation().getHauledEntityType().equals(EntityType.FURNITURE)) {
					return calculatePosition(parent.getAssignedHaulingAllocation().getSourcePositionType(),
							parent.getAssignedHaulingAllocation().getSourcePosition(),
							parent.getAssignedHaulingAllocation().getHauledEntityId(), gameContext);
				} else {
					return calculatePosition(parent.getAssignedHaulingAllocation().getSourcePositionType(),
							parent.getAssignedHaulingAllocation().getSourcePosition(),
							parent.getAssignedHaulingAllocation().getSourceContainerId(), gameContext);
				}
			} else {
				// FIXME Is job always non-null here?
				return getJobLocation(gameContext);
			}
		} else if (parent.getAssignedJob() != null) {
			return getJobLocation(gameContext);
		} else if (parent.getAssignedFurnitureId() != null) {
			// Need to go to a workspace of assigned furniture
			Entity assignedFurniture = gameContext.getEntities().get(parent.getAssignedFurnitureId());
			if (assignedFurniture == null) {
				Logger.error("Could not find assigned furniture by ID " + parent.getAssignedFurnitureId());
				return null;
			}

			FurnitureEntityAttributes attributes = (FurnitureEntityAttributes)assignedFurniture.getPhysicalEntityComponent().getAttributes();
			if (attributes.getFurnitureType().getFurnitureCategory().isBlocksMovement()) {
				// Blocks movement
				if (attributes.getCurrentLayout().getWorkspaces().isEmpty()) {
					// No workspaces
					Logger.error("Not yet implemented: Going to adjacent tile to furniture");
					return null;
				} else {
					// Has workspaces
					FurnitureLayout.Workspace navigableWorkspace = getNearestNavigableWorkspace(assignedFurniture, gameContext.getAreaMap(),
							toGridPoint(parent.parentEntity.getLocationComponent().getWorldOrParentPosition()));
					if (navigableWorkspace == null) {
						// Could not navigate to any workspaces
						return null;
					} else {
						return toVector(navigableWorkspace.getAccessedFrom());
					}
				}
			} else {
				// Does not block movement
				return assignedFurniture.getLocationComponent().getWorldPosition();
			}

		} else {
			return null;
		}
	}

	public void setOverrideLocation(Vector2 overrideLocation) {
		this.overrideLocation = overrideLocation;
	}

	private Vector2 calculatePosition(HaulingAllocation.AllocationPositionType allocationPositionType,
									  GridPoint2 allocationPosition, Long targetEntityId, GameContext gameContext) {
		if (HaulingAllocation.AllocationPositionType.FURNITURE.equals(allocationPositionType)) {
			Entity targetFurniture = gameContext.getAreaMap().getTile(allocationPosition)
					.getEntity(targetEntityId);
			if (targetFurniture != null) {
				FurnitureLayout.Workspace navigableWorkspace = getNearestNavigableWorkspace(targetFurniture, gameContext.getAreaMap(),
						toGridPoint(parent.parentEntity.getLocationComponent().getWorldOrParentPosition()));
				if (navigableWorkspace != null) {
					return toVector(navigableWorkspace.getAccessedFrom());
				} else {
					Logger.error("Could not navigate to any workspaces when picking destination");
					return null;
				}
			} else {
				Logger.error("Could not find furniture for destination of assigned item allocation");
				return null;
			}
		} else {
			return toVector(allocationPosition);
		}
	}

	private boolean currentlyCarryingItemFromAllocation() {
		if (parent.getAssignedHaulingAllocation().getHauledEntityId() == null) {
			return false;
		}
		HaulingComponent haulingComponent = parent.parentEntity.getComponent(HaulingComponent.class);
		boolean haulingItem = haulingComponent != null && haulingComponent.getHauledEntity() != null;
		if (haulingItem) {
			return true;
		} else {
			InventoryComponent inventoryComponent = parent.parentEntity.getComponent(InventoryComponent.class);
			Entity carriedItem = inventoryComponent.getById(parent.getAssignedHaulingAllocation().getHauledEntityId());
			return carriedItem != null;
		}
	}

	protected Vector2 getJobLocation(GameContext gameContext) {
		Job assignedJob = parent.getAssignedJob();
		if (assignedJob.getType().isAccessedFromAdjacentTile()) {
			Array<GridPoint2> navigableTiles = new Array<>();

			for (MapTile neighbourTile : gameContext.getAreaMap().getOrthogonalNeighbours(assignedJob.getJobLocation().x, assignedJob.getJobLocation().y).values()) {
				if (neighbourTile.isNavigable()) {
					navigableTiles.add(neighbourTile.getTilePosition());
				}
			}
			if (navigableTiles.size == 0) {
				return null;
			} else if (navigableTiles.size == 1) {
				return toVector(navigableTiles.get(0));
			} else {
				// Slight hack - picking one at random should eventually pick one we can get to
				return toVector(navigableTiles.get(gameContext.getRandom().nextInt(navigableTiles.size)));
			}
		} else {
			return toVector(assignedJob.getJobLocation());
		}
	}

	@Override
	public void pathfindingComplete(GraphPath<Vector2> path, long relatedId) {
		this.path = path;
		if (path == null || path.getCount() == 0) {
			// No path found / possible
			completionType = FAILURE;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (pathfindingRequested) {
			asJson.put("pathfindingRequested", true);
		}

		if (path != null) {
			JSONArray pathJson = new JSONArray();
			for (Vector2 pathNode : path) {
				pathJson.add(JSONUtils.toJSON(pathNode));
			}
			asJson.put("path", pathJson);
		}

		if (timeWaitingForPath > 0f) {
			asJson.put("timeWaitingForPath", timeWaitingForPath);
		}
		if (pathCursor != 0) {
			asJson.put("pathCursor", pathCursor);
		}
		if (overrideLocation != null) {
			asJson.put("overrideLocation", JSONUtils.toJSON(overrideLocation));
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.pathfindingRequested = asJson.getBooleanValue("pathfindingRequested");

		JSONArray pathJson = asJson.getJSONArray("path");
		if (pathJson != null) {
			path = new DefaultGraphPath<>();
			for (int cursor = 0; cursor < pathJson.size(); cursor++) {
				path.add(JSONUtils.vector2(pathJson.getJSONObject(cursor)));
			}
		}

		this.timeWaitingForPath = asJson.getFloatValue("timeWaitingForPath");
		this.pathCursor = asJson.getIntValue("pathCursor");
		this.overrideLocation = JSONUtils.vector2(asJson.getJSONObject("overrideLocation"));
	}
}
