package technology.rocketjump.undermount.mapping.tile.wall;

import com.badlogic.gdx.math.Vector2;
import org.junit.Test;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;

import static org.fest.assertions.Assertions.assertThat;

public class EdgeTest {

	@Test
	public void testGetWallDirection() throws Exception {
		Edge westToEastWall = new Edge(new Vector2(0, 0), new Vector2(1, 0));
		assertThat(westToEastWall.getDirection()).isEqualTo(CompassDirection.EAST);

		Edge eastToWestWall = new Edge(new Vector2(1, 0), new Vector2(0, 0));
		assertThat(eastToWestWall.getDirection()).isEqualTo(CompassDirection.WEST);

		Edge northToSouthWall = new Edge(new Vector2(0, 1), new Vector2(0, 0));
		assertThat(northToSouthWall.getDirection()).isEqualTo(CompassDirection.SOUTH);

		Edge southToNorthWall = new Edge(new Vector2(0, 0), new Vector2(0, 1));
		assertThat(southToNorthWall.getDirection()).isEqualTo(CompassDirection.NORTH);
	}
}