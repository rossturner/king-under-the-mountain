package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpecies;

public class PlantSeedDispersedMessage {

	private final PlantSpecies plantSpecies;
	private final Vector2 origin;
	private final boolean isFruit;

	public PlantSeedDispersedMessage(PlantSpecies plantSpecies, Vector2 origin, boolean isFruit) {
		this.plantSpecies = plantSpecies;
		this.origin = origin;
		this.isFruit = isFruit;
	}

	public PlantSpecies getPlantSpecies() {
		return plantSpecies;
	}

	public Vector2 getOrigin() {
		return origin;
	}

	public boolean isFruit() {
		return isFruit;
	}
}
