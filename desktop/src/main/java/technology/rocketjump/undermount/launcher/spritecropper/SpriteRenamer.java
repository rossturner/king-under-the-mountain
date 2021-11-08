package technology.rocketjump.undermount.launcher.spritecropper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class SpriteRenamer {

	private final ObjectWriter objectMapper;

	public SpriteRenamer() {
		objectMapper = new ObjectMapper().writerWithDefaultPrettyPrinter();
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			throw new IllegalArgumentException("This class must be passed a single argument of a directory to process");
		}
		Path filePath = Paths.get(new URL("file:///"+args[0]).toURI());
		new SpriteRenamer().processDirectory(filePath);
	}

	private void processDirectory(Path directoryPath) throws Exception {
		for (Path path : Files.list(directoryPath).collect(Collectors.toList())) {
			if (Files.isDirectory(path)) {
				processDirectory(path);
			} else if (path.getFileName().toString().endsWith("_NORMALS.png")) {
				// Ignore normals
			} else if (path.getFileName().toString().endsWith(".png")) {
				String filename = path.getFileName().toString();
				filename = filename.replaceAll("_", "");
				Files.move(path, directoryPath.resolve(filename));
			} else if (path.getFileName().toString().equalsIgnoreCase("descriptors.json")) {
				processDescriptors(path);
			}
		}
	}

	private void processDescriptors(Path descriptorsFilePath) throws IOException {
		JsonArray descriptorsJson = new Gson().fromJson(FileUtils.readFileToString(descriptorsFilePath.toFile()), JsonArray.class);
		descriptorsJson.forEach(node -> {
			if (node.isJsonObject()) {
				processDescriptorObject(node.getAsJsonObject());
			}
		});
		Gson gson = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.create();
		String outputText = gson.toJson(descriptorsJson);
		FileUtils.write(descriptorsFilePath.toFile(), outputText);
	}

	private void processDescriptorObject(JsonObject jsonObject) {
		for (String property : jsonObject.keySet()) {
			if (property.equals("filename")) {
				String filenameValue = jsonObject.get(property).getAsString();
				filenameValue = filenameValue.replaceAll("_", "");
				jsonObject.addProperty(property, filenameValue);
			} else if (jsonObject.get(property).isJsonObject()) {
				processDescriptorObject(jsonObject.getAsJsonObject(property));
			}
		}

	}

}
