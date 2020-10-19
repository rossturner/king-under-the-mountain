package technology.rocketjump.undermount.misc;

import com.badlogic.gdx.ai.pfa.GraphPath;

import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

/**
 * Vector-based implementation of GraphPath to avoid any weirdness with LibGDX Array (default) implementation
 * @param <N>
 */
public class VectorGraphPath<N> implements GraphPath<N> {

	private final Vector<N> nodeList = new Vector<>();

	@Override
	public int getCount() {
		return nodeList.size();
	}

	@Override
	public N get(int index) {
		return nodeList.get(index);
	}

	@Override
	public void add(N node) {
		nodeList.add(node);
	}

	@Override
	public void clear() {
		nodeList.clear();
	}

	@Override
	public void reverse() {
		Collections.reverse(nodeList);
	}

	@Override
	public Iterator<N> iterator() {
		return nodeList.iterator();
	}
}
