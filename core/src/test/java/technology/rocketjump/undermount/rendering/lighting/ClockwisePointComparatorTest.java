package technology.rocketjump.undermount.rendering.lighting;

import com.badlogic.gdx.math.Vector2;
import org.junit.Test;

import java.util.Iterator;
import java.util.TreeMap;

import static org.fest.assertions.Assertions.assertThat;


public class ClockwisePointComparatorTest {

	@Test
	public void testPointsSortedIntoClockwiseOrder() {
		TreeMap<Vector2, Integer> map = new TreeMap<>(new ClockwisePointComparator());

		// Integer denotes expected order
		map.put(new Vector2(-1.0f, 1.0f), 7);
		map.put(new Vector2(2.0f, 1.0f), 2);
		map.put(new Vector2(-0.5f, -0.5f), 4);
		map.put(new Vector2(0.5f, -1.0f), 3);
		map.put(new Vector2(0.5f, 1.0f), 1);
		map.put(new Vector2(-1.0f, 0.0f), 6);
		map.put(new Vector2(-1.5f, -1.5f), 5); // nearest on same line should come first

		Iterator<Integer> valueIterator = map.values().iterator();

		assertThat(valueIterator.next()).isEqualTo(1);
		assertThat(valueIterator.next()).isEqualTo(2);
		assertThat(valueIterator.next()).isEqualTo(3);
		assertThat(valueIterator.next()).isEqualTo(4);
		assertThat(valueIterator.next()).isEqualTo(5);
		assertThat(valueIterator.next()).isEqualTo(6);
		assertThat(valueIterator.next()).isEqualTo(7);
	}

}