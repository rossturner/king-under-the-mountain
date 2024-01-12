package technology.rocketjump.undermount.mapgen.calculators;

import com.badlogic.gdx.ai.msg.PriorityQueue;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.mapgen.calculators.model.GridPoint2PathfindingNode;
import technology.rocketjump.undermount.mapgen.calculators.model.Map2DCollection;
import technology.rocketjump.undermount.mapgen.model.output.GameMap;
import technology.rocketjump.undermount.mapgen.model.output.GameMapTile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RiverPathfindingCalculator {

	private final GridPoint2 origin;
	private final GameMapTile originTile;
	private final GridPoint2 destination;

	private final GameMap map;
	private final PriorityQueue<GridPoint2PathfindingNode> frontier = new PriorityQueue<>();
	private final Map2DCollection<GridPoint2PathfindingNode> explored;

	public RiverPathfindingCalculator(GridPoint2 origin, GridPoint2 destination, GameMap map) {
		this.map = map;
		this.explored = new Map2DCollection<>(map.getWidth());
		this.origin = origin;
		this.originTile = map.get(origin);
		this.destination = destination;
	}

	public List<GridPoint2> findPath() {
		List<GridPoint2> path = new ArrayList<>();

		if (origin.equals(destination)) {
			path.add(destination);
			return path; // Shouldn't ever really happen
		}

		frontier.add(new GridPoint2PathfindingNode(
				origin, 0, null, destination.dst(origin))
		);

		while (frontier.size() > 0 ) { // Note this usually ends up processing the entire region
			processNode(frontier.poll());
		}

		GridPoint2PathfindingNode nodeToNavigateVia = explored.get(destination);
		if (nodeToNavigateVia != null) {
			nodeToNavigateVia.setWorldPosition(destination);
		}
		while (nodeToNavigateVia != null) {
			path.add(nodeToNavigateVia.getWorldPosition());
			nodeToNavigateVia = nodeToNavigateVia.getPreviousNodeInPath();
		}
		Collections.reverse(path);
		return path;
	}

	private void processNode(GridPoint2PathfindingNode node) {
		GameMapTile nodeTile = map.get(node.getWorldPosition());
		if (nodeTile.equals(destination)) {
			frontier.clear();
		} else {
			for (GameMapTile neighbour : map.getOrthogonalNeighbours(node.getWorldPosition())) {
				if (neighbour.isNavigableByRiver()) {
					float newCostToGetHere = node.getCostToGetHere() + getCost(nodeTile, neighbour);
					GridPoint2PathfindingNode previouslyExploredNode = explored.get(neighbour.getPosition());
					if (previouslyExploredNode == null || previouslyExploredNode.getCostToGetHere() > newCostToGetHere) {
						GridPoint2PathfindingNode nextNode = new GridPoint2PathfindingNode(neighbour.getPosition(), newCostToGetHere, node, neighbour.getPosition().dst(destination));
						explored.add(neighbour.getPosition().x, neighbour.getPosition().y, nextNode);
						frontier.add(nextNode);
					}

				}
			}
		}

	}

	private float getCost(GameMapTile start, GameMapTile end) {
		float heightDifference =  end.getHeightMapValue() - start.getHeightMapValue();
		// If end is higher, heightDifference will be positive
		// Penalise positive height change
		if (heightDifference > 0) {
			heightDifference = heightDifference * 3f;
		}
 		return 1f + (heightDifference * 1000f);
	}

	private int calculateMaxNodesToExplore(GameMap map) {
		return (2 * map.getWidth()) + map.getHeight();
	}

}
