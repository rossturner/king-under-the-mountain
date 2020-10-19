package technology.rocketjump.undermount.rendering.utils;

import com.badlogic.gdx.graphics.Color;
import org.fest.assertions.Delta;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;


public class HexColorsTest {

	@Test
	public void testNonAlphaColor() {
		String hexCode = "#FF00FF";

		Color color = HexColors.get(hexCode);

		assertThat(color.a).isEqualTo(1);
		assertThat(color.r).isEqualTo(1);
		assertThat(color.g).isEqualTo(0);
		assertThat(color.b).isEqualTo(1);

		String reconstituted = HexColors.toHexString(color);

		assertThat(reconstituted).isEqualTo(hexCode);
	}

	@Test
	public void testAlphaColor() {
		String hexCode = "#FF00FF88";

		Color color = HexColors.get(hexCode);

		assertThat(color.a).isEqualTo(0.5333f, Delta.delta(0.001f));
		assertThat(color.r).isEqualTo(1);
		assertThat(color.g).isEqualTo(0);
		assertThat(color.b).isEqualTo(1);

		String reconstituted = HexColors.toHexString(color);

		assertThat(reconstituted).isEqualTo(hexCode);
	}

}