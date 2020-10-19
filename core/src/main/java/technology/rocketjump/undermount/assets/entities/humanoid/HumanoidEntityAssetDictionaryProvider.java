package technology.rocketjump.undermount.assets.entities.humanoid;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.assets.entities.humanoid.model.HumanoidEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.SpriteDescriptor;
import technology.rocketjump.undermount.rendering.RenderMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static technology.rocketjump.undermount.assets.TextureAtlasRepository.TextureAtlasType.DIFFUSE_ENTITIES;
import static technology.rocketjump.undermount.assets.TextureAtlasRepository.TextureAtlasType.NORMAL_ENTITIES;

public class HumanoidEntityAssetDictionaryProvider implements Provider<HumanoidEntityAssetDictionary> {

	public static final float EPSILON = 0.0001f;

	private final EntityAssetTypeDictionary entityAssetTypeDictionary;
	private final TextureAtlasRepository textureAtlasRepository;

	@Inject
	public HumanoidEntityAssetDictionaryProvider(EntityAssetTypeDictionary entityAssetTypeDictionary, TextureAtlasRepository textureAtlasRepository) {
		this.entityAssetTypeDictionary = entityAssetTypeDictionary;
		this.textureAtlasRepository = textureAtlasRepository;
	}

	@Override
	public HumanoidEntityAssetDictionary get() {
		TextureAtlas diffuseTextureAtlas = textureAtlasRepository.get(DIFFUSE_ENTITIES);
		TextureAtlas normalTextureAtlas = textureAtlasRepository.get(NORMAL_ENTITIES);
		FileHandle entityDefinitionsFile = Gdx.files.internal("assets/definitions/entityAssets/humanoidEntityAssets.json");
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			List<HumanoidEntityAsset> assetList = objectMapper.readValue(entityDefinitionsFile.readString(),
					objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, HumanoidEntityAsset.class));

			for (HumanoidEntityAsset asset : assetList) {
				for (SpriteDescriptor spriteDescriptor : asset.getSpriteDescriptors().values()) {
					addSprite(spriteDescriptor, diffuseTextureAtlas, RenderMode.DIFFUSE);
					addSprite(spriteDescriptor, normalTextureAtlas, RenderMode.NORMALS);
				}
			}

			return new HumanoidEntityAssetDictionary(assetList, entityAssetTypeDictionary);
		} catch (IOException e) {
			// TODO better exception handling
			throw new RuntimeException(e);
		}

	}

	public static void addSprite(SpriteDescriptor spriteDescriptor, TextureAtlas textureAtlas, RenderMode renderMode) {
		String filename = spriteDescriptor.getFilename();
		if (filename.endsWith("_0.png")) {
			filename = filename.replace("_0.png", "");
		}
		if (filename.endsWith(".png")) {
			filename = filename.substring(0, filename.length() - 4);
		}

		Sprite sprite = textureAtlas.createSprite(filename);
		if (sprite == null) {
			// No sprite found
			Logger.error("Warning: No sprite found for name " + filename + " in render mode " + renderMode.name());
			return;
		}
		sprite.setFlip(spriteDescriptor.isFlipX(), spriteDescriptor.isFlipY());

		if (spriteDescriptor.getScale() < EPSILON) {
			spriteDescriptor.setScale(1.0f);
		}

		spriteDescriptor.setSprite(renderMode, sprite);
	}

	public static void addAnimatedSpriteArray(SpriteDescriptor spriteDescriptor, TextureAtlas textureAtlas, RenderMode renderMode) {
		String filename = spriteDescriptor.getFilename();
		if (filename.endsWith(".png")) {
			throw new RuntimeException("Animated sprite filename should not end with .png, found " + spriteDescriptor.getFilename());
		}

		Array<Sprite> spriteArray = textureAtlas.createSprites(filename);
		if (spriteArray == null) {
			// No sprite found
			Logger.error("Warning: No sprite found for name " + filename + " in render mode " + renderMode.name());
			return;
		}
		for (Sprite sprite : spriteArray) {
			sprite.setFlip(spriteDescriptor.isFlipX(), spriteDescriptor.isFlipY());
		}

		if (spriteDescriptor.getScale() < EPSILON) {
			spriteDescriptor.setScale(1.0f);
		}

		spriteDescriptor.setAnimatedSprites(renderMode, spriteArray);
	}
}

