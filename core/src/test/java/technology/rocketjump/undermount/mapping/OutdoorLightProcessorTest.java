package technology.rocketjump.undermount.mapping;

import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class OutdoorLightProcessorTest {

	private OutdoorLightProcessor outdoorLightProcessor;

	private final static float SQRT_2 = (float) Math.sqrt(2.0);

	@Before
	public void setup() {
		outdoorLightProcessor = new OutdoorLightProcessor();
	}

}