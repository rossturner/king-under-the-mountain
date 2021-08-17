package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismTypeDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;

import java.util.Random;

public class MechanismEntityAttributesFactory {

	private final MechanismTypeDictionary mechanismTypeDictionary;
	private final Random random = new RandomXS128();

	@Inject
	public MechanismEntityAttributesFactory(MechanismTypeDictionary mechanismTypeDictionary) {
		this.mechanismTypeDictionary = mechanismTypeDictionary;
	}

	public MechanismEntityAttributes byName(String mechanismTypeName, GameMaterial primaryMaterial) {
		MechanismType type = mechanismTypeDictionary.getByName(mechanismTypeName);
		if (type == null) {
			throw new RuntimeException("Unknown mechanism type: " + mechanismTypeName);
		}
		return byType(type, primaryMaterial);
	}

	public MechanismEntityAttributes byType(MechanismType mechanismType, GameMaterial primaryMaterial) {
		MechanismEntityAttributes attributes = new MechanismEntityAttributes(random.nextLong());
		attributes.setMechanismType(mechanismType);
		attributes.setMaterial(primaryMaterial);
		return attributes;
	}

}
