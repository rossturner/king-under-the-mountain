package technology.rocketjump.undermount.rendering.lighting;

import technology.rocketjump.undermount.mapping.tile.wall.Edge;

import java.util.Comparator;

/**
 * Comparator for which edge is nearer to the origin
 */
public class NearestEdgeComparator implements Comparator<Edge> {

	@Override
	public int compare(Edge a, Edge b) {
		float diff = a.averageEndpointDistanceSquared() - b.averageEndpointDistanceSquared();
		if (diff < 0) {
			return -1;
		} else if (diff > 0) {
			return 1;
		} else {
			return 0;
		}
	}

}
