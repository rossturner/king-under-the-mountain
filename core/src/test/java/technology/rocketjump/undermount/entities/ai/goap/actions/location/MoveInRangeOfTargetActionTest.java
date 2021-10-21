package technology.rocketjump.undermount.entities.ai.goap.actions.location;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import org.junit.Test;
import technology.rocketjump.undermount.misc.VectorUtils;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class MoveInRangeOfTargetActionTest {

	@Test
	public void testSupercoverLine() {
		List<GridPoint2> result = VectorUtils.getGridpointsBetween(
				new Vector2(2.5f, 7.5f),
				new Vector2(17.5f, 2.5f)
		);

		assertThat(result).hasSize(16);

		result = VectorUtils.getGridpointsBetween(
				new Vector2(242.46912f,153.3414f),
				new Vector2(242.50305f,156.50229f)
		);

		assertThat(result).hasSize(4);
	}

}