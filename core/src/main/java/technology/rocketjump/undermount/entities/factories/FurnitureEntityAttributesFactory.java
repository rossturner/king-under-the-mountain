package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.materials.model.GameMaterial;

import java.util.Random;

public class FurnitureEntityAttributesFactory {

	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final Random random = new RandomXS128();
	private final HairColorFactory hairColorFactory;

	@Inject
	public FurnitureEntityAttributesFactory(FurnitureTypeDictionary furnitureTypeDictionary, HairColorFactory hairColorFactory) {
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.hairColorFactory = hairColorFactory;
	}

	public FurnitureEntityAttributes byName(String furnitureTypeName, GameMaterial primaryMaterial) {
		FurnitureEntityAttributes attributes = new FurnitureEntityAttributes(random.nextLong());
		attributes.setFurnitureType(furnitureTypeDictionary.getByName(furnitureTypeName));
		attributes.setPrimaryMaterialType(primaryMaterial.getMaterialType());
		attributes.getMaterials().put(primaryMaterial.getMaterialType(), primaryMaterial);
		attributes.setAccessoryColor(hairColorFactory.randomHairColor(random));
		return attributes;
	}

	public FurnitureEntityAttributes byType(FurnitureType furnitureType, GameMaterial primaryMaterial) {
		FurnitureEntityAttributes attributes = new FurnitureEntityAttributes(random.nextLong());
		attributes.setFurnitureType(furnitureType);
		attributes.setPrimaryMaterialType(primaryMaterial.getMaterialType());
		attributes.getMaterials().put(primaryMaterial.getMaterialType(), primaryMaterial);
		attributes.setAccessoryColor(hairColorFactory.randomHairColor(random));
		return attributes;
	}

}
