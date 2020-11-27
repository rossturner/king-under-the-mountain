package technology.rocketjump.undermount.sprites;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.OverlapTypeDictionary;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.assets.model.OverlapType;
import technology.rocketjump.undermount.mapping.tile.floor.OverlapLayout;
import technology.rocketjump.undermount.mapping.tile.floor.OverlapLayoutAtlas;
import technology.rocketjump.undermount.mapping.tile.floor.OverlapQuadrantDictionary;
import technology.rocketjump.undermount.sprites.model.QuadrantSprites;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static technology.rocketjump.undermount.assets.TextureAtlasRepository.TextureAtlasType.MASKS_TEXTURE_ATLAS;

@Singleton
public class MasksSpriteCache {

	private final OverlapLayoutAtlas overlapLayoutAtlas;
	private final Map<OverlapType, IntMap<Array<Sprite>>> typeToLayoutToSpriteMap = new HashMap<>();
	private final OverlapQuadrantDictionary overlapQuadrantDictionary;
	private final Sprite placeholder;

	@Inject
	public MasksSpriteCache(OverlapLayoutAtlas overlapLayoutAtlas, TextureAtlasRepository textureAtlasRepository,
							OverlapTypeDictionary overlapTypeDictionary, OverlapQuadrantDictionary overlapQuadrantDictionary) {
		this.overlapLayoutAtlas = overlapLayoutAtlas;
		this.overlapQuadrantDictionary = overlapQuadrantDictionary;
		TextureAtlas textureAtlas = textureAtlasRepository.get(MASKS_TEXTURE_ATLAS);
		placeholder = textureAtlas.createSprite("placeholder");

		for (OverlapType overlapType : overlapTypeDictionary.getAll()) {
			IntMap<Array<Sprite>> layoutToSpriteMap = new IntMap<>();
			for (Integer uniqueLayoutId : Arrays.asList(24, 66, 90, 165)) {
				Array<Sprite> spriteArray = textureAtlas.createSprites(overlapType + "_" + uniqueLayoutId);
				layoutToSpriteMap.put(uniqueLayoutId, spriteArray);

			}
			typeToLayoutToSpriteMap.put(overlapType, layoutToSpriteMap);
		}

	}

	public Sprite getMaskForOverlap(OverlapType type, OverlapLayout layout, long seed) {
		return getMaskForOverlap(type, layout.getId(), seed);
	}

	public Sprite getMaskForOverlap(OverlapType type, int layoutId, long seed) {
		OverlapLayoutAtlas.OverlapAtlasEntry atlasEntry = overlapLayoutAtlas.getByLayoutId(layoutId);

		IntMap<Array<Sprite>> layoutToSpriteMap = typeToLayoutToSpriteMap.get(type);
		if (layoutToSpriteMap == null) {
			Logger.error("No entry for overlap type " + type.getOverlapName());
			return placeholder;
		}
		// FIXME #87 check entry exists for overlap type, return default sprite otherwise
		Array<Sprite> spriteArray = layoutToSpriteMap.get(atlasEntry.getUnflippedLayout().getId());
		if (spriteArray == null || spriteArray.isEmpty()) {
			return placeholder;
		}
		int spriteNum = Math.abs((int)seed) % spriteArray.size;
		return spriteArray.get(spriteNum);
	}

	public QuadrantSprites getMasksForOverlap(OverlapType type, OverlapLayout layout, long seed) {
		IntArray wallLayoutQuadrants = overlapQuadrantDictionary.getOverlapQuadrants(layout.getId());
		return new QuadrantSprites(
				getMaskForOverlap(type, wallLayoutQuadrants.get(0), seed),
				getMaskForOverlap(type, wallLayoutQuadrants.get(1), seed),
				getMaskForOverlap(type, wallLayoutQuadrants.get(2), seed),
				getMaskForOverlap(type, wallLayoutQuadrants.get(3), seed)
		);
	}

}
