package technology.rocketjump.undermount.sprites;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.google.inject.Inject;
import technology.rocketjump.undermount.assets.FloorTypeDictionary;
import technology.rocketjump.undermount.assets.RoomEdgeTypeDictionary;
import technology.rocketjump.undermount.assets.WallQuadrantDictionary;
import technology.rocketjump.undermount.assets.WallTypeDictionary;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.assets.model.RoomEdgeType;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.mapping.tile.layout.RoomTileLayout;
import technology.rocketjump.undermount.mapping.tile.layout.TileLayout;
import technology.rocketjump.undermount.mapping.tile.layout.TileLayoutAtlas;
import technology.rocketjump.undermount.rooms.Bridge;
import technology.rocketjump.undermount.sprites.model.BridgeTileLayout;
import technology.rocketjump.undermount.sprites.model.QuadrantSprites;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TerrainSpriteCache {

	private final Map<Long, Map<Integer, Array<Sprite>>> wallTypeIdToWallLayoutToSpriteMap = new ConcurrentHashMap<>();
	private final Map<Long, Map<Integer, Sprite>> roomEdgeTypeIdToLayoutToSpriteMap = new ConcurrentHashMap<>();
	private final Map<Long, Array<Sprite>> floorTypeIdToFloorSprites = new ConcurrentHashMap<>();
	private final WallQuadrantDictionary wallQuadrantDictionary;
	private final TileLayoutAtlas tileLayoutAtlas;
	private final BridgeTileSpriteCache bridgeTileSpriteCache;

	@Inject
	public TerrainSpriteCache(TextureAtlas textureAtlas, WallTypeDictionary wallTypeDictionary, FloorTypeDictionary floorTypeDictionary,
							  WallQuadrantDictionary wallQuadrantDictionary, TileLayoutAtlas tileLayoutAtlas,
							  RoomEdgeTypeDictionary roomEdgeTypeDictionary, BridgeTileSpriteCache bridgeTileSpriteCache) {
		this.wallQuadrantDictionary = wallQuadrantDictionary;
		this.tileLayoutAtlas = tileLayoutAtlas;
		this.bridgeTileSpriteCache = bridgeTileSpriteCache;

		// Populate wall sprites for each material
		for (WallType wallType : wallTypeDictionary.getAllDefinitions()) {
			Map<Integer, Array<Sprite>> spriteMapForMaterial = new ConcurrentHashMap<>();
			for (Integer wallLayoutIdMatchingSprite : wallQuadrantDictionary.getUniqueQuadrantIds()) {
				Array<Sprite> wallSprites = textureAtlas.createSprites(wallType.getWallTypeName() + "_" + wallLayoutIdMatchingSprite);
				spriteMapForMaterial.put(wallLayoutIdMatchingSprite, wallSprites);
			}
			wallTypeIdToWallLayoutToSpriteMap.put(wallType.getWallTypeId(), spriteMapForMaterial);
		}

		// Populate each room edge sprite
		for (RoomEdgeType roomEdgeType : roomEdgeTypeDictionary.getAllDefinitions()) {
			Map<Integer, Sprite> spriteMapForMaterial = new ConcurrentHashMap<>();
			Set<Integer> uniqueQuadrantIds = wallQuadrantDictionary.getUniqueQuadrantIds();
			for (Integer layoutIdMatchingSprite : uniqueQuadrantIds) {
				if (layoutIdMatchingSprite == 255) {
					continue;
				}
				Sprite roomEdgeSprite = textureAtlas.createSprite(roomEdgeType.getRoomEdgeTypeName(), layoutIdMatchingSprite);
				if (roomEdgeSprite != null) {
					// Can't place a null value into a ConcurrentHashMap (unlike LongMap)
					spriteMapForMaterial.put(layoutIdMatchingSprite, roomEdgeSprite);
				}
			}
			roomEdgeTypeIdToLayoutToSpriteMap.put(roomEdgeType.getRoomEdgeTypeId(), spriteMapForMaterial);
		}

		// Populate floor sprites for each material
		for (FloorType floorType : floorTypeDictionary.getAllDefinitions()) {
			floorTypeIdToFloorSprites.put(floorType.getFloorTypeId(), textureAtlas.createSprites(floorType.getFloorTypeName()));
		}
	}

	public QuadrantSprites getSpritesForWall(WallType wallType, TileLayout wallLayout, long seed) {
		int simplifiedLayoutId = tileLayoutAtlas.simplifyLayoutId(wallLayout.getId());
		// FIXME Disabling until I can figure out why single sprite layout 66 isn't working
//		if (wallQuadrantDictionary.getUniqueQuadrantIds().contains(simplifiedLayoutId)) {
//			return new QuadrantSprites(getWallSpriteForLayoutAndType(simplifiedLayoutId, wall.getMaterialType()));
//		} else {
			IntArray wallLayoutQuadrants = wallQuadrantDictionary.getWallQuadrants(simplifiedLayoutId);
			return new QuadrantSprites(
					getWallSpriteForLayoutAndType(wallLayoutQuadrants.get(0), wallType, seed),
					getWallSpriteForLayoutAndType(wallLayoutQuadrants.get(1), wallType, seed),
					getWallSpriteForLayoutAndType(wallLayoutQuadrants.get(2), wallType, seed),
					getWallSpriteForLayoutAndType(wallLayoutQuadrants.get(3), wallType, seed)
			);
//		}
	}

	public QuadrantSprites getSpritesForRoomEdge(RoomEdgeType roomEdgeType, RoomTileLayout layout) {
		int simplifiedLayoutId = tileLayoutAtlas.simplifyLayoutId(layout.getId());
		IntArray wallLayoutQuadrants = wallQuadrantDictionary.getWallQuadrants(simplifiedLayoutId);
		return new QuadrantSprites(
				getRoomEdgeSpriteForLayoutAndType(wallLayoutQuadrants.get(0), roomEdgeType),
				getRoomEdgeSpriteForLayoutAndType(wallLayoutQuadrants.get(1), roomEdgeType),
				getRoomEdgeSpriteForLayoutAndType(wallLayoutQuadrants.get(2), roomEdgeType),
				getRoomEdgeSpriteForLayoutAndType(wallLayoutQuadrants.get(3), roomEdgeType)
		);
	}

	public Sprite getWallSpriteForLayoutAndType(int wallQuadrantLayoutId, WallType wallType, long seed) {
		Map<Integer, Array<Sprite>> layoutToSpriteMap = wallTypeIdToWallLayoutToSpriteMap.get(wallType.getWallTypeId());
		// FIXME #87 check entry exists for material, return default sprite otherwise
		Array<Sprite> spritesForLayout = layoutToSpriteMap.get(wallQuadrantLayoutId);
		if (spritesForLayout.size == 1) {
			return spritesForLayout.get(0);
		} else {
			int spriteNum = Math.abs((int)seed) % spritesForLayout.size;
			return spritesForLayout.get(spriteNum);
		}
	}

	public Sprite getRoomEdgeSpriteForLayoutAndType(int wallQuadrantLayoutId, RoomEdgeType roomEdgeType) {
		if (wallQuadrantLayoutId == 255) {
			return null;
		}
		Map<Integer, Sprite> layoutToSpriteMap = roomEdgeTypeIdToLayoutToSpriteMap.get(roomEdgeType.getRoomEdgeTypeId());
		// FIXME #87 check entry exists for material, return default sprite otherwise
		return layoutToSpriteMap.get(wallQuadrantLayoutId);
	}

	public Sprite getFloorSpriteForType(FloorType floorType, long seed) {
		Array<Sprite> sprites = floorTypeIdToFloorSprites.get(floorType.getFloorTypeId());
		// FIXME #87 check entry exists for given material
		int spriteNum = Math.abs((int)seed) % sprites.size;
		return sprites.get(spriteNum);
	}

	public Sprite getForBridge(Bridge bridge, BridgeTileLayout tileLayout) {
		return bridgeTileSpriteCache.getForBridge(bridge.getMaterial().getMaterialType(), bridge.getOrientation(), tileLayout);
	}

}
