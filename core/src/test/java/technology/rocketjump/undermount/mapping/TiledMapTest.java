package technology.rocketjump.undermount.mapping;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileNeighbours;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;
import technology.rocketjump.undermount.materials.model.GameMaterial;

import java.util.Random;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class TiledMapTest {

    @Mock
    private FloorType mockFloorType;
    @Mock
    private GameMaterial mockFloorMaterial;

    @Test
    public void testXYBounds() throws Exception {
        TiledMap map = new TiledMap(new Random().nextLong(), 10, 5, mockFloorType, mockFloorMaterial);

        map.getTile(0, 1).getRoof().setState(TileRoofState.OPEN);

        assertThat(map.getTile(0, 1).getRoof().getState()).isEqualTo(TileRoofState.OPEN);
        assertThat(map.getTile(9, 4).getRoof().getState()).isEqualTo(TileRoofState.MOUNTAIN_ROOF);
        assertThat(map.getTile(10, 4)).isNull();
        assertThat(map.getTile(9, 5)).isNull();

        TileNeighbours neighbours = map.getNeighbours(0, 0);
        assertThat(neighbours.get(CompassDirection.NORTH).getRoof().getState()).isEqualTo(TileRoofState.OPEN);
    }

    @Test
    public void testXYBounds_withMapVertices() {
        TiledMap map = new TiledMap(1L, 3, 3, mockFloorType, mockFloorMaterial);

        MapTile cell00 = new MapTile(0L, 0, 0, mockFloorType, mockFloorMaterial);
        assertThat(map.getVertex(cell00, CompassDirection.SOUTH_WEST).getVertexX()).isEqualTo(0);
        assertThat(map.getVertex(cell00, CompassDirection.SOUTH_WEST).getVertexY()).isEqualTo(0);

        MapTile cell02 = new MapTile(0L, 0, 2, mockFloorType, mockFloorMaterial);
        assertThat(map.getVertex(cell02, CompassDirection.NORTH_WEST).getVertexX()).isEqualTo(0);
        assertThat(map.getVertex(cell02, CompassDirection.NORTH_WEST).getVertexY()).isEqualTo(3);

        MapTile cell22 = new MapTile(0L, 2, 2, mockFloorType, mockFloorMaterial);
        assertThat(map.getVertex(cell22, CompassDirection.NORTH_EAST).getVertexX()).isEqualTo(3);
        assertThat(map.getVertex(cell22, CompassDirection.NORTH_EAST).getVertexY()).isEqualTo(3);

        MapTile cell20 = new MapTile(0L, 2, 0, mockFloorType, mockFloorMaterial);
        assertThat(map.getVertex(cell20, CompassDirection.SOUTH_EAST).getVertexX()).isEqualTo(3);
        assertThat(map.getVertex(cell20, CompassDirection.SOUTH_EAST).getVertexY()).isEqualTo(0);
    }

    @Test
    public void testGetNearestTiles_lowerLeftCase() {
        TiledMap map = new TiledMap(1L, 3, 3, mockFloorType, mockFloorMaterial);

        Array<MapTile> nearestTiles = map.getNearestTiles(new Vector2(1.1f, 1.1f));

        assertThat(nearestTiles).hasSize(4);
        assertThat(nearestTiles.get(0)).isEqualTo(map.getTile(1, 1));
        assertThat(nearestTiles.get(1)).isEqualTo(map.getTile(0, 1));
        assertThat(nearestTiles.get(2)).isEqualTo(map.getTile(1, 0));
        assertThat(nearestTiles.get(3)).isEqualTo(map.getTile(0, 0));
    }

    @Test
    public void testGetNearestTiles_upperLeftCase() {
        TiledMap map = new TiledMap(1L, 3, 3, mockFloorType, mockFloorMaterial);

        Array<MapTile> nearestTiles = map.getNearestTiles(new Vector2(1.1f, 1.8f));

        assertThat(nearestTiles).hasSize(4);
        assertThat(nearestTiles.get(0)).isEqualTo(map.getTile(1, 1));
        assertThat(nearestTiles.get(1)).isEqualTo(map.getTile(0, 1));
        assertThat(nearestTiles.get(2)).isEqualTo(map.getTile(1, 2));
        assertThat(nearestTiles.get(3)).isEqualTo(map.getTile(0, 2));
    }

    @Test
    public void testGetNearestTiles_lowerRightCase() {
        TiledMap map = new TiledMap(1L, 6, 6, mockFloorType, mockFloorMaterial);

        Array<MapTile> nearestTiles = map.getNearestTiles(new Vector2(3.7f, 2.2f));

        assertThat(nearestTiles).hasSize(4);
        assertThat(nearestTiles.get(0).getWorldPositionOfCenter()).isEqualTo(new Vector2(3.5f, 2.5f));
        assertThat(nearestTiles.get(1).getWorldPositionOfCenter()).isEqualTo(new Vector2(4.5f, 2.5f));
        assertThat(nearestTiles.get(2).getWorldPositionOfCenter()).isEqualTo(new Vector2(3.5f, 1.5f));
        assertThat(nearestTiles.get(3).getWorldPositionOfCenter()).isEqualTo(new Vector2(4.5f, 1.5f));
    }

    @Test
    public void testGetNearestTiles_upperRightCase() {
        TiledMap map = new TiledMap(1L, 3, 3, mockFloorType, mockFloorMaterial);

        Array<MapTile> nearestTiles = map.getNearestTiles(new Vector2(1.8f, 1.8f));

        assertThat(nearestTiles).hasSize(4);
        assertThat(nearestTiles.get(0)).isEqualTo(map.getTile(1, 1));
        assertThat(nearestTiles.get(1)).isEqualTo(map.getTile(2, 1));
        assertThat(nearestTiles.get(2)).isEqualTo(map.getTile(1, 2));
        assertThat(nearestTiles.get(3)).isEqualTo(map.getTile(2, 2));
    }
}