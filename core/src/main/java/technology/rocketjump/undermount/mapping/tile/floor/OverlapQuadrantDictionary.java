package technology.rocketjump.undermount.mapping.tile.floor;

import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static technology.rocketjump.undermount.mapping.tile.CompassDirection.*;

@Singleton
public class OverlapQuadrantDictionary {

	private final Map<Integer, IntArray> layoutToQuadrantArray = new ConcurrentHashMap<>();
	private final Set<Integer> uniqueQuadrantIds = new HashSet<>();

	@Inject
	public OverlapQuadrantDictionary(OverlapLayoutAtlas overlapLayoutAtlas) {

		IntSet uniqueLayoutIds = overlapLayoutAtlas.getUniqueLayoutIds();

		for (int layoutId = 0; layoutId < 256; layoutId++) {
			OverlapLayout layoutForId = new OverlapLayout(layoutId);
			IntArray quadrants = determineQuadrantsForLayout(layoutForId);
			layoutToQuadrantArray.put(layoutId, quadrants);
		}
	}


	public IntArray getOverlapQuadrants(int layoutId) {
		if (!layoutToQuadrantArray.containsKey(layoutId)) {
			OverlapLayout layout = new OverlapLayout(layoutId);
			throw new RuntimeException(getClass().getSimpleName() + " does not have an entry for layout " + layoutId + "\n" + layout.toString());
		}
		return layoutToQuadrantArray.get(layoutId);
	}

	private IntArray determineQuadrantsForLayout(OverlapLayout layout) {
		IntArray quadrants = new IntArray();

		// top left
		if (!layout.neighbourInDirection(NORTH_WEST) &&
				!layout.neighbourInDirection(WEST) &&
				!layout.neighbourInDirection(NORTH)) {
			quadrants.add(0);
		} else if (layout.neighbourInDirection(WEST) &&
				!layout.neighbourInDirection(NORTH)) {
			quadrants.add(24);
		} else if (!layout.neighbourInDirection(WEST) &&
				layout.neighbourInDirection(NORTH)) {
			quadrants.add(66);
		} else if (layout.neighbourInDirection(WEST) &&
				layout.neighbourInDirection(NORTH)) {
			quadrants.add(90);
		} else if (layout.neighbourInDirection(NORTH_WEST) &&
				!layout.neighbourInDirection(NORTH) &&
				!layout.neighbourInDirection(WEST)) {
			quadrants.add(165);
		} else {
			Logger.error("Can't determine quadrant layout");
		}

		// top right
		if (!layout.neighbourInDirection(NORTH_EAST) &&
				!layout.neighbourInDirection(EAST) &&
				!layout.neighbourInDirection(NORTH)) {
			quadrants.add(0);
		} else if (layout.neighbourInDirection(EAST) &&
				!layout.neighbourInDirection(NORTH)) {
			quadrants.add(24);
		} else if (!layout.neighbourInDirection(EAST) &&
				layout.neighbourInDirection(NORTH)) {
			quadrants.add(66);
		} else if (layout.neighbourInDirection(EAST) &&
				layout.neighbourInDirection(NORTH)) {
			quadrants.add(90);
		} else if (layout.neighbourInDirection(NORTH_EAST) &&
				!layout.neighbourInDirection(NORTH) &&
				!layout.neighbourInDirection(EAST)) {
			quadrants.add(165);
		} else {
			quadrants.add(0);
			Logger.error("Can't determine quadrant layout");
		}

		// bottom left
		if (!layout.neighbourInDirection(SOUTH_WEST) &&
				!layout.neighbourInDirection(WEST) &&
				!layout.neighbourInDirection(SOUTH)) {
			quadrants.add(0);
		} else if (layout.neighbourInDirection(WEST) &&
				!layout.neighbourInDirection(SOUTH)) {
			quadrants.add(24);
		} else if (!layout.neighbourInDirection(WEST) &&
				layout.neighbourInDirection(SOUTH)) {
			quadrants.add(66);
		} else if (layout.neighbourInDirection(WEST) &&
				layout.neighbourInDirection(SOUTH)) {
			quadrants.add(90);
		} else if (layout.neighbourInDirection(SOUTH_WEST) &&
				!layout.neighbourInDirection(SOUTH) &&
				!layout.neighbourInDirection(WEST)) {
			quadrants.add(165);
		} else {
			quadrants.add(0);
			Logger.error("Can't determine quadrant layout");
		}

		// bottom right
		if (!layout.neighbourInDirection(SOUTH_EAST) &&
				!layout.neighbourInDirection(EAST) &&
				!layout.neighbourInDirection(SOUTH)) {
			quadrants.add(0);
		} else if (layout.neighbourInDirection(EAST) &&
				!layout.neighbourInDirection(SOUTH)) {
			quadrants.add(24);
		} else if (!layout.neighbourInDirection(EAST) &&
				layout.neighbourInDirection(SOUTH)) {
			quadrants.add(66);
		} else if (layout.neighbourInDirection(EAST) &&
				layout.neighbourInDirection(SOUTH)) {
			quadrants.add(90);
		} else if (layout.neighbourInDirection(SOUTH_EAST) &&
				!layout.neighbourInDirection(SOUTH) &&
				!layout.neighbourInDirection(EAST)) {
			quadrants.add(165);
		} else {
			quadrants.add(0);
			Logger.error("Can't determine quadrant layout");
		}



		return quadrants;
	}

}
