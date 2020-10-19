package technology.rocketjump.undermount.mapping.factories;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Guice;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.guice.UndermountGuiceModule;
import technology.rocketjump.undermount.mapgen.model.output.GameMap;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.floor.RiverTile;
import technology.rocketjump.undermount.materials.model.GameMaterial;

import java.util.Random;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class WaterFlowCalculatorTest {

	private WaterFlowCalculator waterFlowCalculator;
	private MapGenWrapper mapGenWrapper;
	@Mock
	private FloorType mockFloorType;
	@Mock
	private GameMaterial mockFloorMaterial;

	@Before
	public void setUp() throws Exception {
		waterFlowCalculator = new WaterFlowCalculator();
		mapGenWrapper = Guice.createInjector(new UndermountGuiceModule()).getInstance(MapGenWrapper.class);
	}

	@Test
	public void calculateRiverFlow_assignsWaterSourceToEveryRiverTile_withSetSeed() throws Exception {
		runRiverTest(1L);
	}

	public void runRiverTest(long seed) {
		System.out.println("Using seed: " + seed);

		GameMap generatedMap = mapGenWrapper.createUsingLibrary(seed, 300, 200);
		TiledMap targetMap = new TiledMap(seed, generatedMap.getWidth(), generatedMap.getHeight(), mockFloorType, mockFloorMaterial);

		waterFlowCalculator.calculateRiverFlow(generatedMap, targetMap, seed);

		if (!generatedMap.getRiverStartTiles().isEmpty()) {
			for (GridPoint2 riverTileLocation : generatedMap.getRiverTiles()) {
				RiverTile tileWater = targetMap.getTile(riverTileLocation).getFloor().getRiverTile();
				assertThat(tileWater.getWaterAmount()).isEqualTo(RiverTile.MAX_WATER_PER_TILE);
			}
		}
	}

	@Test
	public void calculateRiverFlow_assignsWaterSourceToEveryRiverTile_withRandomSeeds() {
		Random random = new RandomXS128();
		final int NUM_TESTS_TO_RUN = 10;
		for (int i = 1; i <= NUM_TESTS_TO_RUN; i++) {
			System.out.println("\nTest " + i + " of " + NUM_TESTS_TO_RUN);
			runRiverTest(random.nextLong());
		}
	}

}