package technology.rocketjump.undermount.rendering.lighting;

import com.badlogic.gdx.math.Vector2;
import org.junit.Test;
import technology.rocketjump.undermount.mapping.tile.wall.Edge;

import java.util.Iterator;
import java.util.TreeMap;

import static org.fest.assertions.Assertions.assertThat;

public class NearestEdgeComparatorTest {

	@Test
	public void testNearestEdgeComparator() {
		TreeMap<Edge, Integer> map = new TreeMap<>(new NearestEdgeComparator());

		map.put(new Edge(
				new Vector2(0.0f, 3.0f),
				new Vector2(0.0f, 4.0f)
		), 3);
		map.put(new Edge(
				new Vector2(-10f, -1f),
				new Vector2(-10f, -2f)
		), 4);
		map.put(new Edge(
				new Vector2(1.0f, 1.0f),
				new Vector2(1.5f, 1.0f)
		), 2);
		map.put(new Edge(
				new Vector2(0.5f, 0.5f),
				new Vector2(1.0f, 0.5f)
		), 1);


		Iterator<Integer> valueIterator = map.values().iterator();

		assertThat(valueIterator.next()).isEqualTo(1);
		assertThat(valueIterator.next()).isEqualTo(2);
		assertThat(valueIterator.next()).isEqualTo(3);
		assertThat(valueIterator.next()).isEqualTo(4);

	}

}