package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectType;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectTypeDictionary;

import java.util.Random;

@Singleton
public class OngoingEffectAttributesFactory {

	private final OngoingEffectTypeDictionary typeDictionary;

	@Inject
	public OngoingEffectAttributesFactory(OngoingEffectTypeDictionary typeDictionary) {
		this.typeDictionary = typeDictionary;
	}

	public OngoingEffectAttributes createByTypeName(String effectTypeName) {
		OngoingEffectType effectType = typeDictionary.getByName(effectTypeName);
		if (effectType == null) {
			Logger.error("Could not find " + OngoingEffectType.class.getSimpleName() +"  by name: " + effectTypeName);
			return null;
		}

		return createByType(effectType, new RandomXS128());
	}

	public OngoingEffectAttributes createByType(OngoingEffectType type, Random random) {
		OngoingEffectAttributes attributes = new OngoingEffectAttributes(random.nextLong(), type);
		return attributes;
	}


}
