package technology.rocketjump.undermount.modding;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import technology.rocketjump.undermount.modding.model.ModArtifact;
import technology.rocketjump.undermount.modding.model.ModArtifactDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ArtifactParser {

	private final ModArtifactDefinition artifactDefinition;

	public ArtifactParser(ModArtifactDefinition artifactDefinition) {
		this.artifactDefinition = artifactDefinition;
	}

	public Optional<ModArtifact> parse(Path modBasePath) throws IOException {
		Path inputBasePath = modBasePath.resolve(artifactDefinition.modDir);
		if (!Files.exists(inputBasePath) || !Files.isDirectory(inputBasePath)) {
			return Optional.empty();
		}


		ModArtifact artifact = new ModArtifact(artifactDefinition);

		String filenameMatcher = artifactDefinition.inputFileNameMatcher;
		boolean recursive = false;
		if (filenameMatcher.startsWith("**/")) {
			recursive = true;
			filenameMatcher = filenameMatcher.substring("**/".length());
		}
		if (!filenameMatcher.contains(".")) {
			filenameMatcher += artifactDefinition.inputFileType.fileExtension;
		}

		parseDir(inputBasePath, artifact, filenameMatcher, recursive);

		if (artifact.sourceFiles.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(artifact);
		}
	}

	private void parseDir(Path directory, ModArtifact artifact, String filenameMatcher, boolean recursive) throws IOException {
		Files.list(directory).forEach(file -> {
			try {
				if (Files.isDirectory(file)) {
					if (recursive) {
						parseDir(file, artifact, filenameMatcher, recursive);
					}
				} else {
					if (matches(file, filenameMatcher)) {
						artifact.sourceFiles.add(file);
					}
				}
			} catch (IOException e) {
				System.err.println("Error while parsing " + directory.toString() + ", " + e.getMessage());
			}
		});
	}

	private boolean matches(Path file, String filenameMatcher) {
		String filename = file.getFileName().toString();
		if (filenameMatcher.contains("!")) {
			Pair<String, String> matcherParts = parseNotMatcher(filenameMatcher);
			return matches(file, matcherParts.getLeft()) && !filename.contains(matcherParts.getRight());
		} else if (filenameMatcher.contains("*")) {
			return filename.matches(filenameMatcher.replace(".", "\\.").replace("*", ".*"));
		} else {
			return filenameMatcher.equalsIgnoreCase(filename);
		}
	}

	private Pair<String, String> parseNotMatcher(String filenameMatcher) {
		StringBuilder matcherBuilder = new StringBuilder();
		StringBuilder notMatcher = new StringBuilder();
		boolean openBracketFound = false;
		boolean closeBracketFound = false;

		for (int cursor = 0; cursor < filenameMatcher.length(); cursor++) {
			char character = filenameMatcher.charAt(cursor);
			if (character == '!') {
				continue;
			} else if (character == '[') {
				openBracketFound = true;
			} else if (character == ']') {
				closeBracketFound = true;
			} else {
				if (openBracketFound && !closeBracketFound) {
					notMatcher.append(character);
				} else {
					matcherBuilder.append(character);
				}
			}
		}
		return new ImmutablePair<>(matcherBuilder.toString(), notMatcher.toString());
	}
}
