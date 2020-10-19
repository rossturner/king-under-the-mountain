package technology.rocketjump.undermount.mapping.tile.layout;

import com.google.inject.Singleton;

import java.util.LinkedHashSet;
import java.util.Set;

@Singleton
public class TileLayoutAtlas {

	int[] simplifiedLayoutArray = new int[256];
	Set<Integer> uniqueLayouts = new LinkedHashSet();

    public TileLayoutAtlas() {
        for (int layoutId = 0; layoutId < 256; layoutId++) {
            TileLayout layoutForId = new TileLayout(layoutId);
			TileLayout simplified = layoutForId.reduceToMeaningfulForm();
			simplifiedLayoutArray[layoutId] = simplified.getId();
			uniqueLayouts.add(simplified.getId());
		}
    }

    public int simplifyLayoutId(int originalLayoutId) {
        return simplifiedLayoutArray[originalLayoutId];
    }


	public Set<Integer> getUniqueLayouts() {
		return uniqueLayouts;
	}
}
