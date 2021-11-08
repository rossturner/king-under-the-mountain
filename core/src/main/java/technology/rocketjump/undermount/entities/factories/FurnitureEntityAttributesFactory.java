package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.rendering.utils.ColorMixer;
import technology.rocketjump.undermount.rendering.utils.HexColors;

import java.util.Random;

public class FurnitureEntityAttributesFactory {

	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final Random random = new RandomXS128();
	private Array<Color> lightColors = new Array<>();

	@Inject
	public FurnitureEntityAttributesFactory(FurnitureTypeDictionary furnitureTypeDictionary) {
		this.furnitureTypeDictionary = furnitureTypeDictionary;

		lightColors.add(Color.WHITE);
		lightColors.add(HexColors.get("#f6f6d1"));
		lightColors.add(HexColors.get("#f8fdff"));
	}

	public FurnitureEntityAttributes byName(String furnitureTypeName, GameMaterial primaryMaterial) {
		FurnitureEntityAttributes attributes = new FurnitureEntityAttributes(random.nextLong());
		attributes.setFurnitureType(furnitureTypeDictionary.getByName(furnitureTypeName));
		attributes.setPrimaryMaterialType(primaryMaterial.getMaterialType());
		attributes.getMaterials().put(primaryMaterial.getMaterialType(), primaryMaterial);
		attributes.setColor(ColoringLayer.ACCESSORY_COLOR, randomLightColor(random));
		return attributes;
	}

	private Color randomLightColor(Random random) {
		return ColorMixer.randomBlend(random, lightColors);
	}

	public FurnitureEntityAttributes byType(FurnitureType furnitureType, GameMaterial primaryMaterial) {
		FurnitureEntityAttributes attributes = new FurnitureEntityAttributes(random.nextLong());
		attributes.setFurnitureType(furnitureType);
		attributes.setPrimaryMaterialType(primaryMaterial.getMaterialType());
		attributes.getMaterials().put(primaryMaterial.getMaterialType(), primaryMaterial);
		attributes.setColor(ColoringLayer.ACCESSORY_COLOR, randomLightColor(random));
		return attributes;
	}

}
