package technology.rocketjump.undermount.assets;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.IntArray;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.mapping.tile.layout.WallLayout;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class WallQuadrantDictionary {

	private final Map<Integer, IntArray> layoutToQuadrantArray = new ConcurrentHashMap<>();
	private final Set<Integer> uniqueQuadrantIds = new HashSet<>();

	@Inject
	public WallQuadrantDictionary() throws IOException {
		this(Gdx.files.internal("assets/terrain/wallLayoutQuadrants.json"));
	}

	public WallQuadrantDictionary(FileHandle wallLayoutQuadrantsJson) throws IOException {
		JSONObject jsonFile = JSON.parseObject(wallLayoutQuadrantsJson.readString());

		for (String layoutId : jsonFile.keySet()) {
			JSONArray quadrants = jsonFile.getJSONArray(layoutId);
			if (quadrants.size() != 4) {
				throw new IOException("Unexpected values in " + wallLayoutQuadrantsJson.file().getName() + ": " + quadrants.toString());
			}
			IntArray quadrantArray = new IntArray(4);
			quadrantArray.add(quadrants.getIntValue(0));
			uniqueQuadrantIds.add(quadrants.getIntValue(0));
			quadrantArray.add(quadrants.getIntValue(1));
			uniqueQuadrantIds.add(quadrants.getIntValue(1));
			quadrantArray.add(quadrants.getIntValue(2));
			uniqueQuadrantIds.add(quadrants.getIntValue(2));
			quadrantArray.add(quadrants.getIntValue(3));
			uniqueQuadrantIds.add(quadrants.getIntValue(3));

			layoutToQuadrantArray.put(Integer.parseInt(layoutId), quadrantArray);
		}
	}

	public IntArray getWallQuadrants(int simplifiedLayoutId) {
		if (!layoutToQuadrantArray.containsKey(simplifiedLayoutId)) {
			WallLayout simplifiedLayout = new WallLayout(simplifiedLayoutId);
			throw new RuntimeException("WallQuadrantDictionary does not have an entry for layout " + simplifiedLayoutId + "\n" + simplifiedLayout.toString());
		}
		return layoutToQuadrantArray.get(simplifiedLayoutId);
	}

	public Set<Integer> getUniqueQuadrantIds() {
		return uniqueQuadrantIds; // Effectively [ 0, 24, 66, 90, 255 ]
	}

}
