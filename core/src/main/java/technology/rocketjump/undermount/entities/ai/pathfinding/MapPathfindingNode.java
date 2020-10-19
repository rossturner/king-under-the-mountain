package technology.rocketjump.undermount.entities.ai.pathfinding;

import com.badlogic.gdx.math.Vector2;

import java.util.Objects;

public class MapPathfindingNode implements Comparable<MapPathfindingNode> {

	private static final float EPSILON = 0.0001f;
	private Vector2 worldPosition;
	private final float costToGetHere;
	private final MapPathfindingNode previousNodeInPath;
	private final float estimateToGoal;

	public MapPathfindingNode(Vector2 worldPosition, float costToGetHere, MapPathfindingNode previousNodeInPath, float estimateToGoal) {
		this.worldPosition = worldPosition;
		this.costToGetHere = costToGetHere;
		this.previousNodeInPath = previousNodeInPath;
		this.estimateToGoal = estimateToGoal;
	}

	@Override
	public int compareTo(MapPathfindingNode other) {
		return Math.round((this.getTotalDistanceFromAndTo() - other.getTotalDistanceFromAndTo()) * 100f);
	}

	public float getTotalDistanceFromAndTo() {
		return costToGetHere + estimateToGoal;
	}

	/**
	 * Note that equals only uses world position epsilon equality
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other == null || getClass() != other.getClass()) return false;
		MapPathfindingNode otherNode = (MapPathfindingNode) other;
		return worldPosition.epsilonEquals(otherNode.worldPosition, EPSILON);
	}

	@Override
	public int hashCode() {
		return Objects.hash(worldPosition);
	}

	public Vector2 getWorldPosition() {
		return worldPosition;
	}

	public float getCostToGetHere() {
		return costToGetHere;
	}

	public MapPathfindingNode getPreviousNodeInPath() {
		return previousNodeInPath;
	}

	public float getEstimateToGoal() {
		return estimateToGoal;
	}

	public void setWorldPosition(Vector2 worldPosition) {
		this.worldPosition = worldPosition;
	}
}
