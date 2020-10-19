package technology.rocketjump.undermount.mapping.tile.wall;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileNeighbours;
import technology.rocketjump.undermount.mapping.tile.layout.WallLayout;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class WallLayoutTest {

    private final static GameMaterial MOCK_MATERIAL = new GameMaterial("mock", 0, GameMaterialType.OTHER);

    @Mock
    private FloorType mockFloorType;
    @Mock
    private WallType mockWallType;
	@Mock
	private GameMaterial floorMaterial;

	@Test
    public void testFromNeighbours() throws Exception {
        TileNeighbours neighbours = new TileNeighbours();
        for (CompassDirection compassDirection : CompassDirection.values()) {
            MapTile terrain = new MapTile(0L, compassDirection.getXOffset(), compassDirection.getYOffset(), mockFloorType, floorMaterial);
            if (compassDirection.equals(CompassDirection.NORTH) || compassDirection.equals(CompassDirection.SOUTH)) {
                terrain.addWall(new TileNeighbours(), MOCK_MATERIAL, mockWallType);
            }
            neighbours.put(compassDirection, terrain);
        }

        WallLayout wallLayout = new WallLayout(neighbours);

        assertThat(wallLayout.getId()).isEqualTo(66);
        assertThat(new WallLayout(neighbours).toString()).isEqualTo(wallLayout.toString());
    }

    @Test
    public void testFromString() {
        assertThat(WallLayout.fromString("....X..X.").toString()).isEqualTo("...\n.X.\n.X.");
        assertThat(WallLayout.fromString("XXX.X..X.").toString()).isEqualTo("XXX\n.X.\n.X.");
        assertThat(WallLayout.fromString("XXXXXXXXX").toString()).isEqualTo("XXX\nXXX\nXXX");
    }

    @Test
    public void testToString() {
        WallLayout northToSouth = new WallLayout(66);
        assertThat(northToSouth.toString()).isEqualTo(".X.\n.X.\n.X.");

        WallLayout northEnd = new WallLayout(64);
        assertThat(northEnd.toString()).isEqualTo("...\n.X.\n.X.");

        assertThat(new WallLayout(255).toString()).isEqualTo("XXX\nXXX\nXXX");
        assertThat(new WallLayout(256).toString()).isEqualTo("...\n.X.\n...");
    }

    @Test
    public void reduceToMeaningfulForm_removesCornerPiecesWithoutTwoNeighbours() {
        assertThat(WallLayout.fromString("X..\n.X.\n.X.").reduceToMeaningfulForm().toString()).isEqualTo("...\n.X.\n.X.");
        assertThat(WallLayout.fromString("..X\n.X.\n.X.").reduceToMeaningfulForm().toString()).isEqualTo("...\n.X.\n.X.");
        assertThat(WallLayout.fromString("...\n.X.\nXX.").reduceToMeaningfulForm().toString()).isEqualTo("...\n.X.\n.X.");
        assertThat(WallLayout.fromString("...\n.X.\nXXX").reduceToMeaningfulForm().toString()).isEqualTo("...\n.X.\n.X.");
    }

}