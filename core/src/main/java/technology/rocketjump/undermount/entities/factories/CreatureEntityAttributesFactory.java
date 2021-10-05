package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.Race;

import java.util.Random;

@Singleton
public class CreatureEntityAttributesFactory {

	private final Random random = new RandomXS128();

	@Inject
	public CreatureEntityAttributesFactory() {

	}

	public CreatureEntityAttributes create(Race race) {
		return new CreatureEntityAttributes(race, random.nextLong());
	}

}
