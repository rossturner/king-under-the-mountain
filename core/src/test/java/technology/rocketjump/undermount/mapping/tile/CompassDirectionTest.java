package technology.rocketjump.undermount.mapping.tile;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class CompassDirectionTest {

	@Test
	public void testGetIndex() throws Exception {
		assertThat(CompassDirection.NORTH_WEST.getIndex()).isEqualTo(0);
		assertThat(CompassDirection.NORTH.getIndex()).isEqualTo(1);
		assertThat(CompassDirection.NORTH_EAST.getIndex()).isEqualTo(2);
		assertThat(CompassDirection.WEST.getIndex()).isEqualTo(3);
	}
}