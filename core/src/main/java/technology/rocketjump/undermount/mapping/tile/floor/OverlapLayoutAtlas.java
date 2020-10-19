package technology.rocketjump.undermount.mapping.tile.floor;

import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.IntSet;

public class OverlapLayoutAtlas {

    IntMap<OverlapAtlasEntry> library = new IntMap<>(256 * 2);

	IntSet uniqueLayoutIds = new IntSet(64);

    public OverlapLayoutAtlas() {
        for (int layoutId = 0; layoutId < 256; layoutId++) {
            OverlapLayout layoutForId = new OverlapLayout(layoutId);
            OverlapLayout simplified = layoutForId.reduceToMeaningfulForm();

			// Check if simplified unrotated is already in atlas
			if (uniqueLayoutIds.contains(simplified.getId())) {
				library.put(layoutId, new OverlapAtlasEntry(simplified));
				continue;
			}

			// else check if this layout flipped is already in the atlas
			OverlapLayout flippedX = new OverlapLayout(simplified.getId()).flipX();
			if (uniqueLayoutIds.contains(flippedX.getId())) {
				library.put(layoutId, new OverlapAtlasEntry(flippedX, true, false, simplified));
				continue;
			}
			OverlapLayout flippedY = new OverlapLayout(simplified.getId()).flipY();
			if (uniqueLayoutIds.contains(flippedY.getId())) {
				library.put(layoutId, new OverlapAtlasEntry(flippedY, false, true, simplified));
				continue;
			}
			OverlapLayout flippedXY = new OverlapLayout(simplified.getId()).flipX().flipY();
			if (uniqueLayoutIds.contains(flippedXY.getId())) {
				library.put(layoutId, new OverlapAtlasEntry(flippedXY, true, true, simplified));
				continue;
			}

			// Else this is unique so put it in library
			library.put(layoutId, new OverlapAtlasEntry(simplified));
			uniqueLayoutIds.add(simplified.getId());
        }
    }

    public OverlapAtlasEntry getByLayoutId(int layoutId) {
        return library.get(layoutId);
    }

    public IntSet getUniqueLayoutIds() {
        return uniqueLayoutIds;
    }

    public static class OverlapAtlasEntry {

        private final OverlapLayout layout;
        private final boolean flipX;
		private final boolean flipY;
        private final OverlapLayout unflippedLayout;

        public OverlapAtlasEntry(OverlapLayout layout, boolean flipX, boolean flipY, OverlapLayout unflippedLayout) {
            this.layout = layout;
			this.flipX = flipX;
			this.flipY = flipY;
            this.unflippedLayout = unflippedLayout;
        }

        public OverlapAtlasEntry(OverlapLayout layout) {
            this.layout = layout;
			this.flipX = false;
			this.flipY = false;
            this.unflippedLayout = layout;
        }

        public OverlapLayout getLayout() {
            return layout;
        }

        public OverlapLayout getUnflippedLayout() {
            return unflippedLayout;
        }

		public boolean isFlipX() {
			return flipX;
		}

		public boolean isFlipY() {
			return flipY;
		}
	}

}
