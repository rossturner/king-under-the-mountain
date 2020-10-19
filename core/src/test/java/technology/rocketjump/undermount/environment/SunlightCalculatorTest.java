package technology.rocketjump.undermount.environment;

import com.badlogic.gdx.graphics.Color;
import org.junit.Before;
import org.junit.Test;
import technology.rocketjump.undermount.environment.model.SunlightPhase;

import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class SunlightCalculatorTest {

	private SunlightCalculator sunlightCalculator;

	public static final Color MOONLIGHT 	= new Color(50f/255f, 	60f/255f, 	100f/255f, 	1.0f);
	public static final Color DAWN 			= new Color(90f/255f, 	110f/255f, 	140f/255f, 	1.0f);
	public static final Color SUNRISE 		= new Color(230f/255f, 	220f/255f, 	170f/255f, 	1.0f);
	public static final Color FULL_SUNLIGHT	= new Color(255f/255f, 	255f/255f, 	255f/255f, 	1.0f);
	public static final Color SUNSET		= new Color(240f/255f, 	210f/255f, 	170f/255f, 	1.0f);

	private final List<SunlightPhase> sunlightPhases = Arrays.asList(
			new SunlightPhase(0.0f, MOONLIGHT),
			new SunlightPhase(3.0f, MOONLIGHT),
			new SunlightPhase(4.5f, DAWN),
			new SunlightPhase(5.5f, SUNRISE),
			new SunlightPhase(8.0f, FULL_SUNLIGHT),
			new SunlightPhase(17.0f, FULL_SUNLIGHT),
			new SunlightPhase(20.0f, SUNSET),
			new SunlightPhase(21.0f, DAWN),
			new SunlightPhase(23.0f, MOONLIGHT),
			new SunlightPhase(24.0f, MOONLIGHT)
	);

	@Before
	public void setUp() throws Exception {
		this.sunlightCalculator = new SunlightCalculator(sunlightPhases);
	}

	@Test
	public void getMixedSunlightColor() throws Exception {
		Color betweenMoonlightAndDawn = sunlightCalculator.getSunlightColor(4.0f);

		assertThat(betweenMoonlightAndDawn.r).isGreaterThan(MOONLIGHT.r);
		assertThat(betweenMoonlightAndDawn.r).isLessThan(DAWN.r);

		assertThat(betweenMoonlightAndDawn.g).isGreaterThan(MOONLIGHT.g);
		assertThat(betweenMoonlightAndDawn.g).isLessThan(DAWN.g);

		assertThat(betweenMoonlightAndDawn.b).isGreaterThan(MOONLIGHT.b);
		assertThat(betweenMoonlightAndDawn.b).isLessThan(DAWN.b);

		Color slightlyLater = sunlightCalculator.getSunlightColor(4.1f);
		assertThat(slightlyLater.r).isGreaterThan(betweenMoonlightAndDawn.r);
		assertThat(slightlyLater.g).isGreaterThan(betweenMoonlightAndDawn.g);
		assertThat(slightlyLater.b).isGreaterThan(betweenMoonlightAndDawn.b);
	}

}