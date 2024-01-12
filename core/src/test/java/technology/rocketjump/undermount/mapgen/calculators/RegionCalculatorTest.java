package technology.rocketjump.undermount.mapgen.calculators;

import org.junit.Test;
import technology.rocketjump.undermount.mapgen.model.output.GameMap;
import technology.rocketjump.undermount.mapgen.model.output.TileType;

import static org.fest.assertions.Assertions.assertThat;

public class RegionCalculatorTest {

	@Test
	public void testSimpleCase() {
		GameMap map = new GameMap(4, 2);
		map.get(0, 0).setAsMountain();
		map.get(0, 1).setAsMountain();
		map.get(1, 0).setAsMountain();
		map.get(1, 1).setAsMountain();
		map.get(2, 0).setAsOutside();
		map.get(2, 1).setAsOutside();
		map.get(3, 0).setAsOutside();
		map.get(3, 1).setAsOutside();

		RegionCalculator regionCalculator = new RegionCalculator();

		regionCalculator.assignRegions(map);

		assertThat(map.getRegions().size()).isEqualTo(2);
		assertThat(map.getRegions().get(1L).size()).isEqualTo(4);
		assertThat(map.getRegions().get(1L).getTileType()).isEqualTo(TileType.MOUNTAIN);
		assertThat(map.getRegions().get(2L).size()).isEqualTo(4);
		assertThat(map.getRegions().get(2L).getTileType()).isEqualTo(TileType.OUTSIDE);
	}

}