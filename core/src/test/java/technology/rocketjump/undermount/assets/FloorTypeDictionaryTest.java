package technology.rocketjump.undermount.assets;

import com.badlogic.gdx.files.FileHandle;
import org.junit.Before;
import org.junit.Test;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class FloorTypeDictionaryTest {

	private FloorTypeDictionary FloorTypeDictionary;

	@Before
	public void setUp() throws Exception {
		FloorTypeDictionary = new FloorTypeDictionary(new FileHandle(new File("assets/definitions/types/floorTypes.json")));
	}

	@Test
	public void get_returnsFloorForMatchingMaterial() {
		FloorType result = FloorTypeDictionary.getByFloorTypeName("grass");

		assertThat(result.getFloorTypeName()).isEqualTo("grass");
		assertThat(result.getMaterialType()).isEqualTo(GameMaterialType.EARTH);
		assertThat(result.getNumSprites()).isEqualTo(5);
		assertThat(result.getLayer()).isEqualTo(10);
	}

	@Test
	public void get_returnsNull_whenNoFloorForMaterial() {
		assertThat(FloorTypeDictionary.getByFloorTypeName("brick")).isNull();
	}

}