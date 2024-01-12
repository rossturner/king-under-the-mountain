package technology.rocketjump.undermount.mapgen;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.RandomXS128;
import technology.rocketjump.undermount.mapgen.generators.GameMapGenerator;
import technology.rocketjump.undermount.mapgen.generators.SequentialIdGenerator;
import technology.rocketjump.undermount.mapgen.model.RockGroup;
import technology.rocketjump.undermount.mapgen.model.input.*;
import technology.rocketjump.undermount.mapgen.rendering.MapRenderer;
import technology.rocketjump.undermount.mapgen.rendering.camera.MapGenInputProcessor;
import technology.rocketjump.undermount.mapgen.rendering.camera.OnKeyUp;

import java.util.Random;

import static com.badlogic.gdx.Gdx.input;

public class MapGenApplicationAdapter extends ApplicationAdapter implements OnKeyUp {
	public static final float TIME_BETWEEN_UPDATES = 0.2f;

	private MapRenderer mapRenderer;
	private Random random;

	private GameMapGenerator mapGenerator;

	private MapGenInputProcessor inputProcessor;

	private float timeSinceLastUpdate = 0f;

	@Override
	public void create () {
		mapRenderer = new MapRenderer();
		inputProcessor = new MapGenInputProcessor(this);
		input.setInputProcessor(inputProcessor);

		reset();
	}

	@Override
	public void render () {
		float deltaTime = Gdx.graphics.getDeltaTime();
		update(deltaTime);

		Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		mapRenderer.render(mapGenerator.getCurrentMap());
	}

	private void update(float deltaTime) {
		timeSinceLastUpdate += deltaTime;
		if (timeSinceLastUpdate > TIME_BETWEEN_UPDATES) {
			timeSinceLastUpdate = 0;
			mapGenerator.processNextStep();
		}
	}

	public void reset() {
		long seed = new RandomXS128().nextLong();
//		long seed = -8022446485855808454L;
		System.out.println("Using seed " + seed);
		random = new RandomXS128(seed);

		// See http://wiki.terrafirmacraft.com/Ores_%26_Minerals

		OreType ironOre = new OreType("Iron Ore", new Color(153f/255f, 0.1f, 0.1f, 1f), 1f);
		OreType coal = new OreType("Bituminous coal", new Color(0.1f, 0.1f, 0.1f, 1f), 2.3f);
		OreType nativeGold = new OreType("Native gold", new Color(1f, 1f, 0.1f, 1f), 0.3f);
		OreType tinOre = new OreType("Tin ore / cassiterite", new Color(204f/255f, 230f/255f, 255f/255f, 1), 0.3f);
		OreType nativeCopper = new OreType("Native copper", new Color(255f/255f, 140f/255f, 26f/255f, 1), 0.6f);
		OreType silverOre = new OreType("Silver ore / Galena", new Color(204f/255f, 204f/255f, 204f/255f, 1), 0.4f);
		OreType nativeSilver = new OreType("Native silver", new Color(234f/255f, 234f/255f, 234f/255f, 1), 0.4f);
		OreType copperOre = new OreType("Copper ore / Malachite", new Color(140f/255f, 200f/255f, 126f/255f, 1), 0.6f);

		RockType sandstone = new RockType(RockGroup.Sedimentary, "Sandstone", new Color(255f/255f, 153f/255f, 51f/255f, 1), 1f);
		sandstone.addOreType(ironOre, coal, tinOre);
		RockType limestone = new RockType(RockGroup.Sedimentary, "Limestone", new Color(255f/255f, 255f/255f, 230f/255f, 1), 2f);
		limestone.addOreType(ironOre, coal, tinOre);
		RockType shale = new RockType(RockGroup.Sedimentary, "Shale", new Color(77f/255f, 25f/255f, 25f/255f, 1), 1f);
		shale.addOreType(ironOre, coal, tinOre);
		RockType dolostone = new RockType(RockGroup.Sedimentary, "Dolostone", new Color(243f/255f, 216f/255f, 216f/255f, 1), 1f);
		dolostone.addOreType(ironOre, coal, tinOre);

		RockType granite = new RockType(RockGroup.Igneous, "Granite", new Color(77f/255f, 77f/255f, 77f/255f, 1), 4f);
		granite.addOreType(nativeGold, ironOre, nativeCopper, nativeSilver);
		RockType basalt = new RockType(RockGroup.Igneous, "Basalt", new Color(140f/255f, 140f/255f, 140f/255f, 1), 1f);
		basalt.addOreType(nativeGold, ironOre, nativeCopper);

		RockType marble = new RockType(RockGroup.Metamorphic, "Marble", new Color(240f/255f, 240f/255f, 240f/255f, 1), 1f);
		marble.addOreType(copperOre, silverOre);
		RockType slate = new RockType(RockGroup.Metamorphic, "Slate", new Color(140f/255f, 140f/255f, 140f/255f, 1), 1f);
		slate.addOreType(silverOre, copperOre /* and zinc ore */);

		GemType amethyst = new GemType(RockGroup.Igneous, "Amethyst", new Color(230f/255f, 40f/255f, 240f/255f, 1f), 1f);
		GemType rockCrystal = new GemType(RockGroup.Sedimentary, "Rock Crystal", new Color(250f/255f, 250f/255f, 250f/255f, 1f), 1f);
		GemType emerald = new GemType(RockGroup.Metamorphic, "Emerald", new Color(30f/255f, 255f/255f, 70f/255f, 1f), 1f);

		GameMapGenerationParams generationParams = new GameMapGenerationParams(320, 240);
		generationParams.addRockTypes(sandstone, limestone, shale, dolostone);
		generationParams.addRockTypes(granite, basalt);
		generationParams.addRockTypes(marble, slate);

		generationParams.addGemTypes(amethyst, rockCrystal, emerald);

		generationParams.getTreeTypes().add(new TreeType("Tree type A"));
		generationParams.getTreeTypes().add(new TreeType("Tree type B"));

		generationParams.getShrubTypes().add(new ShrubType("Non fruit A", false));
		generationParams.getShrubTypes().add(new ShrubType("Non fruit B", false));
		generationParams.getShrubTypes().add(new ShrubType("Fruit A", true));
		generationParams.getShrubTypes().add(new ShrubType("Fruit B", true));

		generationParams.getMushroomTypes().add(new MushroomType("Mushroom A", 0.2f));
		generationParams.getMushroomTypes().add(new MushroomType("Mushroom B", 0.4f));

		mapGenerator = new GameMapGenerator(generationParams, random);

//		long startTime = System.currentTimeMillis();
//		mapGenerator.completeGeneration();
//		long endTime = System.currentTimeMillis();
//		System.out.println("Map generation completed in " + (endTime - startTime) + "ms");

		timeSinceLastUpdate = -1f;
		SequentialIdGenerator.reset();

	}
	
	@Override
	public void dispose () {
	}

	@Override
	public void onKeyUp(int keycode) {
		if (keycode == Input.Keys.R) {
			reset();
		}
	}
}
