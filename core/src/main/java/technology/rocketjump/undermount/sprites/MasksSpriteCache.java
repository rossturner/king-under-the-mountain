package technology.rocketjump.undermount.sprites;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.IntSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.mapping.tile.floor.OverlapLayout;
import technology.rocketjump.undermount.mapping.tile.floor.OverlapLayoutAtlas;

import java.util.HashMap;
import java.util.Map;

import static technology.rocketjump.undermount.assets.TextureAtlasRepository.TextureAtlasType.MASKS_TEXTURE_ATLAS;

@Singleton
public class MasksSpriteCache {

	private final OverlapLayoutAtlas overlapLayoutAtlas;
	private final Map<String, IntMap<Array<Sprite>>> typeToLayoutToSpriteMap = new HashMap<>();

	@Inject
	public MasksSpriteCache(OverlapLayoutAtlas overlapLayoutAtlas, TextureAtlasRepository textureAtlasRepository) {
		this.overlapLayoutAtlas = overlapLayoutAtlas;
		TextureAtlas textureAtlas = textureAtlasRepository.get(MASKS_TEXTURE_ATLAS);

		String overlapType = "organic"; // FIXME MODDING Load these in from the overlap types definition

		IntMap<Array<Sprite>> layoutToSpriteMap = new IntMap<>();
		IntSet.IntSetIterator uniqueLayoutIterator = overlapLayoutAtlas.getUniqueLayoutIds().iterator();
		while (uniqueLayoutIterator.hasNext) {
			int uniqueLayoutId = uniqueLayoutIterator.next();
			if (uniqueLayoutId == 0) {
				continue;
			}
			Array<Sprite> spriteArray = textureAtlas.createSprites(overlapType + "_" + uniqueLayoutId);
			layoutToSpriteMap.put(uniqueLayoutId, spriteArray);

			OverlapLayout originalayout = new OverlapLayout(uniqueLayoutId);

			OverlapLayout flipX = originalayout.flipX();
			Array<Sprite> flippedXSprites = new Array<>(spriteArray.size);
			for (Sprite originalSprite : spriteArray) {
				Sprite flippedXSprite = new Sprite(originalSprite);
				flippedXSprite.setFlip(true, false);
				flippedXSprites.add(flippedXSprite);
			}
			layoutToSpriteMap.put(flipX.getId(), flippedXSprites);

			OverlapLayout flipY = originalayout.flipY();
			Array<Sprite> flippedYSprites = new Array<>(spriteArray.size);
			for (Sprite originalSprite : spriteArray) {
				Sprite flippedYSprite = new Sprite(originalSprite);
				flippedYSprite.setFlip(false, true);
				flippedYSprites.add(flippedYSprite);
			}
			layoutToSpriteMap.put(flipY.getId(), flippedYSprites);

			OverlapLayout flipXY = originalayout.flipX().flipY();
			Array<Sprite> flippedXYSprites = new Array<>(spriteArray.size);
			for (Sprite originalSprite : spriteArray) {
				Sprite flippeXYSprite = new Sprite(originalSprite);
				flippeXYSprite.setFlip(true, true);
				flippedXYSprites.add(flippeXYSprite);
			}
			layoutToSpriteMap.put(flipXY.getId(), flippedXYSprites);

		}
		uniqueLayoutIterator.reset();
		typeToLayoutToSpriteMap.put(overlapType, layoutToSpriteMap);
	}

	public Sprite getMaskForOverlap(OverlapLayout layout, long seed) {
		OverlapLayoutAtlas.OverlapAtlasEntry atlasEntry = overlapLayoutAtlas.getByLayoutId(layout.getId());

		IntMap<Array<Sprite>> layoutToSpriteMap = typeToLayoutToSpriteMap.get("organic");
		// FIXME #87 check entry exists for overlap type, return default sprite otherwise
		// FIXME Passing in unflipped layout here so it works - the atlas entry contains the necessary flipping info so
		// we should use this instead of 4*sprites
		Array<Sprite> spriteArray = layoutToSpriteMap.get(atlasEntry.getUnflippedLayout().getId());
		int spriteNum = Math.abs((int)seed) % spriteArray.size;
		return spriteArray.get(spriteNum);
	}

}
