package technology.rocketjump.undermount.mapping.tile.floor;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class OverlapLayoutAtlasTest {

	@Test
	public void library_mapsIds_toReducedForm() {
		OverlapLayoutAtlas atlas = new OverlapLayoutAtlas();

		assertThat(atlas.getByLayoutId(0).getLayout().getId()).isEqualTo(0);
		assertThat(atlas.getByLayoutId(1).getLayout().getId()).isEqualTo(1);
		assertThat(atlas.getByLayoutId(2).getLayout().getId()).isEqualTo(2);
		assertThat(atlas.getByLayoutId(3).getLayout().getId()).isEqualTo(2);
	}

	public static void main(String... args) throws IOException {
		// Used to produce output of all wall layout mappings
		OverlapLayoutAtlas atlas = new OverlapLayoutAtlas();
		Map<Integer, OverlapLayoutAtlas.OverlapAtlasEntry> meaningfulLayouts = new LinkedHashMap<>();

		File output = new File("core/build/overlapLayoutAtlas.txt");
		output.delete();
		output.createNewFile();

		try (FileWriter writer = new FileWriter(output)) {
			writer.write("\n");
			for (int i = 0; i < 256; i++) {

				OverlapLayout layoutForId = new OverlapLayout(i);
				OverlapLayoutAtlas.OverlapAtlasEntry atlasEntry = atlas.getByLayoutId(i);
				if (!atlasEntry.isFlipX() && !atlasEntry.isFlipY()) {
					meaningfulLayouts.put(atlasEntry.getLayout().getId(), atlasEntry);
				}
				OverlapLayout atlasLayout;

				writer.write(String.valueOf(i) + "\t");
				writer.write(layoutForId.topRow() + " ");

				if (atlasEntry.isFlipX()) {
					writer.write("x");
				}
				if (atlasEntry.isFlipY()) {
					writer.write("y");
				}
				writer.write("=>\t");
				atlasLayout = atlasEntry.getUnflippedLayout();
				writer.write(atlasLayout.topRow());
				writer.write("\n\t" + layoutForId.middleRow() + "\t\t" + atlasLayout.middleRow());
				writer.write("\n\t" + layoutForId.bottomRow() + "\t\t" + atlasLayout.bottomRow());
				writer.write("\n\n");

			}
			writer.flush();
		}

		output = new File("core/build/meaningfulOverlapLayouts.txt");
		output.delete();
		output.createNewFile();

		try (FileWriter writer = new FileWriter(output)) {
			writer.write("\n");
			for (OverlapLayoutAtlas.OverlapAtlasEntry meaningful : meaningfulLayouts.values()) {
				OverlapLayout meaningfulLayout = meaningful.getLayout();
				writer.write(String.valueOf(meaningfulLayout.getId()));
				writer.write("\t");
				writer.write(meaningfulLayout.topRow());
				writer.write("\n\t" + meaningfulLayout.middleRow());
				writer.write("\n\t" + meaningfulLayout.bottomRow());
				writer.write("\n\n");
			}

			writer.flush();
		}
	}

}