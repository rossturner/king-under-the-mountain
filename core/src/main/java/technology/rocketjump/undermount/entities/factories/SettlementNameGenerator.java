package technology.rocketjump.undermount.entities.factories;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.factories.names.NameGenerator;
import technology.rocketjump.undermount.entities.factories.names.NorseNameGenerator;
import technology.rocketjump.undermount.entities.model.physical.creature.Gender;

import java.io.File;
import java.io.IOException;

@Singleton
public class SettlementNameGenerator extends NameGenerator {

	@Inject
	public SettlementNameGenerator(NorseNameGenerator norseNameGenerator) throws IOException {
		this("assets/text/settlement/descriptor.json", norseNameGenerator);
	}

	public SettlementNameGenerator(String filename, NorseNameGenerator norseNameGenerator) throws IOException {
		super(new File(filename), norseNameGenerator);
	}

	public String create(long seed) {
		return super.create(seed, Gender.ANY).getFirstName();
	}


}
