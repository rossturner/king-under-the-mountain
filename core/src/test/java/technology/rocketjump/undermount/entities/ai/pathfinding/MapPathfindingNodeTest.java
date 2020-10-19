package technology.rocketjump.undermount.entities.ai.pathfinding;

import org.junit.Test;

import java.util.PriorityQueue;

import static org.fest.assertions.Assertions.assertThat;

public class MapPathfindingNodeTest {

	@Test
	public void priorityQueueUsage_putsInCorrectOrder() {
		MapPathfindingNode a = new MapPathfindingNode(null, 1.0f, null, 1.0f);
		MapPathfindingNode b = new MapPathfindingNode(null, 2.0f, null, 1.0f);
		MapPathfindingNode c = new MapPathfindingNode(null, 3.0f, null, 1.0f);

		PriorityQueue<MapPathfindingNode> priorityQueue = new PriorityQueue<>();

		priorityQueue.add(b);
		priorityQueue.add(c);
		priorityQueue.add(a);

		assertThat(priorityQueue.poll().getTotalDistanceFromAndTo()).isEqualTo(2.0f);
		assertThat(priorityQueue.poll().getTotalDistanceFromAndTo()).isEqualTo(3.0f);
		assertThat(priorityQueue.poll().getTotalDistanceFromAndTo()).isEqualTo(4.0f);
	}

}