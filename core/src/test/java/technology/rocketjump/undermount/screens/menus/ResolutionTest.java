package technology.rocketjump.undermount.screens.menus;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class ResolutionTest {

	@Test
	public void byString() {
		Resolution resolution = Resolution.byString("800x800");

		assertThat(resolution.toString()).isEqualTo("800x800");
	}

	@Test(expected = NumberFormatException.class)
	public void byString_throwsException_withBadInput() {
		Resolution.byString("abc");
	}
}