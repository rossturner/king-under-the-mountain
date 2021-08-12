package technology.rocketjump.undermount.sprites;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.google.inject.Inject;
import com.google.inject.Provider;
import technology.rocketjump.undermount.assets.*;
import technology.rocketjump.undermount.mapping.tile.layout.TileLayoutAtlas;

import static technology.rocketjump.undermount.assets.TextureAtlasRepository.TextureAtlasType.DIFFUSE_TERRAIN;

public class DiffuseTerrainSpriteCacheProvider implements Provider<TerrainSpriteCache> {

	private final WallTypeDictionary wallTypeDictionary;
	private final FloorTypeDictionary floorTypeDictionary;
	private final WallQuadrantDictionary wallQuadrantDictionary;
	private final TileLayoutAtlas layoutAtlas;
	private final TextureAtlasRepository textureAtlasRepository;
	private final RoomEdgeTypeDictionary roomEdgeTypeDictionary;
	private final BridgeTypeDictionary bridgeTypeDictionary;
	private final ChannelTypeDictionary channelTypeDictionary;

	@Inject
	public DiffuseTerrainSpriteCacheProvider(WallTypeDictionary wallTypeDictionary, FloorTypeDictionary floorTypeDictionary,
											 WallQuadrantDictionary wallQuadrantDictionary, TileLayoutAtlas layoutAtlas,
											 TextureAtlasRepository textureAtlasRepository, RoomEdgeTypeDictionary roomEdgeTypeDictionary,
											 BridgeTypeDictionary bridgeTypeDictionary, ChannelTypeDictionary channelTypeDictionary) {
		this.wallTypeDictionary = wallTypeDictionary;
		this.floorTypeDictionary = floorTypeDictionary;
		this.wallQuadrantDictionary = wallQuadrantDictionary;
		this.layoutAtlas = layoutAtlas;
		this.textureAtlasRepository = textureAtlasRepository;
		this.roomEdgeTypeDictionary = roomEdgeTypeDictionary;
		this.bridgeTypeDictionary = bridgeTypeDictionary;
		this.channelTypeDictionary = channelTypeDictionary;
	}

	@Override
	public TerrainSpriteCache get() {
		TextureAtlas diffuseTextureAtlas = textureAtlasRepository.get(DIFFUSE_TERRAIN);
		BridgeTileSpriteCache bridgeTileSpriteCache = new BridgeTileSpriteCache(diffuseTextureAtlas, bridgeTypeDictionary);
		return new TerrainSpriteCache(diffuseTextureAtlas, wallTypeDictionary, floorTypeDictionary, wallQuadrantDictionary,
				layoutAtlas, roomEdgeTypeDictionary, bridgeTileSpriteCache, channelTypeDictionary);
	}
}
