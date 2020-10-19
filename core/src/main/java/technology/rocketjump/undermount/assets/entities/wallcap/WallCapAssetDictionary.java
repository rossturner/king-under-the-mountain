package technology.rocketjump.undermount.assets.entities.wallcap;

import com.google.inject.Inject;
import com.google.inject.ProvidedBy;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.entities.model.EntityAsset;
import technology.rocketjump.undermount.assets.entities.wallcap.model.WallCapAsset;
import technology.rocketjump.undermount.doors.Doorway;
import technology.rocketjump.undermount.doors.DoorwayOrientation;
import technology.rocketjump.undermount.doors.DoorwaySize;
import technology.rocketjump.undermount.entities.model.physical.furniture.DoorwayEntityAttributes;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ProvidedBy(WallCapAssetDictionaryProvider.class)
@Singleton
public class WallCapAssetDictionary {

	private final Map<String, WallCapAsset> assetsByName = new ConcurrentHashMap<>();
	private final Map<String, Map<GameMaterialType, Map<DoorwayOrientation, Map<DoorwaySize, WallCapAsset>>>> typedMap = new ConcurrentHashMap<>();

	@Inject
	public WallCapAssetDictionary(List<WallCapAsset> completeAssetList) {
		for (WallCapAsset asset : completeAssetList) {
			assetsByName.put(asset.getUniqueName(), asset);

			typedMap.computeIfAbsent(asset.getWallTypeName(), e -> new ConcurrentHashMap<>());

			addToMapByMaterialType(asset, asset.getDoorwayMaterialType(), typedMap.get(asset.getWallTypeName()));
		}
	}

	public WallCapAsset getMatching(Doorway doorway, DoorwayEntityAttributes wallCapAttributes) {
		// TODO Need to return placeholder cap if none matched
//		Map<GameMaterialType, Map<DoorwayOrientation, Map<DoorwaySize, WallCapAsset>>> materialMap = typedMap.get(wallCapAttributes.getAttachedWallType().getWallTypeId());

		// FIXME Forcing to only match against rough stone wall types
		Map<GameMaterialType, Map<DoorwayOrientation, Map<DoorwaySize, WallCapAsset>>> materialMap = typedMap.values().iterator().next();
		if (materialMap == null) {
			return null;
		}
		Map<DoorwayOrientation, Map<DoorwaySize, WallCapAsset>> orientationMap = materialMap.get(doorway.getDoorwayMaterialType());
		if (orientationMap == null) {
			return null;
		}
		Map<DoorwaySize, WallCapAsset> sizeMap = orientationMap.get(doorway.getOrientation());
		if (sizeMap == null) {
			return null;
		}
		return sizeMap.get(doorway.getDoorwaySize());
	}


	private void addToMapByMaterialType(WallCapAsset asset, GameMaterialType doorwayMaterialType, Map<GameMaterialType, Map<DoorwayOrientation, Map<DoorwaySize, WallCapAsset>>> assetMap) {
		if (doorwayMaterialType == null) {
			// Add to all material types
			for (GameMaterialType gameMaterialType : GameMaterialType.values()) {
				addToMapByMaterialType(asset, gameMaterialType, assetMap);
			}
			return;
		}


		if (!assetMap.containsKey(doorwayMaterialType)) {
			assetMap.put(doorwayMaterialType, new ConcurrentHashMap<>());
		}

		addToMapByOrientation(asset, assetMap.get(doorwayMaterialType));
	}

	private void addToMapByOrientation(WallCapAsset asset, Map<DoorwayOrientation, Map<DoorwaySize, WallCapAsset>> assetMap) {
		if (!assetMap.containsKey(asset.getDoorwayOrientation())) {
			assetMap.put(asset.getDoorwayOrientation(), new ConcurrentHashMap<>());
		}

		addToMapByDoorwaySize(asset, assetMap.get(asset.getDoorwayOrientation()));
	}

	private void addToMapByDoorwaySize(WallCapAsset asset, Map<DoorwaySize, WallCapAsset> assetMap) {
		if (!assetMap.containsKey(asset.getDoorwaySize())) {
			assetMap.put(asset.getDoorwaySize(), asset);
		} else {
			throw new RuntimeException("Duplicated wall cap asset: " + asset.toString());
		}
	}


	public Map<? extends String, ? extends EntityAsset> getAll() {
		return assetsByName;
	}
}
