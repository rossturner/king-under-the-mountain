package technology.rocketjump.undermount.modding;

import org.junit.Before;
import org.junit.Test;
import technology.rocketjump.undermount.modding.model.ModArtifact;
import technology.rocketjump.undermount.modding.model.ModArtifactListing;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

import static org.fest.assertions.Assertions.assertThat;

public class ArtifactParserTest {

	private ArtifactParser artifactParser;

	@Before
	public void setup() {
		artifactParser = new ArtifactParser(new ModArtifactListing().getAll().get(0));
	}

	@Test
	public void parse_doesSomething() throws IOException {
		Optional<ModArtifact> parsed = artifactParser.parse(Paths.get("../core/mods/base"));

		assertThat(parsed.isPresent()).isTrue();
		assertThat(parsed.get().sourceFiles).hasSize(1);
		assertThat(parsed.get().checksum()).isNotEqualTo(0L);
	}

}