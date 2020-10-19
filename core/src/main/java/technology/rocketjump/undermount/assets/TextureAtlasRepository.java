package technology.rocketjump.undermount.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.google.inject.Singleton;

import java.util.EnumMap;

/**
 * This class is be the single point of creation (and disposal) for TextureAtlas instances
 */
@Singleton
public class TextureAtlasRepository implements AssetDisposable {

	private final EnumMap<TextureAtlasType, TextureAtlas> typeMap = new EnumMap<>(TextureAtlasType.class);

	public TextureAtlasRepository() {
		for (TextureAtlasType textureAtlasType : TextureAtlasType.values()) {
			typeMap.put(textureAtlasType, new TextureAtlas(new FileHandle("assets/tilesets/" + textureAtlasType.getFilename())));
		}
	}

	public TextureAtlas get(TextureAtlasType type) {
		return typeMap.get(type);
	}

	@Override
	public void dispose() {
		typeMap.values().forEach(TextureAtlas::dispose);
	}

	public enum TextureAtlasType {

		DIFFUSE_ENTITIES("diffuse-entities.atlas"),
		NORMAL_ENTITIES("normal-entities.atlas"),
		DIFFUSE_TERRAIN("diffuse-terrain.atlas"),
		NORMAL_TERRAIN("normal-terrain.atlas"),
		GUI_TEXTURE_ATLAS("gui.atlas"),
		MASKS_TEXTURE_ATLAS("masks.atlas");

		private final String filename;

		TextureAtlasType(String filename) {
			this.filename = filename;
		}

		public String getFilename() {
			return filename;
		}
	}
}
