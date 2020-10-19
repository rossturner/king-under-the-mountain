package technology.rocketjump.undermount.modding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.undermount.modding.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Singleton
public class ModParser {

	private final ModArtifactListing artifactListing;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Inject
	public ModParser(ModArtifactListing modArtifactListing) {
		this.artifactListing = modArtifactListing;
	}

	public ParsedMod parseMod(Path modBasePath) throws IOException {
		ParsedMod parsedMod = new ParsedMod(modBasePath, readModInfo(modBasePath));

		for (ModArtifactDefinition artifactDefinition : artifactListing.getAll()) {
			Optional<ModArtifact> artifact = new ArtifactParser(artifactDefinition).parse(modBasePath);
			artifact.ifPresent(parsedMod::add);
		}

		return parsedMod;
	}

	private ModInfo readModInfo(Path modBasePath) throws IOException {
		Path infoPath = modBasePath.resolve("modInfo.json");
		if (!Files.exists(infoPath)) {
			throw new IOException("Could not find modInfo.json in " + modBasePath.toString());
		}
		return objectMapper.readValue(FileUtils.readFileToString(infoPath.toFile(), "UTF-8"), ModInfo.class);
	}

	public ModArtifactListing getArtifactListing() {
		return artifactListing;
	}
}
