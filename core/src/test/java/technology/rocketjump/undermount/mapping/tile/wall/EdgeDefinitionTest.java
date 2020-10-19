package technology.rocketjump.undermount.mapping.tile.wall;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static technology.rocketjump.undermount.mapping.tile.wall.WallEdgeDefinition.*;

public class EdgeDefinitionTest {

	@Test
	public void testFlipX() throws Exception {
		Array<Edge> originalInner = new Array<>();
		originalInner.add(new Edge(new Vector2(0, 0), new Vector2(0, 1)));

		Array<Edge> originalOuter = new Array<>();
		originalOuter.add(new Edge(new Vector2(INNER_EDGE_X1, INNER_EDGE_Y1), new Vector2(INNER_EDGE_X2, INNER_EDGE_Y1)));

		WallEdgeDefinition baseDefinitions = new WallEdgeDefinition(originalInner, originalOuter);

		WallEdgeDefinition flippedDefinitions = baseDefinitions.flipX();

		assertThat(flippedDefinitions.getInnerEdges().get(0).getPointA()).isEqualTo(new Vector2(1, 0));
		assertThat(flippedDefinitions.getInnerEdges().get(0).getPointB()).isEqualTo(new Vector2(1, 1));

		assertThat(flippedDefinitions.getOuterEdges().get(0).getPointA()).isEqualTo(new Vector2(INNER_EDGE_X2, INNER_EDGE_Y1));
		assertThat(flippedDefinitions.getOuterEdges().get(0).getPointB()).isEqualTo(new Vector2(INNER_EDGE_X1, INNER_EDGE_Y1));
	}

	@Test
	public void testGetEdgesForVisibilityPolygon() throws Exception {

	}
}