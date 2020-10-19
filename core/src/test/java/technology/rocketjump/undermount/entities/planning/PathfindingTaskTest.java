package technology.rocketjump.undermount.entities.planning;

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.math.Vector2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.TileNeighbours;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.types.PathfindingRequestMessage;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class PathfindingTaskTest implements PathfindingCallback {

	@Mock
	private FloorType mockFloorType;
	@Mock
	private GameMaterial mockMaterial;
	private GraphPath<Vector2> resultPath;
	@Mock
	private WallType mockWallType;
	@Mock
	private GameMaterial baseFloorMaterial;

	@Test
	public void testSimplestCase() throws Exception {
		TiledMap map = new TiledMap(1L, 3, 3, mockFloorType, baseFloorMaterial);

		Vector2 origin = new Vector2(0.5f, 1.5f);
		Vector2 destination = new Vector2(2.5f, 1.5f);

		PathfindingRequestMessage requestMessage = new PathfindingRequestMessage(origin, destination, map, this, 0L);
		PathfindingTask pathfindingTask = new PathfindingTask(requestMessage);
		pathfindingTask.call();

		assertThat(resultPath.getCount()).isEqualTo(2);
		assertThat(resultPath.get(0)).isEqualTo(new Vector2(1.5f, 1.5f));
		assertThat(resultPath.get(1)).isEqualTo(destination);
	}

	@Test
	public void testSimpleWallAvoidance() throws Exception {
		TiledMap map = new TiledMap(1L, 5, 5, mockFloorType, baseFloorMaterial);
		map.getTile(2, 1).addWall(new TileNeighbours(), mockMaterial, mockWallType);
		map.getTile(2, 2).addWall(new TileNeighbours(), mockMaterial, mockWallType);
		map.getTile(2, 3).addWall(new TileNeighbours(), mockMaterial, mockWallType);
		map.getTile(2, 4).addWall(new TileNeighbours(), mockMaterial, mockWallType);

		/*
		Producing map looking like

			..X..
			..X..
			o.X.d
			..X..
			.....

			Where X is wall, o and d are origin and destination
		 */

		Vector2 origin = new Vector2(0.5f, 2.5f);
		Vector2 destination = new Vector2(4.5f, 2.5f);

		PathfindingRequestMessage requestMessage = new PathfindingRequestMessage(origin, destination, map, this, 0L);
		PathfindingTask pathfindingTask = new PathfindingTask(requestMessage);
		pathfindingTask.call();

		assertThat(resultPath).hasSize(6);
		assertThat(resultPath.get(0)).isEqualTo(new Vector2(1.5f, 1.5f));
		assertThat(resultPath.get(1)).isEqualTo(new Vector2(1.5f, 0.5f));
		assertThat(resultPath.get(2)).isEqualTo(new Vector2(2.5f, 0.5f));
		assertThat(resultPath.get(3)).isEqualTo(new Vector2(3.5f, 0.5f));
		assertThat(resultPath.get(4)).isEqualTo(new Vector2(3.5f, 1.5f));
		assertThat(resultPath.get(5)).isEqualTo(destination);
	}

	@Test
	public void testLargeUnnavigableCase() throws Exception {
		TiledMap map = new TiledMap(1L, 1000, 1000, mockFloorType, baseFloorMaterial);
		map.getTile(998, 998).addWall(new TileNeighbours(), mockMaterial, mockWallType);
		map.getTile(999, 998).addWall(new TileNeighbours(), mockMaterial, mockWallType);
		map.getTile(998, 999).addWall(new TileNeighbours(), mockMaterial, mockWallType);
		map.getTile(999, 999).setRegionId(2);

		Vector2 origin = new Vector2(0.5f, 0.5f);
		Vector2 destination = new Vector2(999.5f, 999.5f);
		// Destination is top-right corner blocked off by walls

		long startTime = System.currentTimeMillis();
		PathfindingRequestMessage requestMessage = new PathfindingRequestMessage(origin, destination, map, this, 0L);
		PathfindingTask pathfindingTask = new PathfindingTask(requestMessage);
		pathfindingTask.call();
		long endTime = System.currentTimeMillis();

		assertThat(endTime - startTime).isLessThan(3000L); // Can't take too long to realise not-navigable
		assertThat(resultPath).isEmpty();
	}

	@Test
	public void testBlockedOffArea() throws Exception {
		TiledMap map = new TiledMap(1L, 5, 5, mockFloorType, baseFloorMaterial);
		map.getTile(3, 3).addWall(new TileNeighbours(), mockMaterial, mockWallType);
		map.getTile(4, 3).addWall(new TileNeighbours(), mockMaterial, mockWallType);
		map.getTile(3, 4).addWall(new TileNeighbours(), mockMaterial, mockWallType);

		Vector2 origin = new Vector2(0.5f, 0.5f);
		Vector2 destination = new Vector2(4.5f, 4.5f);
		// Destination is top-right corner blocked off by walls

		long startTime = System.currentTimeMillis();
		PathfindingRequestMessage requestMessage = new PathfindingRequestMessage(origin, destination, map, this, 0L);
		PathfindingTask pathfindingTask = new PathfindingTask(requestMessage);
		pathfindingTask.call();
		long endTime = System.currentTimeMillis();

		assertThat(endTime - startTime).isLessThan(3000L); // Can't take too long to realise not-navigable
		assertThat(resultPath).isEmpty();
	}

	@Test
	public void testSingleDiagonalPath() throws Exception {
		TiledMap map = new TiledMap(1L, 5, 5, mockFloorType, baseFloorMaterial);

		Vector2 origin = new Vector2(1.5f, 1.5f);
		Vector2 destination = new Vector2(2.5f, 2.5f);

		PathfindingRequestMessage requestMessage = new PathfindingRequestMessage(origin, destination, map, this, 0L);
		PathfindingTask pathfindingTask = new PathfindingTask(requestMessage);
		pathfindingTask.call();

		assertThat(resultPath).hasSize(1);
		assertThat(resultPath.get(0)).isEqualTo(new Vector2(2.5f, 2.5f));
	}

	@Override
	public void pathfindingComplete(GraphPath<Vector2> path, long relatedId) {
		this.resultPath = path;
	}
}