package technology.rocketjump.undermount.mapping.tile.underground;

import technology.rocketjump.undermount.mapping.tile.floor.OverlapLayoutAtlas;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class PipeLayoutAtlasTest {



	public static void main(String... args) throws IOException {
		// Used to produce output of all wall layout mappings
		PipeLayoutAtlas atlas = new PipeLayoutAtlas();
		Map<Integer, OverlapLayoutAtlas.OverlapAtlasEntry> meaningfulLayouts = new LinkedHashMap<>();

		File output = new File("core/build/pipeLayoutAtlas.txt");
		output.delete();
		output.createNewFile();

		try (FileWriter writer = new FileWriter(output)) {
			writer.write("\n");
			for (Integer uniqueLayout : atlas.uniqueLayouts) {
				PipeLayout layoutForId = new PipeLayout(uniqueLayout);

				writer.write(String.valueOf(uniqueLayout) + "\n");
				writer.write(layoutForId.topRow() + "\n");
				writer.write(layoutForId.middleRow() + "\n");
				writer.write(layoutForId.bottomRow() + "\n\n");

			}
			writer.flush();
		}
	}

}