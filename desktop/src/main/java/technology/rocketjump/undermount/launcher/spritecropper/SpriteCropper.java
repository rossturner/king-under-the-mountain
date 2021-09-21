package technology.rocketjump.undermount.launcher.spritecropper;

import com.badlogic.gdx.math.Vector2;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SpriteCropper {

	private static final int ALPHA_BAND = 3;
	private final ObjectWriter objectMapper;

	public SpriteCropper() {
		objectMapper = new ObjectMapper().writerWithDefaultPrettyPrinter();
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			throw new IllegalArgumentException("This class must be passed a single argument of a directory to process");
		}
		Path filePath = Paths.get(new URL("file:///"+args[0]).toURI());
		new SpriteCropper().processDirectory(filePath);
	}

	private void processDirectory(Path filePath) throws Exception {
		Map<String, Path> spriteFiles = new HashMap<>();
		Path descriptorsFilePath = null;
		JsonArray descriptorsJson = null;

		for (Path path : Files.list(filePath).collect(Collectors.toList())) {
			if (Files.isDirectory(path)) {
				processDirectory(path);
			} else if (path.getFileName().toString().endsWith("_NORMALS.png")) {
				// Ignore normals
			} else if (path.getFileName().toString().endsWith(".png")) {
				spriteFiles.put(path.getFileName().toString(), path);
			} else if (path.getFileName().toString().equalsIgnoreCase("descriptors.json")) {
				descriptorsFilePath = path;
				descriptorsJson = new Gson().fromJson(FileUtils.readFileToString(path.toFile()), JsonArray.class);
			}
		}

		if (descriptorsJson != null) {
			processSprites(descriptorsJson, spriteFiles);
			// write descriptors.json back in place
			Gson gson = new GsonBuilder()
					.setPrettyPrinting()
					.disableHtmlEscaping()
					.create();
			String outputText = gson.toJson(descriptorsJson);

			FileUtils.write(descriptorsFilePath.toFile(), outputText);
		}
	}

	private void processSprites(JsonArray descriptorsJson, Map<String, Path> spriteFiles) throws Exception {
		Map<String, Vector2> newOffsets = new HashMap<>();

		for (Map.Entry<String, Path> entry : spriteFiles.entrySet()) {
			String filename = entry.getKey();
			System.out.println("Processing " + filename);
			Path spriteFile = entry.getValue();

			BufferedImage original = ImageIO.read(spriteFile.toFile());
			int width = original.getWidth();
			int height = original.getHeight();

			int cropLeft, cropTop, cropRight, cropBottom;
			for (cropLeft = 0; cropLeft < width; cropLeft++) {
				boolean entireLineTransparent = true;
				for (int y = 0; y < height; y++) {
					boolean transparent = original.getData().getSample(cropLeft, y, ALPHA_BAND) == 0;
					if (!transparent) {
						entireLineTransparent = false;
						break;
					}
				}
				if (!entireLineTransparent) {
					break;
				}
			}
			for (cropTop = 0; cropTop < height; cropTop++) {
				boolean entireLineTransparent = true;
				for (int x = 0; x < width; x++) {
					boolean transparent = original.getData().getSample(x, height - 1 - cropTop, ALPHA_BAND) == 0;
					if (!transparent) {
						entireLineTransparent = false;
						break;
					}
				}
				if (!entireLineTransparent) {
					break;
				}
			}
			for (cropRight = 0; cropRight < width; cropRight++) {
				boolean entireLineTransparent = true;
				for (int y = 0; y < height; y++) {
					boolean transparent = original.getData().getSample(width - 1 - cropRight, y, ALPHA_BAND) == 0;
					if (!transparent) {
						entireLineTransparent = false;
						break;
					}
				}
				if (!entireLineTransparent) {
					break;
				}
			}
			for (cropBottom = 0; cropBottom < height; cropBottom++) {
				boolean entireLineTransparent = true;
				for (int x = 0; x < width; x++) {
					boolean transparent = original.getData().getSample(x, cropBottom, ALPHA_BAND) == 0;
					if (!transparent) {
						entireLineTransparent = false;
						break;
					}
				}
				if (!entireLineTransparent) {
					break;
				}
			}
			// Reduce all by 1 for entirely transparent padding around image
			if (cropLeft > 0) {
				cropLeft--;
			}
			if (cropTop > 0) {
				cropTop--;
			}
			if (cropRight > 0) {
				cropRight--;
			}
			if (cropBottom > 0) {
				cropBottom--;
			}

			if (cropLeft > 0 || cropTop > 0 || cropRight > 0 || cropBottom > 0) {
				int newWidth = width - cropLeft - cropRight;
				int newHeight = height - cropTop - cropBottom;

				BufferedImage croppedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
				croppedImage.getGraphics().drawImage(original, 0, 0, newWidth, newHeight, cropLeft, cropBottom, cropLeft + newWidth, cropBottom + newHeight, null);

				ImageIO.write(croppedImage, "png", spriteFile.toFile());

				Vector2 originalMidpoint = new Vector2(((float)width) / 2f, ((float)height) / 2f);
				Vector2 newMidpoint = new Vector2(cropLeft + (((float)newWidth) / 2f), cropBottom + ((float)newHeight / 2f));
				Vector2 offset = originalMidpoint.cpy().sub(newMidpoint);
				offset.x = 0-offset.x;
				newOffsets.put(filename, offset);
			}
		}

		processDescriptors(descriptorsJson, newOffsets);
	}

	private void processDescriptors(JsonArray descriptorsJson, Map<String, Vector2> newOffsets) {
		for (int cursor = 0; cursor < descriptorsJson.size(); cursor++) {
			JsonObject descriptorRootNode = descriptorsJson.get(cursor).getAsJsonObject();

			JsonObject spriteDescriptors = descriptorRootNode.getAsJsonObject("spriteDescriptors");
			if (spriteDescriptors == null) {
				System.err.println("Could not find spriteDescriptors in " + descriptorRootNode.toString());
			} else {
				for (String direction : spriteDescriptors.keySet()) {
					JsonObject directionJson = spriteDescriptors.getAsJsonObject(direction);

					String filename = directionJson.get("filename").getAsString();
					System.out.println("Processing descriptors for " + filename);
					Vector2 newOffset = newOffsets.get(filename);
					if (newOffset != null) {
						JsonObject originalOffset = directionJson.getAsJsonObject("offsetPixels");
						if (originalOffset == null) {
							originalOffset = new JsonObject();
						}
						Vector2 replacementOffset = newOffset.cpy().add(
							originalOffset.get("x") == null ? 0f : originalOffset.get("x").getAsFloat(),
							originalOffset.get("y") == null ? 0f : originalOffset.get("y").getAsFloat()
						);
						originalOffset.addProperty("x", replacementOffset.x);
						originalOffset.addProperty("y", replacementOffset.y);

						directionJson.add("offsetPixels", originalOffset);

						JsonArray childAssets = directionJson.getAsJsonArray("childAssets");
						if (childAssets != null) {
							for (int childCursor = 0; childCursor < childAssets.size(); childCursor++) {
								JsonObject childAssetJson = childAssets.get(childCursor).getAsJsonObject();
								JsonObject childOffsetJson = childAssetJson.getAsJsonObject("offsetPixels");
								if (childOffsetJson == null) {
									childOffsetJson = new JsonObject();
								}
								Vector2 childOffsetVec = new Vector2(
										childOffsetJson.get("x") == null ? 0f : childOffsetJson.get("x").getAsFloat(),
										childOffsetJson.get("y") == null ? 0f : childOffsetJson.get("y").getAsFloat()
								);
								childOffsetVec.sub(newOffset);
								childOffsetJson.addProperty("x", childOffsetVec.x);
								childOffsetJson.addProperty("y", childOffsetVec.y);
								childAssetJson.add("offsetPixels", childOffsetJson);
							}
						}
					}
				}
			}
		}
	}
}
