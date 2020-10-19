package technology.rocketjump.undermount.guice;

import com.badlogic.gdx.Gdx;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import technology.rocketjump.undermount.assets.FloorTypeDictionary;

import java.io.IOException;


public class FloorDictionaryProvider implements Provider<FloorTypeDictionary> {
	@Override
	public FloorTypeDictionary get() {
		try {
			return new FloorTypeDictionary(Gdx.files.internal("assets/definitions/types/floorTypes.json"));
		} catch (IOException e) {
			throw new ProvisionException("Failed to create " + FloorTypeDictionary.class.getSimpleName(), e);
		}
	}
}
