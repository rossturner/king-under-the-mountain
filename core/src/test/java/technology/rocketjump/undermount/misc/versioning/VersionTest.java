package technology.rocketjump.undermount.misc.versioning;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class VersionTest {


	@Test
	public void toString_givesExpectedResults() {

		assertThat(new Version("Alpha 2").toString()).isEqualTo("Alpha 2");
		assertThat(new Version("ALPHA 2").toString()).isEqualTo("Alpha 2");

		assertThat(new Version("Alpha 2.1").toString()).isEqualTo("Alpha 2.1");
		assertThat(new Version("Alpha 2.0.1").toString()).isEqualTo("Alpha 2.0.1");

		assertThat(new Version("1.2.3").toString()).isEqualTo("1.2.3");
	}

	@Test
	public void componentsSetAsExpected() {

		Version version = new Version("Alpha 2.13");

		assertThat(version.qualifier).isEqualTo(Version.Qualifier.Alpha);
		assertThat(version.major).isEqualTo(2);
		assertThat(version.minor).isEqualTo(13);
		assertThat(version.revision).isEqualTo(0);
	}

	@Test
	public void intComparison() {
		Version alpha21 = new Version("Alpha 2.1");
		Version alpha3 = new Version("Alpha 3");

		assertThat(alpha3.toInteger()).isGreaterThan(alpha21.toInteger());

		assertThat(alpha21.toInteger()).isEqualTo(2001000);
		assertThat(alpha3.toInteger()).isEqualTo(3000000);
	}

}