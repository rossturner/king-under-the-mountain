package technology.rocketjump.undermount.entities.factories;

import org.junit.Before;
import org.junit.Test;
import technology.rocketjump.undermount.entities.factories.names.NorseNameGenerator;
import technology.rocketjump.undermount.entities.model.physical.creature.Gender;
import technology.rocketjump.undermount.entities.model.physical.creature.HumanoidName;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class NorseNameGeneratorTest {

	private NorseNameGenerator norseNameGenerator;

	@Before
	public void setUp() throws Exception {
		norseNameGenerator = new NorseNameGenerator(new File("assets/text/old_norse/given_names.csv"));
	}

	@Test
	public void create() throws Exception {
		HumanoidName name = norseNameGenerator.create(1L, Gender.FEMALE);

		assertThat(name.getFirstName()).isEqualTo("Helka");
		assertThat(name.getLastName()).isEqualTo("Welchdóttir");

		assertThat(name.toString()).isEqualTo("Helka Welchdóttir");
	}

	@Test
	public void test_convertForSurname() {
		// Taken from http://www.vikinganswerlady.com/ONNames.shtml#general_info

		assertThat(norseNameGenerator.convertForSurname("Snorri")).isEqualTo("Snorra");
		assertThat(norseNameGenerator.convertForSurname("Sturla")).isEqualTo("Sturlu");
		assertThat(norseNameGenerator.convertForSurname("Sveinn")).isEqualTo("Sveins");
		assertThat(norseNameGenerator.convertForSurname("Ketill")).isEqualTo("Ketils");
		assertThat(norseNameGenerator.convertForSurname("Geirr")).isEqualTo("Geirs");
		assertThat(norseNameGenerator.convertForSurname("Grímr")).isEqualTo("Gríms");
		assertThat(norseNameGenerator.convertForSurname("Hálfdan")).isEqualTo("Hálfdanar");
		assertThat(norseNameGenerator.convertForSurname("Auðunn")).isEqualTo("Auðunar");
		assertThat(norseNameGenerator.convertForSurname("Sigurðr")).isEqualTo("Sigurðar");
		assertThat(norseNameGenerator.convertForSurname("-biörn")).isEqualTo("-biarnar");
		assertThat(norseNameGenerator.convertForSurname("-örn")).isEqualTo("-arnar");
		assertThat(norseNameGenerator.convertForSurname("-maðr")).isEqualTo("-manns");
		assertThat(norseNameGenerator.convertForSurname("Vigfúss")).isEqualTo("Vigfúss");
	}

}