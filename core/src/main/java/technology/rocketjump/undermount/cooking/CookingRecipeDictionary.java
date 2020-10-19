package technology.rocketjump.undermount.cooking;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.cooking.model.CookingRecipe;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeWithMaterial;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;

import java.io.IOException;
import java.util.*;

@Singleton
public class CookingRecipeDictionary {

	private final Map<String, CookingRecipe> byName = new HashMap<>();
	private final ItemTypeDictionary itemTypeDictionary;
	private final GameMaterialDictionary materialDictionary;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final SoundAssetDictionary soundAssetDictionary;

	@Inject
	public CookingRecipeDictionary(ItemTypeDictionary itemTypeDictionary, GameMaterialDictionary materialDictionary,
								   FurnitureTypeDictionary furnitureTypeDictionary, SoundAssetDictionary soundAssetDictionary) throws IOException {
		this.itemTypeDictionary = itemTypeDictionary;
		this.materialDictionary = materialDictionary;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.soundAssetDictionary = soundAssetDictionary;

		FileHandle cookingRecipesJsonFile = Gdx.files.internal("assets/definitions/crafting/cookingRecipes.json");
		ObjectMapper objectMapper = new ObjectMapper();
		List<CookingRecipe> cookingRecipes = objectMapper.readValue(cookingRecipesJsonFile.readString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, CookingRecipe.class));

		for (CookingRecipe cookingRecipe : cookingRecipes) {
			initCookingRecipe(cookingRecipe);
			byName.put(cookingRecipe.getRecipeName(), cookingRecipe);
		}
	}

	public Collection<CookingRecipe> getAll() {
		return byName.values();
	}

	public CookingRecipe getByName(String cookingRecipeName) {
		return byName.get(cookingRecipeName);
	}

	private void initCookingRecipe(CookingRecipe cookingRecipe) {
		if (cookingRecipe.getCookedInFurnitureName() == null) {
			Logger.error("cookedInFurnitureName must not be null in " + cookingRecipe.getRecipeName());
			return;
		} else {
			FurnitureType furnitureType = furnitureTypeDictionary.getByName(cookingRecipe.getCookedInFurnitureName());
			if (furnitureType == null) {
				Logger.error("Could not find furniture type with name " + cookingRecipe.getCookedInFurnitureName() + " required for recipe " + cookingRecipe.getRecipeName());
			} else {
				cookingRecipe.setCookedInFurniture(furnitureType);
			}
		}


		for (ItemTypeWithMaterial itemTypeWithMaterial : cookingRecipe.getInputItemOptions()) {
			initialise(itemTypeWithMaterial, cookingRecipe);
		}
		for (ItemTypeWithMaterial itemTypeWithMaterial : cookingRecipe.getInputLiquidOptions()) {
			initialise(itemTypeWithMaterial, cookingRecipe);
		}

		if (cookingRecipe.getOutputItemTypeName() != null) {
			ItemType itemType = itemTypeDictionary.getByName(cookingRecipe.getOutputItemTypeName());
			if (itemType == null) {
				Logger.error("Could not find item type by name " + cookingRecipe.getOutputItemTypeName() + " as output for recipe " + cookingRecipe.getRecipeName());
			} else {
				cookingRecipe.setOutputItemType(itemType);
			}
		}

		if (cookingRecipe.getOutputMaterialName() != null) {
			GameMaterial material = materialDictionary.getByName(cookingRecipe.getOutputMaterialName());
			if (material == null) {
				Logger.error("Could not find material by name " + cookingRecipe.getOutputMaterialName() + " as output for recipe " + cookingRecipe.getOutputMaterialName());
			} else {
				cookingRecipe.setOutputMaterial(material);
			}
		}


		if (cookingRecipe.getActiveSoundAssetName() != null) {
			cookingRecipe.setActiveSoundAsset(soundAssetDictionary.getByName(cookingRecipe.getActiveSoundAssetName()));
			if (cookingRecipe.getActiveSoundAsset() == null) {
				Logger.error("Could not find sound asset with name " + cookingRecipe.getActiveSoundAssetName() + " for cooking recipe " + cookingRecipe.getRecipeName());
			}
		}
		if (cookingRecipe.getOnCompletionSoundAssetName() != null) {
			cookingRecipe.setOnCompletionSoundAsset(soundAssetDictionary.getByName(cookingRecipe.getOnCompletionSoundAssetName()));
			if (cookingRecipe.getOnCompletionSoundAsset() == null) {
				Logger.error("Could not find sound asset with name " + cookingRecipe.getOnCompletionSoundAssetName() + " for cooking recipe " + cookingRecipe.getRecipeName());
			}
		}
	}

	private void initialise(ItemTypeWithMaterial itemTypeWithMaterial, CookingRecipe cookingRecipe) {
		if (itemTypeWithMaterial.getItemTypeName() != null) {
			ItemType itemType = itemTypeDictionary.getByName(itemTypeWithMaterial.getItemTypeName());
			if (itemType == null) {
				Logger.error("Could not find item type with name " + itemTypeWithMaterial.getItemTypeName() + " required for recipe " + cookingRecipe.getRecipeName());
			} else {
				itemTypeWithMaterial.setItemType(itemType);
			}
		}

		if (itemTypeWithMaterial.getMaterialName() != null) {
			GameMaterial material = materialDictionary.getByName(itemTypeWithMaterial.getMaterialName());
			if (material == null) {
				Logger.error("Could not find material with name " + itemTypeWithMaterial.getMaterialName() + " required for recipe " + cookingRecipe.getRecipeName());
			} else {
				itemTypeWithMaterial.setMaterial(material);
			}
		}
	}
}
