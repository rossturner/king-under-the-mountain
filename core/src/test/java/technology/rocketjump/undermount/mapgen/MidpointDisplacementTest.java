package technology.rocketjump.undermount.mapgen;

import org.fest.assertions.Assertions;
import org.junit.Test;
import technology.rocketjump.undermount.mapgen.generators.MidpointDisplacement;
import technology.rocketjump.undermount.mapgen.model.VertexGameMap;

public class MidpointDisplacementTest {

	@Test
	public void doubleSize_returnsMapWithAveragedMidpoints() throws Exception {
		VertexGameMap input = new VertexGameMap(1, 1);

		input.set(0, 0, 0f);
		input.set(0, 1, 0.4f);
		input.set(1, 0, 0.6f);
		input.set(1, 1, 1f);

		MidpointDisplacement midpointDisplacement = new MidpointDisplacement();
		VertexGameMap output = midpointDisplacement.doubleSize(input);

		Assertions.assertThat(output.get(0, 0).getHeight()).isEqualTo(0f);
		Assertions.assertThat(output.get(1, 1).getHeight()).isEqualTo(0.5f); // Middle point
		Assertions.assertThat(output.get(2, 2).getHeight()).isEqualTo(1f);
	}

}