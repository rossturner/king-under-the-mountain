package technology.rocketjump.undermount.assets.entities.furniture;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.SpriteDescriptor;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureLayoutDictionary;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.rendering.RenderMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static technology.rocketjump.undermount.assets.TextureAtlasRepository.TextureAtlasType.DIFFUSE_ENTITIES;
import static technology.rocketjump.undermount.assets.TextureAtlasRepository.TextureAtlasType.NORMAL_ENTITIES;
import static technology.rocketjump.undermount.assets.entities.creature.CreatureEntityAssetDictionaryProvider.addAnimatedSpriteArray;
import static technology.rocketjump.undermount.assets.entities.creature.CreatureEntityAssetDictionaryProvider.addSprite;

@Singleton
public class FurnitureEntityAssetDictionaryProvider implements Provider<FurnitureEntityAssetDictionary> {

	private final TextureAtlasRepository textureAtlasRepository;
	private final FurnitureTypeDictionary typeDictionary;
	private final FurnitureLayoutDictionary layoutDictionary;
	private final EntityAssetTypeDictionary assetTypeDictionary;

	private FurnitureEntityAssetDictionary instance;

	@Inject
	public FurnitureEntityAssetDictionaryProvider(TextureAtlasRepository textureAtlasRepository,
												  FurnitureTypeDictionary typeDictionary, FurnitureLayoutDictionary layoutDictionary,
												  EntityAssetTypeDictionary assetTypeDictionary) {
		this.textureAtlasRepository = textureAtlasRepository;
		this.typeDictionary = typeDictionary;
		this.layoutDictionary = layoutDictionary;
		this.assetTypeDictionary = assetTypeDictionary;
	}

	@Override
	public FurnitureEntityAssetDictionary get() {
		if (instance == null) {
			instance = create();
		}
		return instance;
	}

	private FurnitureEntityAssetDictionary create() {
		TextureAtlas diffuseTextureAtlas = textureAtlasRepository.get(DIFFUSE_ENTITIES);
		TextureAtlas normalTextureAtlas = textureAtlasRepository.get(NORMAL_ENTITIES);
		FileHandle entityDefinitionsFile = Gdx.files.internal("assets/definitions/entityAssets/furnitureEntityAssets.json");
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			List<FurnitureEntityAsset> assetList = objectMapper.readValue(entityDefinitionsFile.readString(),
					objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, FurnitureEntityAsset.class));

			for (FurnitureEntityAsset asset : assetList) {
				for (SpriteDescriptor spriteDescriptor : asset.getSpriteDescriptors().values()) {
					if (spriteDescriptor.getIsAnimated()) {
						addAnimatedSpriteArray(spriteDescriptor, diffuseTextureAtlas, RenderMode.DIFFUSE);
						addAnimatedSpriteArray(spriteDescriptor, normalTextureAtlas, RenderMode.NORMALS);
					} else {
						addSprite(spriteDescriptor, diffuseTextureAtlas, RenderMode.DIFFUSE);
						addSprite(spriteDescriptor, normalTextureAtlas, RenderMode.NORMALS);
					}
				}
			}

			return new FurnitureEntityAssetDictionary(assetList, assetTypeDictionary, typeDictionary, layoutDictionary);
		} catch (IOException e) {
			// TODO better exception handling
			throw new RuntimeException(e);
		}
	}

}
