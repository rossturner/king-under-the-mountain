package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;

import java.util.Random;

public class PlantEntityAttributesFactory {

	private final PlantSpeciesDictionary speciesDictionary;

	@Inject
	public PlantEntityAttributesFactory(PlantSpeciesDictionary speciesDictionary) {
		this.speciesDictionary = speciesDictionary;
	}

	public PlantEntityAttributes createBySpeciesName(String speciesName) {
		PlantSpecies species = speciesDictionary.getByName(speciesName);
		if (species == null) {
			Logger.error("Could not find " + PlantSpecies.class.getSimpleName() +"  by name: " + speciesName);
			return null;
		}

		return createBySpecies(species, new RandomXS128());
	}

	public PlantEntityAttributes createBySpecies(PlantSpecies plantSpecies, Random random) {
		PlantEntityAttributes attributes = new PlantEntityAttributes(random.nextLong(), plantSpecies);
		attributes.setGrowthRate(calculateGrowthRate(random, plantSpecies.getMaxGrowthSpeedVariance()));
		return attributes;
	}

	public PlantEntityAttributes createBySeedMaterial(GameMaterial seedMaterial, Random random) {
		PlantSpecies species = speciesDictionary.getBySeedMaterial(seedMaterial);
		if (species == null) {
			Logger.error("Could not spawn plant by seed material: " + seedMaterial.getMaterialName());
			return null;
		} else {
			return createBySpecies(species, random);
		}
	}

	private float calculateGrowthRate(Random random, float maxGrowthSpeedVariance) {
		return 1f - maxGrowthSpeedVariance + (random.nextFloat() * maxGrowthSpeedVariance * 2f);
	}
}
