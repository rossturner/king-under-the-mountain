package technology.rocketjump.undermount.entities.factories;

import org.fest.assertions.Assertions;
import org.junit.Before;
import org.junit.Test;
import technology.rocketjump.undermount.entities.factories.names.NorseNameGenerator;

import java.io.IOException;


public class SettlementNameGeneratorTest {

	private SettlementNameGenerator settlementNameGenerator;

	@Before
	public void setup() throws IOException {
		settlementNameGenerator = new SettlementNameGenerator("../core/assets/text/settlement/descriptor.json", new NorseNameGenerator());
	}

	@Test
	public void simpleTest() {
		Assertions.assertThat(settlementNameGenerator.create(1L)).isEqualTo("Wildstream");

//		Random random = new RandomXS128();
//		for (int i = 0; i < 100; i++) {
//			System.out.println(settlementNameGenerator.create(random.nextLong()));
//		}

	}

}