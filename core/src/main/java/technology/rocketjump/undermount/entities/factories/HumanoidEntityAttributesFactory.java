package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.factories.names.DwarvenNameGenerator;
import technology.rocketjump.undermount.entities.model.physical.humanoid.Gender;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidEntityAttributes;

import java.util.Random;

@Singleton
public class HumanoidEntityAttributesFactory {

	private final HairColorFactory hairColorFactory;
	private final SkinColorFactory skinColorFactory;
	private final AccessoryColorFactory accessoryColorFactory;
	private final DwarvenNameGenerator dwarvenNameGenerator;
	private final Random random = new RandomXS128();

	@Inject
	public HumanoidEntityAttributesFactory(HairColorFactory hairColorFactory, SkinColorFactory skinColorFactory,
										   AccessoryColorFactory accessoryColorFactory, DwarvenNameGenerator dwarvenNameGenerator) {
		this.hairColorFactory = hairColorFactory;
		this.skinColorFactory = skinColorFactory;
		this.accessoryColorFactory = accessoryColorFactory;
		this.dwarvenNameGenerator = dwarvenNameGenerator;
	}

	public HumanoidEntityAttributes create() {
		HumanoidEntityAttributes attributes = new HumanoidEntityAttributes(random.nextLong(),
				hairColorFactory.randomHairColor(random),
				skinColorFactory.randomSkinColor(random),
				accessoryColorFactory.randomAccessoryColor(random));

		attributes.setName(dwarvenNameGenerator.create(attributes.getSeed(), attributes.getGender()));

		if (random.nextFloat() <= chanceToHaveHair(attributes)) {
			attributes.setHasHair(true);
		} else {
			attributes.setHasHair(false);
		}

		return attributes;
	}

	private float chanceToHaveHair(HumanoidEntityAttributes attributes) {
		// MODDING expose these values
		if (attributes.getGender().equals(Gender.MALE)) {
			return 0.3f;
		} else if (attributes.getGender().equals(Gender.FEMALE)) {
			return 1f;
		} else{
			return 0;
		}
	}
}
