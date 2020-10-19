package technology.rocketjump.undermount.mapping.tile.floor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.assets.model.OverlapType;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileNeighbours;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class OverlapLayoutTest {

	@Mock
	private GameMaterial mockMaterial;
	@Mock
	private OverlapType mockOverlapType;
	@Mock
	private FloorType mockFloorType;

	private FloorType overlappingFloor;

	@Before
	public void setup() {
		overlappingFloor = new FloorType("Higher Layer", null, -1L, GameMaterialType.OTHER, 100,
				1, mockOverlapType, false, null, null);
	}

	@Test
	public void testFromNeighbours() throws Exception {
		TileNeighbours neighbours = new TileNeighbours();
		for (CompassDirection compassDirection : CompassDirection.values()) {
			MapTile terrain = new MapTile(0L, compassDirection.getXOffset(), compassDirection.getYOffset(), mockFloorType, mockMaterial);
			if (compassDirection.equals(CompassDirection.NORTH) || compassDirection.equals(CompassDirection.SOUTH)) {
				terrain.getFloor().setFloorType(overlappingFloor);
				terrain.getFloor().setMaterial(mockMaterial);
			}
			neighbours.put(compassDirection, terrain);
		}

		OverlapLayout overlapLayout = OverlapLayout.fromNeighbours(neighbours, overlappingFloor);

		assertThat(overlapLayout.getId()).isEqualTo(66);
		assertThat(OverlapLayout.fromNeighbours(neighbours, overlappingFloor).toString()).isEqualTo(overlapLayout.toString());
	}

	@Test
	public void testFromString() {
		assertThat(OverlapLayout.fromString(".......X.").toString()).isEqualTo("...\n...\n.X.");
		assertThat(OverlapLayout.fromString("XXX....X.").toString()).isEqualTo("XXX\n...\n.X.");
		assertThat(OverlapLayout.fromString("XXXX.XXXX").toString()).isEqualTo("XXX\nX.X\nXXX");
	}

	@Test
	public void testToString() {
		OverlapLayout northToSouth = new OverlapLayout(66);
		assertThat(northToSouth.toString()).isEqualTo(".X.\n...\n.X.");

		OverlapLayout northEnd = new OverlapLayout(64);
		assertThat(northEnd.toString()).isEqualTo("...\n...\n.X.");

		assertThat(new OverlapLayout(255).toString()).isEqualTo("XXX\nX.X\nXXX");
		assertThat(new OverlapLayout(256).toString()).isEqualTo("...\n...\n...");
	}

	@Test
	public void flipX_pivotsX() {
		assertThat(OverlapLayout.fromString("..X\nX..\nX..").flipX().toString()).isEqualTo("X..\n..X\n..X");

		assertThat(OverlapLayout.fromString("X..\nX..\nX..").flipX().toString()).isEqualTo("..X\n..X\n..X");
		assertThat(OverlapLayout.fromString("XXX\n...\n...").flipX().toString()).isEqualTo("XXX\n...\n...");
		assertThat(OverlapLayout.fromString("..X\n..X\n..X").flipX().toString()).isEqualTo("X..\nX..\nX..");
		assertThat(OverlapLayout.fromString("...\n...\nXXX").flipX().toString()).isEqualTo("...\n...\nXXX");
	}

	@Test
	public void flipY_pivotsY() {
		assertThat(OverlapLayout.fromString("..X\nX..\nX..").flipY().toString()).isEqualTo("X..\nX..\n..X");

		assertThat(OverlapLayout.fromString("X..\nX..\nX..").flipY().toString()).isEqualTo("X..\nX..\nX..");
		assertThat(OverlapLayout.fromString("XXX\n...\n...").flipY().toString()).isEqualTo("...\n...\nXXX");
		assertThat(OverlapLayout.fromString("..X\n..X\n..X").flipY().toString()).isEqualTo("..X\n..X\n..X");
		assertThat(OverlapLayout.fromString("...\n...\nXXX").flipY().toString()).isEqualTo("XXX\n...\n...");
	}

	@Test
	public void reduceToMeaningfulForm_removesCornerPiecesWithoutTwoNeighbours() {
		assertThat(OverlapLayout.fromString("X..\nX..\n.X.").reduceToMeaningfulForm().toString()).isEqualTo("...\nX..\n.X.");
		assertThat(OverlapLayout.fromString("..X\n...\n.XX").reduceToMeaningfulForm().toString()).isEqualTo("..X\n...\n.X.");
		assertThat(OverlapLayout.fromString("...\n...\nXX.").reduceToMeaningfulForm().toString()).isEqualTo("...\n...\n.X.");
		assertThat(OverlapLayout.fromString("...\n...\nXXX").reduceToMeaningfulForm().toString()).isEqualTo("...\n...\n.X.");
	}

}