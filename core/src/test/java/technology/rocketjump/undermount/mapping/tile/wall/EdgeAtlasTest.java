package technology.rocketjump.undermount.mapping.tile.wall;

import com.badlogic.gdx.math.Vector2;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class EdgeAtlasTest {

	@Test
	public void wallEdgeAtlas_shouldContainExpectedEntries() throws IOException {
		WallEdgeAtlas wallEdgeAtlas = new WallEdgeAtlas(new File("assets/terrain/wallEdges.json"),
				new File("assets/terrain/doorwayClosedEdges.json"),
				new File("assets/terrain/doorwayEdges.json"));

		WallEdgeDefinition originalDefinitions = wallEdgeAtlas.getForLayoutId(126);
		assertThat(originalDefinitions.getInnerEdges()).hasSize(4);
		assertThat(originalDefinitions.getInnerEdges().get(0).getPointA()).isEqualTo(new Vector2(0, WallEdgeDefinition.INNER_EDGE_Y2));
		assertThat(originalDefinitions.getOuterEdges()).hasSize(0);

		WallEdgeDefinition flippedFromAtlas = wallEdgeAtlas.getForLayoutId(219);
		assertThat(flippedFromAtlas.getInnerEdges()).hasSize(4);
		// Note that this is not the above x-flipped due to the winding of edges for inference of which direction they face
		assertThat(flippedFromAtlas.getInnerEdges().get(0).getPointA()).isEqualTo(new Vector2(WallEdgeDefinition.INNER_EDGE_X1, 0));
	}


}