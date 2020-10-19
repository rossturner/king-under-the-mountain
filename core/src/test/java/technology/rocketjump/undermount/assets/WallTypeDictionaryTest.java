package technology.rocketjump.undermount.assets;

import com.badlogic.gdx.files.FileHandle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.jobs.CraftingTypeDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class WallTypeDictionaryTest {

	private WallTypeDictionary wallTypeDictionary;
	@Mock
	private ItemTypeDictionary mockItemTypeDictionary;
	@Mock
	private CraftingTypeDictionary mockCraftingTypeDictionary;

	@Before
	public void setUp() throws Exception {
		wallTypeDictionary = new WallTypeDictionary(new FileHandle(new File("assets/definitions/types/wallTypes.json")),
				mockItemTypeDictionary, mockCraftingTypeDictionary);
	}

	@Test
	public void get_returnsWallForMatchingName() {
		WallType result = wallTypeDictionary.getByWallTypeName("brickwall");

		assertThat(result.getWallTypeName()).isEqualTo("brickwall");
		assertThat(result.getMaterialType()).isEqualTo(GameMaterialType.STONE);

		assertThat(wallTypeDictionary.getByWallTypeId(result.getWallTypeId()).getWallTypeName()).isEqualTo("brickwall");
	}

	@Test
	public void get_returnsNull_whenNoWallForMaterial() {
		assertThat(wallTypeDictionary.getByWallTypeName("grass")).isNull();
	}

}