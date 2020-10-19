package technology.rocketjump.undermount.modding;

import org.fest.assertions.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import technology.rocketjump.undermount.modding.model.ModArtifactDefinition;
import technology.rocketjump.undermount.modding.model.ModArtifactListing;
import technology.rocketjump.undermount.modding.model.ParsedMod;

import java.io.IOException;
import java.nio.file.Paths;

@RunWith(MockitoJUnitRunner.class)
public class ModParserTest {

	private ModParser modParser;

	@Before
	public void setUp() throws Exception {
		modParser = new ModParser(new ModArtifactListing());
	}

	@Test
	public void parseMod() throws IOException {
		ModArtifactDefinition definition = new ModArtifactListing().getByName("definitions/plantColorSwatches/*.png");

		ParsedMod parsedMod = modParser.parseMod(Paths.get("../core/mods/base"));

		Assertions.assertThat(parsedMod.get(definition).sourceFiles).isNotEmpty();
	}
}