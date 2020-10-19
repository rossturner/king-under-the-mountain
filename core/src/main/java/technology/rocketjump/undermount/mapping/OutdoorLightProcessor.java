package technology.rocketjump.undermount.mapping;

import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.MapVertex;

import java.util.*;

import static technology.rocketjump.undermount.mapping.tile.CompassDirection.*;

public class OutdoorLightProcessor {

	public void propagateLightFromMapVertex(TiledMap map, MapVertex vertex, float previousLuminosity) {

		// Short circuit if this is a "corner" vertex
		if ((hasWallInAbsoluteDirection(NORTH_WEST, map, vertex) && hasWallInAbsoluteDirection(SOUTH_EAST, map, vertex)) ||
			(hasWallInAbsoluteDirection(NORTH_EAST, map, vertex) && hasWallInAbsoluteDirection(SOUTH_WEST, map, vertex))) {
			return;
		}
		// See if light can propagate to each vertex neighbour
		for (CompassDirection direction : CompassDirection.values()) {
			if (!hasWallInDirection(direction, map, vertex)) {
				MapVertex neighbourVertex = map.getVertex(
						vertex.getVertexX() + direction.getXOffset(),
						vertex.getVertexY() + direction.getYOffset());
				if (neighbourVertex != null) {
					float propagatedLight = calculateFalloff(previousLuminosity, direction.distance());
					if (propagatedLight > neighbourVertex.getOutsideLightAmount()) {
						neighbourVertex.setOutsideLightAmount(propagatedLight);
						// FIXME light propagation is occuring depth first,
						// breadth first (i.e. setting all neighbour values first) would be more efficient
						propagateLightFromMapVertex(map, neighbourVertex, propagatedLight);
					}
				}
			}
		}

	}

	private static class VertexToVisit {

		public final MapVertex vertex;
		public final float distanceCovered;

		private VertexToVisit(MapVertex vertex, float distanceCovered) {
			this.vertex = vertex;
			this.distanceCovered = distanceCovered;
		}
	}

	public void propagateDarknessFromVertex(TiledMap map, MapVertex currentVertex) {
		Deque<VertexToVisit> toVisit = new LinkedList<>();
		List<MapVertex> endpoints = new LinkedList<>();
		Set<MapVertex> visited = new HashSet<>();
		visited.add(currentVertex);
		currentVertex.setOutsideLightAmount(0.0f);

		for (CompassDirection direction : CompassDirection.values()) {
			if (!hasWallInDirection(direction, map, currentVertex)) {
				MapVertex neighbourVertex = map.getVertex(
						currentVertex.getVertexX() + direction.getXOffset(),
						currentVertex.getVertexY() + direction.getYOffset());
				if (neighbourVertex != null) {
					toVisit.add(new VertexToVisit(neighbourVertex, direction.distance()));
					propagateDarknessFromVertexInner(map, neighbourVertex, direction.distance(), toVisit, endpoints, visited);
				}
			}
		}

		while (!toVisit.isEmpty()) {
			VertexToVisit vertexToVisit = toVisit.pop();
			propagateDarknessFromVertexInner(map, vertexToVisit.vertex, vertexToVisit.distanceCovered, toVisit, endpoints, visited);
		}

		// Now loop over endpoints and propagate light back in from them
		for (MapVertex endpoint : endpoints) {
			float currentLuminosity = endpoint.getOutsideLightAmount();
			propagateLightFromMapVertex(map, endpoint, currentLuminosity);
		}


	}

	private final float MAX_LIGHT_PROPAGATION_DISTANCE = 12.5f; // TODO This value needs tweaking. MODDING ?

	private void propagateDarknessFromVertexInner(TiledMap map, MapVertex currentVertex, float distanceTravelled,
												  Deque<VertexToVisit> toVisit, List<MapVertex> endpoints, Set<MapVertex> visited) {
		if (visited.contains(currentVertex)) {
			return;
		} else if (currentVertex.getOutsideLightAmount() < 0.1f) {
			//visited.add(currentVertex);
		} else if (currentVertex.getOutsideLightAmount() >= 0.99f) {
			endpoints.add(currentVertex);
		} else if (distanceTravelled >= MAX_LIGHT_PROPAGATION_DISTANCE){
			endpoints.add(currentVertex);
		} else {
			currentVertex.setOutsideLightAmount(0.0f);
			// Apply same to navigable neighbours
			for (CompassDirection direction : CompassDirection.values()) {
				if (!hasWallInDirection(direction, map, currentVertex)) {
					MapVertex neighbourVertex = map.getVertex(
							currentVertex.getVertexX() + direction.getXOffset(),
							currentVertex.getVertexY() + direction.getYOffset());
					if (neighbourVertex != null) {
						toVisit.add(new VertexToVisit(neighbourVertex, distanceTravelled + direction.distance()));
					}
				}
			}
		}
		visited.add(currentVertex);
	}

	private boolean hasWallInDirection(CompassDirection direction, TiledMap map, MapVertex vertex) {
		switch (direction) {
			case NORTH_WEST:
				return hasWallInAbsoluteDirection(NORTH_WEST, map, vertex) ||
						(hasWallInAbsoluteDirection(NORTH_EAST, map, vertex) && hasWallInAbsoluteDirection(SOUTH_WEST, map, vertex));
			case NORTH_EAST:
				return hasWallInAbsoluteDirection(NORTH_EAST, map, vertex) ||
						(hasWallInAbsoluteDirection(NORTH_WEST, map, vertex) && hasWallInAbsoluteDirection(SOUTH_EAST, map, vertex));
			case SOUTH_WEST:
				return hasWallInAbsoluteDirection(SOUTH_WEST, map, vertex) ||
						(hasWallInAbsoluteDirection(NORTH_WEST, map, vertex) && hasWallInAbsoluteDirection(SOUTH_EAST, map, vertex));
			case SOUTH_EAST:
				return hasWallInAbsoluteDirection(SOUTH_EAST, map, vertex) ||
						(hasWallInAbsoluteDirection(SOUTH_WEST, map, vertex) && hasWallInAbsoluteDirection(NORTH_EAST, map, vertex));
			case NORTH:
				return  (hasWallInAbsoluteDirection(NORTH_WEST, map, vertex)
						&& hasWallInAbsoluteDirection(NORTH_EAST, map, vertex));
			case EAST:
				return  (hasWallInAbsoluteDirection(NORTH_EAST, map, vertex)
						&& hasWallInAbsoluteDirection(SOUTH_EAST, map, vertex));
			case SOUTH:
				return  (hasWallInAbsoluteDirection(SOUTH_WEST, map, vertex)
						&& hasWallInAbsoluteDirection(SOUTH_EAST, map, vertex));
			default: // WEST
				return  (hasWallInAbsoluteDirection(SOUTH_WEST, map, vertex)
						&& hasWallInAbsoluteDirection(NORTH_WEST, map, vertex));
		}

	}

	private boolean hasWallInAbsoluteDirection(CompassDirection direction, TiledMap map, MapVertex vertex) {
		boolean hasWall = true;
		MapTile cell = map.getTile(vertex, direction);
		if (cell != null) {
			hasWall = cell.hasWall();
		}
		return hasWall;
	}

	private float calculateFalloff(float previousLuminsoity, float distance) {
		float newLuminosity = previousLuminsoity - (distance * (previousLuminsoity * 0.15f));
		if (newLuminosity < 0.15f) {
			newLuminosity = 0.0f;
		}
		return newLuminosity;
	}

}
