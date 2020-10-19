package technology.rocketjump.undermount.mapping.tile.wall;

import com.badlogic.gdx.utils.IntMap;
import org.junit.Test;
import technology.rocketjump.undermount.mapping.tile.layout.TileLayoutAtlas;
import technology.rocketjump.undermount.mapping.tile.layout.WallLayout;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class TileLayoutAtlasTest {

	@Test
	public void simplify_mapsIds_toReducedForm() {
		TileLayoutAtlas atlas = new TileLayoutAtlas();

		assertThat(atlas.simplifyLayoutId(0)).isEqualTo(0);
		assertThat(atlas.simplifyLayoutId(1)).isEqualTo(0);
		assertThat(atlas.simplifyLayoutId(2)).isEqualTo(2);
		assertThat(atlas.simplifyLayoutId(3)).isEqualTo(2);

		assertThat(atlas.simplifyLayoutId(234)).isEqualTo(106);
	}

	public static void main(String... args) throws IOException {
		// Used to produce output of all wall layout mappings
		TileLayoutAtlas atlas = new TileLayoutAtlas();
		IntMap<Integer> meaningfulLayouts = new IntMap<>();

		File output = new File("core/build/wallLayoutAtlas.txt");
		output.delete();
		output.createNewFile();

		try (FileWriter writer = new FileWriter(output)) {
			writer.write("\n");
			for (int i = 0; i < 256; i++) {

				WallLayout layoutForId = new WallLayout(i);
				int simplifiedId = atlas.simplifyLayoutId(i);
				meaningfulLayouts.put(i, simplifiedId);
				WallLayout simplifiedLayout = new WallLayout(simplifiedId);

				writer.write(String.valueOf(i) + "\t");
				writer.write(layoutForId.topRow() + " ");
				writer.write("=>\t");
				writer.write(simplifiedLayout.topRow());
				writer.write("\n\t" + layoutForId.middleRow() + "\t\t" + simplifiedLayout.middleRow());
				writer.write("\n\t" + layoutForId.bottomRow() + "\t\t" + simplifiedLayout.bottomRow());
				writer.write("\n\n");

			}
			writer.flush();
		}

		output = new File("core/build/meaningfulLayouts.txt");
		output.delete();
		output.createNewFile();

		try (FileWriter writer = new FileWriter(output)) {
			writer.write("\n");

			for (Integer layoutId : meaningfulLayouts.values()) {
				WallLayout meaningfulLayout = new WallLayout(layoutId);
				writer.write(String.valueOf(meaningfulLayout.getId()));
//				if (meaningful.isFlipped()) {
//					writer.write("f");
//				}
				writer.write("\t");
				writer.write(meaningfulLayout.topRow());
				writer.write("\n\t" + meaningfulLayout.middleRow());
				writer.write("\n\t" + meaningfulLayout.bottomRow());
				writer.write("\n\n");
			}

			writer.flush();
		}
	}

	@Test
	public void getUniqueLayouts_listsUniqueLayouts() {
		TileLayoutAtlas atlas = new TileLayoutAtlas();

		Set<Integer> uniqueLayouts = atlas.getUniqueLayouts();

		assertThat(uniqueLayouts).hasSize(47);
		assertThat(uniqueLayouts).contains(0, 2, 8, 10, 11, 16, 18, 22, 24, 26, 27, 30, 31, 64, 66, 72, 74,
				75, 80, 82, 86, 88, 90, 91, 94, 95, 104, 106, 107, 120, 122, 123, 126, 127, 208, 210, 214,
				216, 218, 219, 222, 223, 248, 250, 251, 254, 255);
	}

}