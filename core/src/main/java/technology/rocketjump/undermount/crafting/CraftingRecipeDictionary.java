package technology.rocketjump.undermount.crafting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.crafting.model.CraftingRecipe;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.undermount.jobs.CraftingTypeDictionary;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class CraftingRecipeDictionary {

	private final Map<CraftingType, List<CraftingRecipe>> byCraftingType = new HashMap<>();
	private final Map<String, CraftingRecipe> byName = new HashMap<>();
	private final CraftingTypeDictionary craftingTypeDictionary;
	private final ItemTypeDictionary itemTypeDictionary;
	private final GameMaterialDictionary materialDictionary;

	@Inject
	public CraftingRecipeDictionary(CraftingTypeDictionary craftingTypeDictionary, ItemTypeDictionary itemTypeDictionary,
									GameMaterialDictionary materialDictionary) throws IOException {
		this.craftingTypeDictionary = craftingTypeDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.materialDictionary = materialDictionary;
		for (CraftingType craftingType : craftingTypeDictionary.getAll()) {
			byCraftingType.put(craftingType, new ArrayList<>());
		}

		FileHandle craftingRecipesJsonFile = Gdx.files.internal("assets/definitions/crafting/craftingRecipes.json");
		ObjectMapper objectMapper = new ObjectMapper();
		List<CraftingRecipe> craftingRecipes = objectMapper.readValue(craftingRecipesJsonFile.readString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, CraftingRecipe.class));

		for (CraftingRecipe craftingRecipe : craftingRecipes) {
			initCraftingRecipe(craftingRecipe);
			byCraftingType.get(craftingRecipe.getCraftingType()).add(craftingRecipe);
			byName.put(craftingRecipe.getRecipeName(), craftingRecipe);
		}
	}

	private void initCraftingRecipe(CraftingRecipe craftingRecipe) {
		CraftingType relatedCraftingType = craftingTypeDictionary.getByName(craftingRecipe.getCraftingTypeName());
		craftingRecipe.setCraftingType(relatedCraftingType);
		if (craftingRecipe.getItemTypeRequiredName() != null) {
			ItemType itemType = itemTypeDictionary.getByName(craftingRecipe.getItemTypeRequiredName());
			if (itemType != null) {
				craftingRecipe.setItemTypeRequired(itemType);
			}
		}
		for (QuantifiedItemTypeWithMaterial quantifiedItemType : craftingRecipe.getInput()) {
			initialise(craftingRecipe, quantifiedItemType);
		}
		for (QuantifiedItemTypeWithMaterial quantifiedItemType : craftingRecipe.getOutput()) {
			initialise(craftingRecipe, quantifiedItemType);
		}

		if (craftingRecipe.getInput().stream().filter(QuantifiedItemTypeWithMaterial::isLiquid).count() > 1) {
			throw new RuntimeException("Crafting recipe can not have more than 1 input liquid, found in " + craftingRecipe.getRecipeName());
		}
		if (craftingRecipe.getOutput().stream().filter(QuantifiedItemTypeWithMaterial::isLiquid).count() > 1) {
			throw new RuntimeException("Crafting recipe can not have more than 1 output liquid, found in " + craftingRecipe.getRecipeName());
		}
	}

	private void initialise(CraftingRecipe craftingRecipe, QuantifiedItemTypeWithMaterial quantifiedItemType) {
		ItemType itemType = itemTypeDictionary.getByName(quantifiedItemType.getItemTypeName());
		if (itemType != null) {
			quantifiedItemType.setItemType(itemType);
		}

		if (quantifiedItemType.getMaterialName() != null) {
			GameMaterial material = materialDictionary.getByName(quantifiedItemType.getMaterialName());
			if (material == null) {
				Logger.error("Could not find material with name " + quantifiedItemType.getMaterialName() + " required for recipe " + craftingRecipe.getRecipeName());
			} else {
				quantifiedItemType.setMaterial(material);
			}
		}
	}

	public List<CraftingRecipe> getByCraftingType(CraftingType craftingType) {
		return byCraftingType.get(craftingType);
	}

	public CraftingRecipe getByName(String recipeName) {
		return byName.get(recipeName);
	}
}
