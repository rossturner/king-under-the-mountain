package technology.rocketjump.undermount.rendering.lighting;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import technology.rocketjump.undermount.assets.entities.furniture.model.DoorState;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.layout.TileLayoutAtlas;
import technology.rocketjump.undermount.mapping.tile.wall.Edge;
import technology.rocketjump.undermount.mapping.tile.wall.WallEdgeAtlas;
import technology.rocketjump.undermount.mapping.tile.wall.WallEdgeDefinition;

import java.util.*;

/**
 * Here be dragons
 */
public class LightProcessor {

	public static final float EPSILON = 0.0001f;
	private final TileLayoutAtlas tileLayoutAtlas;
	private final WallEdgeAtlas wallEdgeAtlas;

	@Inject
	public LightProcessor(TileLayoutAtlas tileLayoutAtlas, WallEdgeAtlas wallEdgeAtlas) {
		this.tileLayoutAtlas = tileLayoutAtlas;
		this.wallEdgeAtlas = wallEdgeAtlas;
	}

	public void updateLightGeometry(PointLight light, TiledMap worldMap) {
		Array<Edge> lightGeometry = light.getLightPolygonEdges();
		lightGeometry.clear();

		TreeMap<Vector2, List<Edge>> sortedPointsToWallEdges = new TreeMap<>(new ClockwisePointComparator());
		Set<Edge> allEdges = new HashSet<>();

		buildSortedPointToEdgeMap(light, worldMap, sortedPointsToWallEdges, allEdges);

		// Now we have a clockwise-sorted map of points to the edges they are a start or endpoint of
		// Also the edges have potentially been set so that pointA comes before pointB as we sweep around clockwise

		Set<Vector2> visitedPoints = new HashSet<>(sortedPointsToWallEdges.size());
		PriorityQueue<Edge> nearestEdgesToConsider = new PriorityQueue<>(new NearestEdgeComparator());

		Edge currentEdge;

		// First (current) edge needs to be the nearest edge that has y > 0 and x points across x = 0, i.e. the first edge directly up
		PriorityQueue<Edge> nearestStartingEdges = getEdgesAboveOriginThatCrossXAxis(allEdges);
		currentEdge = nearestStartingEdges.peek();
		if (currentEdge == null) {
			// This should only happen if we go outside the map, so don't draw any geometry
			return;
		}
		// Initially consider the edges above the origin that go across the X-axis - these will be removed as their pointB
		// is encountered, but then added back in again later when their pointA is encountered as we finish processing clockwise
		nearestEdgesToConsider.addAll(nearestStartingEdges);

		Vector2 previousVisiblePoint = calculateInitialPreviouslyVisiblePoint(light, sortedPointsToWallEdges, currentEdge);
		if (previousVisiblePoint == null) {
			// Don't know why this is happening, when debugging the above returns non-null but later previousVisiblePoint is then null
			return;
		}

		for (Map.Entry<Vector2, List<Edge>> sortedMapEntry : sortedPointsToWallEdges.entrySet()) {
			Vector2 currentPoint = sortedMapEntry.getKey();
			List<Edge> edgesForPoint = sortedMapEntry.getValue();

			for (Edge edgeForPoint : edgesForPoint) {
				if (edgeForPoint.getPointA().epsilonEquals(currentPoint, EPSILON)) {
					// This is pointA so it is starting a new wall
					nearestEdgesToConsider.add(edgeForPoint);
				} else {
					// This must be pointB, and due to the clockwise ordering of points, must be the end of a wall
					nearestEdgesToConsider.remove(edgeForPoint);
				}
			}

			if (nearestEdgesToConsider.peek() != null && !nearestEdgesToConsider.peek().equals(currentEdge)) {
				// Nearest (current) edge has changed, so fill in triangle/add a new light edge
				Edge lastEdge = currentEdge;
				currentEdge = nearestEdgesToConsider.peek();

				if (currentEdge.averageEndpointDistanceSquared() < lastEdge.averageEndpointDistanceSquared()) {
					// If new edge is closer, polygon edge is previousVisible -> intersection with current point
					Vector2 intersection = intersection(currentPoint, lastEdge, PointLight.LIGHT_RADIUS);
					lightGeometry.add(new Edge(
							previousVisiblePoint,
							intersection
					));
					previousVisiblePoint = currentPoint;
				} else {
					// If new edge is further away, polygon edge is intersection with previous visible -> current point
					Vector2 intersection = intersection(previousVisiblePoint, lastEdge, PointLight.LIGHT_RADIUS);
					lightGeometry.add(new Edge(
							intersection,
							currentPoint
					));
					Vector2 intersectionOnNewEdge = intersection(currentPoint, currentEdge, PointLight.LIGHT_RADIUS);
					previousVisiblePoint = intersectionOnNewEdge;
				}

			} // else still on the same current edge, so current point must be occluded

			visitedPoints.add(currentPoint);
		}

		light.updateMesh();
	}

	private void buildSortedPointToEdgeMap(PointLight light, TiledMap worldMap, TreeMap<Vector2, List<Edge>> sortedPointsToWallEdges, Set<Edge> allEdges) {
		addBoundingBoxEdges(light, sortedPointsToWallEdges, worldMap, allEdges);

		Vector2 lightPosition = light.getWorldPosition();
		int lightTileX = (int)Math.floor(lightPosition.x);
		int lightTileY = (int)Math.floor(lightPosition.y);
		for (int yCursor = lightTileY - Math.round(PointLight.LIGHT_RADIUS); yCursor <= lightTileY + PointLight.LIGHT_RADIUS; yCursor++) {
			for (int xCursor = lightTileX - Math.round(PointLight.LIGHT_RADIUS); xCursor <= lightTileX + PointLight.LIGHT_RADIUS; xCursor++) {
				MapTile mapTile = worldMap.getTile(xCursor, yCursor);
				if (mapTile != null && mapTile.hasWall()) {

					int simplifiedLayoutId = tileLayoutAtlas.simplifyLayoutId(mapTile.getWall().getTrueLayout().getId());
					WallEdgeDefinition edgeDefinition = wallEdgeAtlas.getForLayoutId(simplifiedLayoutId);

					addEdgeDefinitionToEdgeMap(light, sortedPointsToWallEdges, allEdges, lightPosition, yCursor, xCursor, edgeDefinition);
				} else if (mapTile != null && mapTile.hasDoorway()) {
					WallEdgeDefinition edgeDefinition = wallEdgeAtlas.getForDoorway(mapTile.getDoorway());
					addEdgeDefinitionToEdgeMap(light, sortedPointsToWallEdges, allEdges, lightPosition, yCursor, xCursor, edgeDefinition);

					if (mapTile.getDoorway().getDoorState().equals(DoorState.CLOSED)) {
						WallEdgeDefinition closedDoorEdgeDefinition = wallEdgeAtlas.getForClosedDoor(mapTile.getDoorway());
						addEdgeDefinitionToEdgeMap(light, sortedPointsToWallEdges, allEdges, lightPosition, yCursor, xCursor, closedDoorEdgeDefinition);
					}

				}
			}
		}
	}

	private void addEdgeDefinitionToEdgeMap(PointLight light, TreeMap<Vector2, List<Edge>> sortedPointsToWallEdges, Set<Edge> allEdges, Vector2 lightPosition, int yCursor, int xCursor, WallEdgeDefinition edgeDefinition) {
		Array<Edge> edges = edgeDefinition.getEdgesForVisibilityPolygon(new Vector2(lightPosition.x - xCursor, lightPosition.y - yCursor));
		for (Edge edge : edges) {
			Edge edgeRelativeToLight = new Edge(
					new Vector2(xCursor, yCursor).add(edge.getPointA()).sub(lightPosition),
					new Vector2(xCursor, yCursor).add(edge.getPointB()).sub(lightPosition)
			);
			if (withinRadiusBounds(edgeRelativeToLight, PointLight.LIGHT_RADIUS)) {
				addEdgeToMap(edgeRelativeToLight, sortedPointsToWallEdges, allEdges);
			}
		}
	}

	private boolean withinRadiusBounds(Edge edge, float radius) {
		return (Math.abs(edge.getPointA().x) < radius && Math.abs(edge.getPointA().y) < radius) &&
				(Math.abs(edge.getPointB().x) < radius && Math.abs(edge.getPointB().y) < radius);
	}

	private PriorityQueue<Edge> getEdgesAboveOriginThatCrossXAxis(Set<Edge> allEdges) {
		PriorityQueue<Edge> nearestStartingEdges = new PriorityQueue<>(new NearestEdgeComparator());
		for (Edge edge : allEdges) {
			if ((edge.getPointA().y > 0 && edge.getPointB().y > 0 && edge.getPointA().x < 0 && edge.getPointB().x >= 0) ||
					(edge.getPointA().y > 0 && edge.getPointB().y > 0 && edge.getPointA().x > 0 && edge.getPointB().x <= 0)) {
				nearestStartingEdges.add(edge);
			}
		}
		return nearestStartingEdges;
	}

	/**
	 * The initial previous visible point is either the left of the initial edge,
	 * or the intersection of the initial edge with the most-clockwise point in front of it
	 */
	private Vector2 calculateInitialPreviouslyVisiblePoint(PointLight light, TreeMap<Vector2, List<Edge>> sortedPointsToWallEdges, Edge initialEdge) {
		Vector2 previousVisiblePoint = null;
		NavigableSet<Vector2> sortedPointsInReverse = sortedPointsToWallEdges.descendingKeySet();
		for (Vector2 pointToCheck : sortedPointsInReverse) {
			if (pointToCheck.equals(initialEdge.getPointA())) {
				previousVisiblePoint = initialEdge.getPointA();
				break;
			} else if (pointToCheck.y <= initialEdge.getPointA().y) {
				// currentEdge is a West->East wall (i.e. where pointA.y == pointB.y) so the first point anti-clockwise with a lower Y value
				// is the one to createHumanoid the intersection with
				previousVisiblePoint = intersection(pointToCheck, initialEdge, PointLight.LIGHT_RADIUS);
				break;
			}
		}
		return previousVisiblePoint;
	}

	/**
	 * This method returns a Vector2 which is the intersection of the direction vector from the origin, scaled so that it
	 * exceeds the light's bounding box (so the magnitude of the direction vector does not matter), or null if the direction
	 * does not intersect the given edge
	 */
	private Vector2 intersection(Vector2 direction, Edge edge, float lightRadius) {
		float extendedlightRadius = lightRadius * 1.5f; // As LIGHT_RADIUS is in x and y equally, the max distance to the corner is LIGHT_RADIUS * sqrt(2) (~ 1.4142)
		float smallestDirectionComponent = Math.min(Math.abs(direction.x), Math.abs(direction.y));
		smallestDirectionComponent = Math.max(smallestDirectionComponent, 0.0001f); // To account for x or y component == 0
		float scale = extendedlightRadius / smallestDirectionComponent;
		direction = new Vector2(direction.x * scale, direction.y * scale);

		Vector2 intersection = new Vector2();
		if (Intersector.intersectLines(0f, 0f, direction.x, direction.y, edge.getPointA().x, edge.getPointA().y, edge.getPointB().x, edge.getPointB().y, intersection)) {
			return intersection;
		} else {
			return null;
		}
	}

	/**
	 * This method adds edges for a square around light.worldPosition of size LIGHT_RADIUS*2 by LIGHT_RADIUS*2, limited by the world edges if the
	 * light is within LIGHT_RADIUS distance of a world edge
	 */
	private void addBoundingBoxEdges(PointLight light, TreeMap<Vector2, List<Edge>> sortedPointsToWallEdges,
									 TiledMap worldMap, Set<Edge> allEdges) {
		Vector2 lightWorldPosition = light.getWorldPosition();
		float left = 0f - Math.min(PointLight.LIGHT_RADIUS, lightWorldPosition.x);
		float right = 0f + Math.min(PointLight.LIGHT_RADIUS, worldMap.getWidth() - lightWorldPosition.x);
		float bottom = 0f - Math.min(PointLight.LIGHT_RADIUS, lightWorldPosition.y);
		float top = 0f + Math.min(PointLight.LIGHT_RADIUS, worldMap.getHeight() - lightWorldPosition.y);

		Vector2 lowerLeft = new Vector2(left, bottom);
		Vector2 upperLeft = new Vector2(left, top);
		Vector2 upperRight = new Vector2(right, top);
		Vector2 lowerRight = new Vector2(right, bottom);

		// Add bounding box edges
		addEdgeToMap(new Edge(lowerLeft, upperLeft), sortedPointsToWallEdges, allEdges);
		addEdgeToMap(new Edge(upperLeft, upperRight), sortedPointsToWallEdges, allEdges);
		addEdgeToMap(new Edge(upperRight, lowerRight), sortedPointsToWallEdges, allEdges);
		addEdgeToMap(new Edge(lowerRight, lowerLeft), sortedPointsToWallEdges, allEdges);
	}

	private void addEdgeToMap(Edge edge, TreeMap<Vector2, List<Edge>> sortedPointsToWallEdges, Set<Edge> allEdges) {
		edge = edge.reorderPointsClockwiseAroundOrigin();
		addPointToMap(edge.getPointA(), edge, sortedPointsToWallEdges);
		addPointToMap(edge.getPointB(), edge, sortedPointsToWallEdges);
		allEdges.add(edge);
	}

	private void addPointToMap(Vector2 point, Edge edge, TreeMap<Vector2, List<Edge>> sortedPointsToWallEdges) {
		if (!sortedPointsToWallEdges.containsKey(point)) {
			List<Edge> wallEdgeArray = new LinkedList<>();
			wallEdgeArray.add(edge);
			sortedPointsToWallEdges.put(point, wallEdgeArray);
		} else {
			sortedPointsToWallEdges.get(point).add(edge);
		}
	}
}
