package technology.rocketjump.undermount.assets.entities.mechanism;

import technology.rocketjump.undermount.assets.entities.mechanism.model.MechanismEntityAsset;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismEntityAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MechanismEntityAssetsByLayout {

	private Map<Integer, List<MechanismEntityAsset>> layoutMap = new HashMap<>();

	public MechanismEntityAssetsByLayout() {
	}

	public void add(MechanismEntityAsset asset) {
		Integer layoutId = asset.getLayoutId();
		if (layoutId == null) {
			layoutId = 0;
		}
		layoutMap.computeIfAbsent(layoutId, a -> new ArrayList<>()).add(asset);
	}

	public MechanismEntityAsset get(MechanismEntityAttributes attributes) {
		Integer layoutId = attributes.getPipeLayout() == null ? 0 : attributes.getPipeLayout().getId();
		List<MechanismEntityAsset> assets = layoutMap.getOrDefault(layoutId, List.of());
		if (assets.isEmpty()) {
			return null;
		} else if (assets.size() == 1) {
			return assets.get(0);
		} else {
			return assets.get(assets.size() % (int)attributes.getSeed());
		}
	}

	public List<MechanismEntityAsset> getAll(MechanismEntityAttributes attributes) {
		Integer layoutId = attributes.getPipeLayout() == null ? 0 : attributes.getPipeLayout().getId();
		return layoutMap.getOrDefault(layoutId, List.of());
	}

//	public MechanismEntityAssetsByAssetType getAssetTypeMapByQuantity(int quantity) {
//		return layoutMap.get(quantity);
//	}

}
