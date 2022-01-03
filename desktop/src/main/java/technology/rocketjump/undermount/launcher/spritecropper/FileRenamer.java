package technology.rocketjump.undermount.launcher.spritecropper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class FileRenamer {

	private final ObjectWriter objectMapper;

	public FileRenamer() {
		objectMapper = new ObjectMapper().writerWithDefaultPrettyPrinter();
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			throw new IllegalArgumentException("This class must be passed 3 arguments of a directory path, the original word and replacement word");
		}
		Path filePath = Paths.get(new URL("file:///"+args[0]).toURI());
		new FileRenamer().processDirectory(filePath, args[1], args[2]);
	}

	private void processDirectory(Path directoryPath, String originalWord, String newWord) throws IOException {
		for (Path path : Files.list(directoryPath).collect(Collectors.toList())) {
			if (Files.isDirectory(path)) {
				processDirectory(path, originalWord, newWord);
			} else if (path.getFileName().toString().contains(originalWord)) {
				String newFilename = path.getFileName().toString().replace(originalWord, newWord);
				Files.move(path, directoryPath.resolve(newFilename));
			}
		}
	}

}
