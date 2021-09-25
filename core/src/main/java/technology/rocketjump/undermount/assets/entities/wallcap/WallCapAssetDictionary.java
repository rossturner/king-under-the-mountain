package technology.rocketjump.undermount.assets.entities.wallcap;

import com.google.inject.Inject;
import com.google.inject.ProvidedBy;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.WallTypeDictionary;
import technology.rocketjump.undermount.assets.entities.model.EntityAsset;
import technology.rocketjump.undermount.assets.entities.wallcap.model.WallCapAsset;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.doors.Doorway;
import technology.rocketjump.undermount.doors.DoorwayOrientation;
import technology.rocketjump.undermount.doors.DoorwaySize;
import technology.rocketjump.undermount.entities.model.physical.furniture.DoorwayEntityAttributes;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ProvidedBy(WallCapAssetDictionaryProvider.class)
@Singleton
public class WallCapAssetDictionary {

	private final Map<String, WallCapAsset> assetsByName = new ConcurrentHashMap<>();
	private final Map<WallType, Map<DoorwayOrientation, Map<DoorwaySize, WallCapAsset>>> assetsByAttachedWallTypeByOrientation = new ConcurrentHashMap<>();
	private WallCapAsset placeholder = new WallCapAsset();

	@Inject
	public WallCapAssetDictionary(List<WallCapAsset> completeAssetList, WallTypeDictionary wallTypeDictionary) {
		for (WallCapAsset asset : completeAssetList) {
			WallType wallType = wallTypeDictionary.getByWallTypeName(asset.getWallTypeName());
			if (wallType == null) {
				Logger.error("Could not find wall type with name " + asset.getWallTypeName() + " for " + asset.getUniqueName());
				continue;
			}

			assetsByAttachedWallTypeByOrientation.computeIfAbsent(wallType, a -> new ConcurrentHashMap<>())
					.computeIfAbsent(asset.getDoorwayOrientation(), a -> new ConcurrentHashMap<>())
					.put(asset.getDoorwaySize(), asset);

			assetsByName.put(asset.getUniqueName(), asset);
		}
	}

	public WallCapAsset getMatching(Doorway doorway, DoorwayEntityAttributes wallCapAttributes) {
		Map<DoorwayOrientation, Map<DoorwaySize, WallCapAsset>> byOrientation = assetsByAttachedWallTypeByOrientation.get(wallCapAttributes.getAttachedWallType());
		if (byOrientation == null) {
			return placeholder;
		}
		Map<DoorwaySize, WallCapAsset> bySize = byOrientation.get(doorway.getOrientation());
		if (bySize == null) {
			return placeholder;
		}
		WallCapAsset wallCapAsset = bySize.get(doorway.getDoorwaySize());
		if (wallCapAsset == null) {
			return placeholder;
		} else {
			return wallCapAsset;
		}
	}

	public Map<? extends String, ? extends EntityAsset> getAll() {
		return assetsByName;
	}
}
