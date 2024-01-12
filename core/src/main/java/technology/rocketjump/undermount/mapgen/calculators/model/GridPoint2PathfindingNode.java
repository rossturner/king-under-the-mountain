package technology.rocketjump.undermount.mapgen.calculators.model;

import com.badlogic.gdx.math.GridPoint2;

import java.util.Objects;

public class GridPoint2PathfindingNode implements Comparable<GridPoint2PathfindingNode> {

	private GridPoint2 worldPosition;
	private final float costToGetHere;
	private final GridPoint2PathfindingNode previousNodeInPath;
	private final float estimateToGoal;

	public GridPoint2PathfindingNode(GridPoint2 worldPosition, float costToGetHere, GridPoint2PathfindingNode previousNodeInPath, float estimateToGoal) {
		this.worldPosition = worldPosition;
		this.costToGetHere = costToGetHere;
		this.previousNodeInPath = previousNodeInPath;
		this.estimateToGoal = estimateToGoal;
	}

	@Override
	public int compareTo(GridPoint2PathfindingNode other) {
		return Math.round((this.getTotalDistanceFromAndTo() - other.getTotalDistanceFromAndTo()) * 1000f);
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
		GridPoint2PathfindingNode otherNode = (GridPoint2PathfindingNode) other;
		return worldPosition.equals(otherNode.worldPosition);
	}

	@Override
	public int hashCode() {
		return Objects.hash(worldPosition);
	}

	public GridPoint2 getWorldPosition() {
		return worldPosition;
	}

	public float getCostToGetHere() {
		return costToGetHere;
	}

	public GridPoint2PathfindingNode getPreviousNodeInPath() {
		return previousNodeInPath;
	}

	public float getEstimateToGoal() {
		return estimateToGoal;
	}

	public void setWorldPosition(GridPoint2 worldPosition) {
		this.worldPosition = worldPosition;
	}
}
