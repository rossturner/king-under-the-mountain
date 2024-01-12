package technology.rocketjump.undermount.mapgen.generators;

import com.badlogic.gdx.math.RandomXS128;
import org.junit.Before;
import org.junit.Test;
import technology.rocketjump.undermount.mapgen.model.BinaryGrid;

import static org.fest.assertions.Assertions.assertThat;

public class OreVeinGeneratorTest {

	private OreVeinGenerator oreVeinGenerator;

	@Before
	public void setup() {
		oreVeinGenerator = new OreVeinGenerator();
	}

	@Test
	public void generate() throws Exception {
		BinaryGrid result = oreVeinGenerator.generate(40, 2f, 3f, new RandomXS128());
		System.out.println(result);
		assertThat(result).isNotNull();
	}

}