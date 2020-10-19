package technology.rocketjump.undermount.assets.entities.model;

import com.badlogic.gdx.math.Vector2;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation.*;

public class EntityAssetOrientationTest {

	@Test
	public void fromFacing_returnsCorrectOrientation() {
		Vector2 NORTH = new Vector2(0f, 1f).nor();
		assertThat(fromFacing(NORTH)).isEqualTo(UP);

		Vector2 NORTH_WEST = new Vector2(-1f, 1f).nor();
		assertThat(fromFacing(NORTH_WEST)).isEqualTo(UP_LEFT);

		Vector2 WEST = new Vector2(-1f, 0f).nor();
		assertThat(fromFacing(WEST)).isEqualTo(DOWN_LEFT);

		Vector2 SOUTH_WEST = new Vector2(-1f, -1f).nor();
		assertThat(fromFacing(SOUTH_WEST)).isEqualTo(DOWN_LEFT);

		Vector2 SOUTH = new Vector2(0f, -1f).nor();
		assertThat(fromFacing(SOUTH)).isEqualTo(DOWN);

		Vector2 SOUTH_EAST = new Vector2(1f, -1f).nor();
		assertThat(fromFacing(SOUTH_EAST)).isEqualTo(DOWN_RIGHT);

		Vector2 EAST = new Vector2(1f, -1f).nor();
		assertThat(fromFacing(EAST)).isEqualTo(DOWN_RIGHT);

		Vector2 NORTH_EAST = new Vector2(1f, 1f).nor();
		assertThat(fromFacing(NORTH_EAST)).isEqualTo(UP_RIGHT);
	}

}