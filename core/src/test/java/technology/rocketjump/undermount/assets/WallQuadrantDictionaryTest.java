package technology.rocketjump.undermount.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.IntArray;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class WallQuadrantDictionaryTest {

	private WallQuadrantDictionary wallQuadrantDictionary;

	@Before
	public void setUp() throws Exception {
		wallQuadrantDictionary = new WallQuadrantDictionary(new FileHandle(new File("assets/terrain/wallLayoutQuadrants.json")));
	}

	@Test
	public void testGetWallQuadrants() throws Exception {
		IntArray wallQuadrants = wallQuadrantDictionary.getWallQuadrants(27);

		assertThat(wallQuadrants.get(0)).isEqualTo(255);
		assertThat(wallQuadrants.get(1)).isEqualTo(90);
		assertThat(wallQuadrants.get(2)).isEqualTo(24);
		assertThat(wallQuadrants.get(3)).isEqualTo(24);
	}

	@Test
	public void testGetUniqueQuadrantIds() {
		Set<Integer> uniqueQuadrantIds = wallQuadrantDictionary.getUniqueQuadrantIds();

		assertThat(uniqueQuadrantIds).hasSize(5);
		assertThat(uniqueQuadrantIds).contains(0, 24, 66, 90, 255);
	}
}