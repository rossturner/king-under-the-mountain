package technology.rocketjump.undermount.modding.model;

import net.openhft.hashing.LongHashFunction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModArtifact {

	public final ModArtifactDefinition artifactDefinition;
	public final List<Path> sourceFiles = new ArrayList<>();
	private Object data;

	public ModArtifact(ModArtifactDefinition artifactDefinition) {
		this.artifactDefinition = artifactDefinition;
	}

	public long checksum() throws IOException {
		if (sourceFiles.isEmpty()) {
			throw new RuntimeException("Attempting to calculate checksum of empty " + getClass().getSimpleName());
		}
		long total = 0L;
		for (Path relatedFile : sourceFiles) {
			LongHashFunction hashFunction = LongHashFunction.xx();
			total += hashFunction.hashBytes(Files.readAllBytes(relatedFile));
			total += hashFunction.hashChars(relatedFile.getFileName().toString());
		}
		return total;
	}

	@Override
	public String toString() {
		return artifactDefinition.getName() + ", files: " + sourceFiles.size();
	}

	public void setData(Object object) {
		this.data = object;
	}

	public Object getData() {
		return data;
	}
}
