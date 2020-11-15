package technology.rocketjump.undermount.sprites;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class IconSpriteCache {

	private final TextureAtlas textureAtlas;
	private final Map<String, Sprite> iconsByName = new HashMap<>();

	@Inject
	public IconSpriteCache(TextureAtlasRepository textureAtlasRepository) {
		textureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS);
	}

	public Sprite getByName(String iconName) {
		return iconsByName.computeIfAbsent(iconName, a -> {
			Sprite sprite = textureAtlas.createSprite(iconName);
			if (sprite == null) {
				sprite = textureAtlas.createSprite("placeholder");
			}
			return sprite;
		});
	}
}
