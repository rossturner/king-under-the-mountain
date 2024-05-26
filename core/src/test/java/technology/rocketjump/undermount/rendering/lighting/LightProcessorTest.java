package technology.rocketjump.undermount.rendering.lighting;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.TileNeighbours;
import technology.rocketjump.undermount.mapping.tile.layout.TileLayoutAtlas;
import technology.rocketjump.undermount.mapping.tile.wall.Edge;
import technology.rocketjump.undermount.mapping.tile.wall.WallEdgeAtlas;
import technology.rocketjump.undermount.materials.model.GameMaterial;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static technology.rocketjump.undermount.rendering.lighting.PointLight.LIGHT_RADIUS;

@RunWith(MockitoJUnitRunner.class)
public class LightProcessorTest {

	private static final float EPSILON = 0.0001f;
	private LightProcessor lightProcessor;

	@Mock
	private GameMaterial mockWallMaterial;
	@Mock
	private FloorType mockFloorType;
	@Mock
	private WallType mockWallType;
	@Mock
	private PointLightMesh mockMesh;
	@Mock
	private GameMaterial baseFloorMaterial;

	@Before
	public void setup() throws IOException {
		lightProcessor = new LightProcessor(new TileLayoutAtlas(), new WallEdgeAtlas(new File("assets/terrain/wallEdges.json"),
				new File("assets/terrain/doorwayClosedEdges.json"),
				new File("assets/terrain/doorwayEdges.json")));

		when(mockFloorType.isUseMaterialColor()).thenReturn(true);
		when(baseFloorMaterial.getColor()).thenReturn(Color.TEAL);
	}

	@Test
	public void testSimpleCase() throws IOException {
		TiledMap emptySmallWorld = new TiledMap(0L, 2, 2, mockFloorType, baseFloorMaterial);

		PointLight light = new PointLight(mockMesh);
		light.setWorldPosition(new Vector2(1.0f, 1.0f));

		lightProcessor.updateLightGeometry(light, emptySmallWorld);

		assertThat(light.getWorldPosition().x).isEqualTo(1.0f);
		assertThat(light.getWorldPosition().y).isEqualTo(1.0f);

		Array<Edge> lightVertices = light.getLightPolygonEdges();
		assertThat(lightVertices.size).isEqualTo(4);
		assertVectorEpsilonEquals(lightVertices.get(0).getPointA(),  new Vector2(-1f, 1f));
		assertVectorEpsilonEquals(lightVertices.get(0).getPointB(),  new Vector2(1, 1f));

		assertVectorEpsilonEquals(lightVertices.get(1).getPointA(),  new Vector2(1f, 1f));
		assertVectorEpsilonEquals(lightVertices.get(1).getPointB(),  new Vector2(1f, -1f));

		assertVectorEpsilonEquals(lightVertices.get(2).getPointA(),  new Vector2(1f, -1f));
		assertVectorEpsilonEquals(lightVertices.get(2).getPointB(),  new Vector2(-1f, -1f));

		assertVectorEpsilonEquals(lightVertices.get(3).getPointA(),  new Vector2(-1f, -1f));
		assertVectorEpsilonEquals(lightVertices.get(3).getPointB(),  new Vector2(-1f, 1f));

		verify(mockMesh).updateGeometry(light.getWorldPosition(), LIGHT_RADIUS, light.getLightPolygonEdges());
	}

	@Test
	public void updateGeometry_3x3World_lightAtLowerLeft() {
		TiledMap emptySmallWorld = new TiledMap(0L, 3, 3, mockFloorType, baseFloorMaterial);

		PointLight light = new PointLight(mockMesh);
		light.setWorldPosition(new Vector2(1.3f, 0.5f));

		lightProcessor.updateLightGeometry(light, emptySmallWorld);

		assertThat(light.getWorldPosition().x).isEqualTo(1.3f);
		assertThat(light.getWorldPosition().y).isEqualTo(0.5f);

		Array<Edge> lightEdges = light.getLightPolygonEdges();
		assertThat(lightEdges.size).isEqualTo(4);
		assertVectorEpsilonEquals(lightEdges.get(0).getPointA(),  new Vector2(-1.3f, 2.5f));
		assertVectorEpsilonEquals(lightEdges.get(0).getPointB(),  new Vector2(1.7f, 2.5f));

		assertVectorEpsilonEquals(lightEdges.get(1).getPointA(),  new Vector2(1.7f, 2.5f));
		assertVectorEpsilonEquals(lightEdges.get(1).getPointB(),  new Vector2(1.7f, -0.5f));

		assertVectorEpsilonEquals(lightEdges.get(2).getPointA(),  new Vector2(1.7f, -0.5f));
		assertVectorEpsilonEquals(lightEdges.get(2).getPointB(),  new Vector2(-1.3f, -0.5f));

		assertVectorEpsilonEquals(lightEdges.get(3).getPointA(),  new Vector2(-1.3f, -0.5f));
		assertVectorEpsilonEquals(lightEdges.get(3).getPointB(),  new Vector2(-1.3f, 2.5f));
	}

	@Test
	public void updateGeometry_withWallAboveRightOfLightPoint() {
		TiledMap smallWorld = new TiledMap(0L, 3, 3, mockFloorType, baseFloorMaterial);
		smallWorld.getTile(1, 1).addWall(new TileNeighbours(), mockWallMaterial, mockWallType);

		PointLight light = new PointLight(mockMesh);
		light.setWorldPosition(new Vector2(0.5f, 0.5f));

		/**
		 * So this looks like
		 *
		 * ...
		 * .X.
		 * L..
		 *
		 * Where X is wall and L is light
		 */

		lightProcessor.updateLightGeometry(light, smallWorld);

		assertThat(light.getWorldPosition().x).isEqualTo(0.5f);
		assertThat(light.getWorldPosition().y).isEqualTo(0.5f);

		Array<Edge> lightPolygonEdges = light.getLightPolygonEdges();
		assertThat(lightPolygonEdges.size).isEqualTo(8);
	}

	@Test
	public void updateGeometry_withLightUnderWall() {
		TiledMap smallWorld = new TiledMap(0L, 4, 4, mockFloorType, baseFloorMaterial);
		smallWorld.getTile(2, 2).addWall(new TileNeighbours(), mockWallMaterial, mockWallType);

		PointLight light = new PointLight(mockMesh);
		light.setWorldPosition(new Vector2(2.25f, 1.0f));

		lightProcessor.updateLightGeometry(light, smallWorld);

		Array<Edge> lightEdges = light.getLightPolygonEdges();
		assertThat(lightEdges).hasSize(10);

//		assertVectorEpsilonEquals(lightVertices.getTile(10), new Vector2(-2.25f, -1f));
//		assertVectorEpsilonEquals(lightVertices.getTile(11), new Vector2(-2.25f, 3f));
//		assertVectorEpsilonEquals(lightVertices.getTile(12), new Vector2(-0.75f, 3f));
//		assertVectorEpsilonEquals(lightVertices.getTile(13), new Vector2(-0.25f, 1f));
//		assertVectorEpsilonEquals(lightVertices.getTile(14), new Vector2(-0.25f, 2f));

	}

	@Test
	public void updateGeometry_withInitialEdgeOccluded() {
		TiledMap smallWorld = new TiledMap(0L, 4, 4, mockFloorType, baseFloorMaterial);
		smallWorld.getTile(2, 2).addWall(new TileNeighbours(), mockWallMaterial, mockWallType);

		PointLight light = new PointLight(mockMesh);
		light.setWorldPosition(new Vector2(3.5f, 2.5f));

		lightProcessor.updateLightGeometry(light, smallWorld);

		Array<Edge> lightEdges = light.getLightPolygonEdges();
		assertVectorEpsilonEquals(lightEdges.get(0).getPointA(),  new Vector2(-1.5f, 1.5f));
		assertVectorEpsilonEquals(lightEdges.get(0).getPointB(),  new Vector2(0.5f, 1.5f));
	}

	@Test
	public void updateGeometry_initialEdgeNotOccluded_butLastPointIsOccluded() {
		TiledMap smallWorld = new TiledMap(0L, 4, 5, mockFloorType, baseFloorMaterial);
		smallWorld.getTile(2, 2).addWall(new TileNeighbours(), mockWallMaterial, mockWallType);
		smallWorld.getTile(2, 4).addWall(new TileNeighbours(), mockWallMaterial, mockWallType);

		PointLight light = new PointLight(mockMesh);
		light.setWorldPosition(new Vector2(2.5f, 1.5f));

		lightProcessor.updateLightGeometry(light, smallWorld);

		Array<Edge> lightEdges = light.getLightPolygonEdges();
		assertVectorEpsilonEquals(lightEdges.get(0).getPointA(),  new Vector2(-0.1875f, 1.09375f));
		assertVectorEpsilonEquals(lightEdges.get(0).getPointB(),  new Vector2(0.1875f, 1.09375f));
	}

	@Test
	public void updateGeometry_initialEdge_endPointOccludedOnRight() {
		TiledMap smallWorld = new TiledMap(0L, 3, 3, mockFloorType, baseFloorMaterial);
		smallWorld.getTile(2, 2).addWall(new TileNeighbours(), mockWallMaterial, mockWallType);

		PointLight light = new PointLight(mockMesh);
		light.setWorldPosition(new Vector2(1.5f, 2.5f));

		lightProcessor.updateLightGeometry(light, smallWorld);

		Array<Edge> lightEdges = light.getLightPolygonEdges();
		assertVectorEpsilonEquals(lightEdges.get(0).getPointA(),  new Vector2(-1.5f, 0.5f));
		assertVectorEpsilonEquals(lightEdges.get(0).getPointB(),  new Vector2(0.5f, 0.5f));
	}

	@Test
	public void updateGeometry_complexCase1() {
		TileNeighbours emptyNeighbours = new TileNeighbours();
		TiledMap worldMap = new TiledMap(0L, 9, 4, mockFloorType, baseFloorMaterial);
		worldMap.getTile(0, 1).addWall(emptyNeighbours, mockWallMaterial, mockWallType);
		worldMap.getTile(1, 2).addWall(emptyNeighbours, mockWallMaterial, mockWallType);
		worldMap.getTile(4, 2).addWall(emptyNeighbours, mockWallMaterial, mockWallType);
		worldMap.getTile(4, 3).addWall(emptyNeighbours, mockWallMaterial, mockWallType);
		worldMap.getTile(7, 2).addWall(emptyNeighbours, mockWallMaterial, mockWallType);

		for (int x = 0; x < worldMap.getWidth(); x++) {
			for (int y = 0; y < worldMap.getHeight(); y++) {
				worldMap.getTile(x, y).update(worldMap.getNeighbours(x, y), worldMap.getVertices(x, y), null);
			}
		}

		PointLight light = new PointLight(mockMesh);
		light.setWorldPosition(new Vector2(5.5f, 2.5f));

		lightProcessor.updateLightGeometry(light, worldMap);

		Array<Edge> lightEdges = light.getLightPolygonEdges();
		assertVectorEpsilonEquals(lightEdges.get(0).getPointA(),  new Vector2(-0.5f, 1.5f));
		assertVectorEpsilonEquals(lightEdges.get(0).getPointB(),  new Vector2(3.5f, 1.5f));
	}

	@Test
	public void updateGeometry_complexCase2() {
		TileNeighbours emptyNeighbours = new TileNeighbours();
		TiledMap worldMap = new TiledMap(0L, 3, 5, mockFloorType, baseFloorMaterial);
		worldMap.getTile(2, 2).addWall(emptyNeighbours, mockWallMaterial, mockWallType);
		worldMap.getTile(2, 4).addWall(emptyNeighbours, mockWallMaterial, mockWallType);

		for (int x = 0; x < worldMap.getWidth(); x++) {
			for (int y = 0; y < worldMap.getHeight(); y++) {
				worldMap.getTile(x, y).update(worldMap.getNeighbours(x, y), worldMap.getVertices(x, y), null);
			}
		}

		PointLight light = new PointLight(mockMesh);
		light.setWorldPosition(new Vector2(2.75f, 1.25f));

		lightProcessor.updateLightGeometry(light, worldMap);

		Array<Edge> lightEdges = light.getLightPolygonEdges();
		// This test is in place because (before fixing) edge[0].pointA is extended to the left of the top most edge
		// i.e. it is from (-0.75, 1.75) -> (0.25, 1.75) rather than (-0.1, 1.75) -> (0.25, 1.75)
		assertVectorEpsilonEquals(lightEdges.get(0).getPointA(),  new Vector2(-0.06363636f, 1.75f));
		assertVectorEpsilonEquals(lightEdges.get(0).getPointB(),  new Vector2(0.25f, 1.75f));
	}

	public static void assertVectorEpsilonEquals(Vector2 actual, Vector2 expected) {
		assertThat(actual.epsilonEquals(expected, EPSILON))
				.overridingErrorMessage(actual.toString() + " is not epsilon equal to " + expected.toString()).isTrue();
	}

}