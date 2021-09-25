package technology.rocketjump.undermount.sprites;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.*;
import technology.rocketjump.undermount.mapping.tile.layout.TileLayoutAtlas;

import static technology.rocketjump.undermount.assets.TextureAtlasRepository.TextureAtlasType.NORMAL_TERRAIN;

@Singleton
public class NormalTerrainSpriteCacheProvider implements Provider<TerrainSpriteCache> {

	private final WallTypeDictionary wallTypeDictionary;
	private final FloorTypeDictionary floorTypeDictionary;
	private final WallQuadrantDictionary wallQuadrantDictionary;
	private final TileLayoutAtlas layoutAtlas;
	private final TextureAtlasRepository textureAtlasRepository;
	private final RoomEdgeTypeDictionary roomEdgeTypeDictionary;
	private final BridgeTypeDictionary bridgeTypeDictionary;
	private final ChannelTypeDictionary channelTypeDictionary;
	private final TerrainSpriteCache instance;

	@Inject
	public NormalTerrainSpriteCacheProvider(WallTypeDictionary wallTypeDictionary, FloorTypeDictionary floorTypeDictionary,
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

		TextureAtlas normalTextureAtlas = textureAtlasRepository.get(NORMAL_TERRAIN);
		BridgeTileSpriteCache bridgeTileSpriteCache = new BridgeTileSpriteCache(normalTextureAtlas, bridgeTypeDictionary);
		instance = new TerrainSpriteCache(normalTextureAtlas, wallTypeDictionary, floorTypeDictionary, wallQuadrantDictionary,
				layoutAtlas, roomEdgeTypeDictionary, bridgeTileSpriteCache, channelTypeDictionary);
	}

	@Override
	public TerrainSpriteCache get() {
		return instance;
	}
}
