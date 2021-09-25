package technology.rocketjump.undermount.assets.entities.item;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.SpriteDescriptor;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.rendering.RenderMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static technology.rocketjump.undermount.assets.TextureAtlasRepository.TextureAtlasType.DIFFUSE_ENTITIES;
import static technology.rocketjump.undermount.assets.TextureAtlasRepository.TextureAtlasType.NORMAL_ENTITIES;
import static technology.rocketjump.undermount.assets.entities.humanoid.HumanoidEntityAssetDictionaryProvider.addSprite;

@Singleton
public class ItemEntityAssetDictionaryProvider implements Provider<ItemEntityAssetDictionary> {

	private final EntityAssetTypeDictionary entityAssetTypeDictionary;
	private final TextureAtlasRepository textureAtlasRepository;
	private final ItemTypeDictionary itemTypeDictionary;
	private ItemEntityAssetDictionary instance;

	@Inject
	public ItemEntityAssetDictionaryProvider(EntityAssetTypeDictionary entityAssetTypeDictionary,
											 TextureAtlasRepository textureAtlasRepository, ItemTypeDictionary itemTypeDictionary) {
		this.entityAssetTypeDictionary = entityAssetTypeDictionary;
		this.textureAtlasRepository = textureAtlasRepository;
		this.itemTypeDictionary = itemTypeDictionary;
	}

	@Override
	public ItemEntityAssetDictionary get() {
		if (instance == null) {
			instance = create();
		}
		return instance;
	}


	public ItemEntityAssetDictionary create() {
		TextureAtlas diffuseTextureAtlas = textureAtlasRepository.get(DIFFUSE_ENTITIES);
		TextureAtlas normalTextureAtlas = textureAtlasRepository.get(NORMAL_ENTITIES);
		FileHandle entityDefinitionsFile = Gdx.files.internal("assets/definitions/entityAssets/itemEntityAssets.json");
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			List<ItemEntityAsset> assetList = objectMapper.readValue(entityDefinitionsFile.readString(),
					objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, ItemEntityAsset.class));

			for (ItemEntityAsset asset : assetList) {
				for (SpriteDescriptor spriteDescriptor : asset.getSpriteDescriptors().values()) {
					addSprite(spriteDescriptor, diffuseTextureAtlas, RenderMode.DIFFUSE);
					addSprite(spriteDescriptor, normalTextureAtlas, RenderMode.NORMALS);
				}
			}

			return new ItemEntityAssetDictionary(assetList, entityAssetTypeDictionary, itemTypeDictionary);
		} catch (IOException e) {
			// TODO better exception handling
			throw new RuntimeException(e);
		}
	}

}
