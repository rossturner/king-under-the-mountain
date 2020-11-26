package technology.rocketjump.undermount.mapping.tile.floor;

import com.badlogic.gdx.utils.IntArray;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

public class OverlapQuadrantDictionaryTest {


	public static void main(String... args) throws IOException {
		// Used to produce output of all wall layout mappings
		OverlapLayoutAtlas atlas = new OverlapLayoutAtlas();
		OverlapQuadrantDictionary quadrantDictionary = new OverlapQuadrantDictionary(atlas);

		File output = new File("core/build/overlapQuadrantAtlas.txt");
		output.delete();
		output.createNewFile();

		try (FileWriter writer = new FileWriter(output)) {
			writer.write("\n");
			Set<OverlapLayout> layouts = new LinkedHashSet<>();
			for (int i = 0; i < 256; i++) {
				layouts.add(new OverlapLayout(i).reduceToMeaningfulForm());
			}

			for (OverlapLayout layout : layouts) {

				writer.write(String.valueOf(layout.getId()) + "\t");
				writer.write(layout.topRow() + " ");

				writer.write("=>\t");
				writer.write(layout.topRow());
				writer.write("\n\t" + layout.middleRow());
				writer.write("\n\t" + layout.bottomRow());
				writer.write("\n");

				IntArray overlapQuadrants = quadrantDictionary.getOverlapQuadrants(layout.getId());
				writer.write(String.valueOf(overlapQuadrants.get(0)));
				writer.write(",");
				writer.write(String.valueOf(overlapQuadrants.get(1)));
				writer.write(",");
				writer.write(String.valueOf(overlapQuadrants.get(2)));
				writer.write(",");
				writer.write(String.valueOf(overlapQuadrants.get(3)));

				writer.write("\n\n");

			}
			writer.flush();
		}
	}
}