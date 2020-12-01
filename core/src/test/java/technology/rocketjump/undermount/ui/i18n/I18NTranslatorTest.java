package technology.rocketjump.undermount.ui.i18n;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.constants.ConstantsRepo;
import technology.rocketjump.undermount.entities.EntityAssetUpdater;
import technology.rocketjump.undermount.entities.EntityStore;
import technology.rocketjump.undermount.entities.ai.goap.GoalDictionary;
import technology.rocketjump.undermount.entities.ai.goap.ScheduleDictionary;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureCategoryDictionary;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureLayoutDictionary;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.factories.*;
import technology.rocketjump.undermount.entities.factories.names.DwarvenNameGenerator;
import technology.rocketjump.undermount.entities.factories.names.NorseNameGenerator;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.LocationComponent;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.humanoid.Gender;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.*;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.CraftingTypeDictionary;
import technology.rocketjump.undermount.jobs.JobTypeDictionary;
import technology.rocketjump.undermount.jobs.ProfessionDictionary;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.jobs.model.Profession;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.layout.WallLayout;
import technology.rocketjump.undermount.mapping.tile.wall.Wall;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.GameMaterialI18nUpdater;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.misc.twitch.TwitchDataStore;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.rooms.RoomStore;
import technology.rocketjump.undermount.rooms.StockpileGroupDictionary;
import technology.rocketjump.undermount.rooms.constructions.ConstructionState;
import technology.rocketjump.undermount.rooms.constructions.FurnitureConstruction;
import technology.rocketjump.undermount.rooms.constructions.WallConstruction;

import java.io.IOException;
import java.util.*;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;
import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.LANGUAGE;

@RunWith(MockitoJUnitRunner.class)
public class I18NTranslatorTest {

	private I18nTranslator translator;
	@Mock
	private ProfessionDictionary mockProfessionDictionary;
	@Mock
	private EntityAssetUpdater mockEntityAssetUpdater;
	@Mock
	private MessageDispatcher mockMessageDispatcher;
	private GameMaterialDictionary gameMaterialDictionary;
	private ItemTypeDictionary itemTypeDictionary;
	@Mock
	private CraftingType mockCraftingType;
	@Mock
	private TiledMap mockMap;
	@Mock
	private MapTile mockMapTile;
	@Mock
	private Entity mockEntity;
	@Mock
	private GameContext mockGameContext;
	@Mock
	private EntityStore mockEntityStore;
	@Mock
	private TextureAtlasRepository mockTextureAtlasRepository;
	@Mock
	private TextureAtlas mockTextureAtlas;
	@Mock
	private LocationComponent mockLocationComponent;
	@Mock
	private WallType mockWallType;
	@Mock
	private ItemType mockItemType;
	@Mock
	private GoalDictionary mockGoalDictionary;
	@Mock
	private ScheduleDictionary mockScheduleDictionary;
	@Mock
	private RoomStore mockRoomStore;
	@Mock
	private HairColorFactory mockColorFactory;
	@Mock
	private JobTypeDictionary mockJobTypeDictionary;
	@Mock
	private CraftingTypeDictionary mockCraftingTypeDictionary;
	@Mock
	private SoundAssetDictionary mockSoundAssetDictionary;
	@Mock
	private ConstantsRepo mockConstantsRepo;
	@Mock
	private UserPreferences mockUserPreferences;
	@Mock
	private ItemEntityAttributesFactory mockItemEntityAttributesFactory;
	@Mock
	private TwitchDataStore mockTwitchDataStore;

	@Before
	public void setup() throws IOException {
		Profession defaultProfession = new Profession();
		defaultProfession.setI18nKey("PROFESSION.VILLAGER");
		when(mockProfessionDictionary.getDefault()).thenReturn(defaultProfession);
		when(mockUserPreferences.getPreference(eq(LANGUAGE), any())).thenReturn("en-gb");

		when(mockCraftingTypeDictionary.getByName(Mockito.anyString())).thenReturn(mockCraftingType);
		when(mockWallType.getI18nKey()).thenReturn("WALL.STONE_BLOCK");

		I18nRepo i18nRepo = new I18nRepo(mockUserPreferences);

		itemTypeDictionary = new ItemTypeDictionary(mockCraftingTypeDictionary, new StockpileGroupDictionary(), mockSoundAssetDictionary, mockConstantsRepo);
		gameMaterialDictionary = new GameMaterialDictionary();
		new GameMaterialI18nUpdater(i18nRepo, gameMaterialDictionary).onLanguageUpdated();

		translator = new I18nTranslator(i18nRepo, mockProfessionDictionary, mockEntityStore);



		when(mockGameContext.getRandom()).thenReturn(new RandomXS128());
	}

	@Test
	public void describeHumanoid() throws IOException {
		NorseNameGenerator nameGenerator = new NorseNameGenerator();
		HumanoidEntityAttributes attributes = new HumanoidEntityAttributesFactory(
				new HairColorFactory(), new SkinColorFactory(), new AccessoryColorFactory(), new DwarvenNameGenerator(new NorseNameGenerator()),
				mockUserPreferences, mockTwitchDataStore).create(new GameContext());
		attributes.setName(nameGenerator.create(88L, Gender.MALE));

		Profession profession = new Profession();
		profession.setI18nKey("PROFESSION.BLACKSMITH");
		Entity entity = new HumanoidEntityFactory(
				mockMessageDispatcher, new ProfessionDictionary(), mockEntityAssetUpdater,
				mockGoalDictionary, mockScheduleDictionary, mockRoomStore).create(attributes, null, new Vector2(), profession, profession, mockGameContext);

		I18nText description = translator.getDescription(entity);

		assertThat(description.toString()).isEqualTo("Olin Olinson, dwarven blacksmith");
	}

	@Test
	public void describeItem() throws IOException {
		ItemEntityAttributes attributes = new ItemEntityAttributes(0);
		attributes.setQuantity(1);
		attributes.setItemType(itemTypeDictionary.getByName("Product-Ration"));
		attributes.setMaterial(gameMaterialDictionary.getByName("Rockbread"));

		Entity entity = new ItemEntityFactory(mockItemEntityAttributesFactory, mockMessageDispatcher, gameMaterialDictionary, mockEntityAssetUpdater).create(attributes, new GridPoint2(), true, mockGameContext);

		I18nText description = translator.getDescription(entity);

		assertThat(description.toString()).isEqualTo("Rockbread ration");
	}

	@Test
	public void describeSackOfVegetables() {
		ItemEntityAttributes attributes = new ItemEntityAttributes(0);
		attributes.setQuantity(2);
		attributes.setItemType(itemTypeDictionary.getByName("Ingredient-Vegetable-Sack"));
		attributes.setMaterial(gameMaterialDictionary.getByName("Potato"));

		Entity entity = new ItemEntityFactory(mockItemEntityAttributesFactory, mockMessageDispatcher, gameMaterialDictionary, mockEntityAssetUpdater).create(attributes, new GridPoint2(), true, mockGameContext);

		I18nText description = translator.getDescription(entity);

		assertThat(description.toString()).isEqualTo("2 potatoes");
	}

	@Test
	public void describeResources() throws IOException {
		Entity entity = createPileOfLogs();

		I18nText description = translator.getDescription(entity);

		assertThat(description.toString()).isEqualTo("5 oaken logs");
	}

	@Test
	public void describeTree() throws IOException {
		Entity entity = createTree();

		I18nText description = translator.getDescription(entity);

		assertThat(description.toString()).isEqualTo("Oak tree");
	}

	@Test
	public void describeShrub() throws IOException {
		Entity entity = createShrub();

		I18nText description = translator.getDescription(entity);

		assertThat(description.toString()).isEqualTo("Bush");
	}

	@Test
	public void describeFurniture() throws IOException {
		FurnitureEntityAttributes attributes = new FurnitureEntityAttributesFactory(new FurnitureTypeDictionary(new FurnitureCategoryDictionary(), new FurnitureLayoutDictionary(),
				itemTypeDictionary), mockColorFactory).byName("STONEMASON_WORKBENCH", gameMaterialDictionary.getByName("Granite"));
		Entity entity = new FurnitureEntityFactory(mockMessageDispatcher, mockEntityAssetUpdater).create(attributes, new GridPoint2(), null, mockGameContext);
		I18nText description = translator.getDescription(entity);

		assertThat(description.toString()).isEqualTo("Granite stonemason workbench");
	}

	@Test
	public void describeDoor() throws IOException {
		FurnitureEntityAttributes attributes = new FurnitureEntityAttributesFactory(new FurnitureTypeDictionary(new FurnitureCategoryDictionary(), new FurnitureLayoutDictionary(),
				itemTypeDictionary), mockColorFactory).byName("SINGLE_DOOR", gameMaterialDictionary.getByName("Oak"));
		Entity entity = new FurnitureEntityFactory(mockMessageDispatcher, mockEntityAssetUpdater).create(attributes, new GridPoint2(), null, mockGameContext);
		I18nText description = translator.getDescription(entity);

		assertThat(description.toString()).isEqualTo("Oaken door");
	}

	@Test
	public void describeFurnitureConstruction() throws IOException {
		GameMaterial material = gameMaterialDictionary.getByName("Oak");
		FurnitureEntityAttributes attributes = new FurnitureEntityAttributesFactory(new FurnitureTypeDictionary(new FurnitureCategoryDictionary(), new FurnitureLayoutDictionary(),
				itemTypeDictionary), mockColorFactory).byName("STONEMASON_WORKBENCH", material);
		Entity furnitureEntity = new FurnitureEntityFactory(mockMessageDispatcher, mockEntityAssetUpdater).create(attributes, new GridPoint2(), null, mockGameContext);

		FurnitureConstruction construction = new FurnitureConstruction(furnitureEntity);
		for (QuantifiedItemTypeWithMaterial requirement : construction.getRequirements()) {
			requirement.setMaterial(material);
		}

		assertThat(translator.getDescription(construction).toString()).isEqualTo("Construction of oaken stonemason workbench");
	}

//	@Test
//	public void describeJobs() throws IOException {
//		GameContext gameContext = new GameContext();
//		gameContext.setRandom(new RandomXS128());
//		gameContext.setAreaMap(mockMap);
//
//		NorseNameGenerator nameGenerator = new NorseNameGenerator();
//		HumanoidEntityAttributes attributes = new HumanoidEntityAttributesFactory(
//				new HairColorFactory(), new SkinColorFactory(), new AccessoryColorFactory(), nameGenerator
//		).create();
//		Profession profession = new Profession();
//		profession.setI18nKey("PROFESSION.BLACKSMITH");
//		Entity entity = new HumanoidEntityFactory(
//				mockMessageDispatcher, new ProfessionDictionary(), mockEntityAssetUpdater,
//				mockGoalDictionary, mockScheduleDictionary).create(attributes, null, new Vector2(), profession, gameContext);
//
//		HumanoidBehaviour behaviour = (HumanoidBehaviour) entity.getBehaviourComponent();
//
//		// Idle
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Idle");
//		behaviour.setCurrentGoal(new IdleGoal(entity, new GameClock(), 1f));
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Idle");
//		// Note that just going to a location as the primary goal is also idling
//		behaviour.setCurrentGoal(new GoToLocationGoal(entity, new Vector2(), mockMessageDispatcher, null));
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Idle");
//
//		// Working on job
//		WorkOnJobGoal workOnJobGoal = new WorkOnJobGoal(entity, mockMessageDispatcher, gameContext);
//		behaviour.setCurrentGoal(workOnJobGoal);
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Looking for work to do");
//
//		// Hauling job
//		HaulingAllocation haulingAllocation = new HaulingAllocation();
//		haulingAllocation.setQuantity(2);
//		haulingAllocation.setItemType(itemTypeDictionary.getByName("Resource-Gem"));
//		haulingAllocation.setGameMaterial(gameMaterialDictionary.getByName("Sapphire"));
//		workOnJobGoal.setAssignedJob(createHaulingJob(haulingAllocation, mockEntity));
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Hauling 2 sapphire gems");
//
//		haulingAllocation.setQuantity(1);
//		haulingAllocation.setItemType(itemTypeDictionary.getByName("Resource-Stone-Unrefined"));
//		haulingAllocation.setGameMaterial(gameMaterialDictionary.getByName("Marble"));
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Hauling marble rough stone boulder");
//
//		// Other jobs
//
//		ProfessionDictionary professionDictionary = new ProfessionDictionary();
//
//		// Crafting jobs
//
//		Job craftingJob = new Job(JobType.CRAFT_ITEM);
//		workOnJobGoal.setAssignedJob(craftingJob);
//
//		craftingJob.setRequiredProfession(professionDictionary.getByName("CARPENTER"));
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Crafting");
//		craftingJob.setRequiredProfession(professionDictionary.getByName("STONEMASON"));
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Sculpting");
//
//		// Mining job
//
//		Job miningJob = new Job(JobType.MINING);
//		miningJob.setJobLocation(new GridPoint2());
//		miningJob.setRequiredProfession(professionDictionary.getByName("MINER"));
//		workOnJobGoal.setAssignedJob(miningJob);
//
//		when(mockMap.getTile(any(GridPoint2.class))).thenReturn(mockMapTile);
//		Wall wall = new Wall(new WallLayout(1), new WallType("Test gem wall", "WALL.GEMS", 1L, GameMaterialType.GEM, false, null, null),
//				gameMaterialDictionary.getByName("Sapphire"));
//		when(mockMapTile.hasWall()).thenReturn(true);
//		when(mockMapTile.getWall()).thenReturn(wall);
//
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Mining sapphire gems");
//
//		wall = new Wall(new WallLayout(1), new WallType("Test rock wall", "WALL.ROUGH_STONE", 1L, GameMaterialType.STONE, false, null, null),
//				gameMaterialDictionary.getByName("Granite"));
//		when(mockMapTile.getWall()).thenReturn(wall);
//
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Mining granite rock wall");
//
//		// Logging job
//
//		Job loggingJob = new Job(JobType.LOGGING);
//		loggingJob.setRequiredProfession(professionDictionary.getByName("LUMBERJACK"));
//		loggingJob.setJobLocation(new GridPoint2());
//		workOnJobGoal.setAssignedJob(loggingJob);
//
//		List<Entity> tileEntities = new ArrayList<>();
//		tileEntities.add(createPlantForMap());
//		when(mockMapTile.getEntities()).thenReturn(tileEntities);
//		when(mockMapTile.hasTree()).thenReturn(true);
//
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Cutting oak tree");
//
//		// Constructing furniture job
//
//		Job constructionJob = new Job(JobType.CONSTRUCT_STONE_FURNITURE);
//		constructionJob.setRequiredProfession(professionDictionary.getByName("STONEMASON"));
//		constructionJob.setJobLocation(new GridPoint2());
//		workOnJobGoal.setAssignedJob(constructionJob);
//
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Sculpting furniture");
//
//		Job clearGroundJob = new Job(JobType.CLEAR_GROUND);
//		clearGroundJob.setRequiredProfession(null);
//		clearGroundJob.setJobLocation(new GridPoint2());
//		workOnJobGoal.setAssignedJob(clearGroundJob);
//
//		tileEntities.clearContextRelatedState();
//		tileEntities.add(createShrub());
//
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Clearing bush");
//
//		Job collectItemJob = new Job(JobType.COLLECT_ITEM);
//		collectItemJob.setRequiredProfession(null);
//		collectItemJob.setJobLocation(new GridPoint2());
//		collectItemJob.setTargetId(7L);
//		workOnJobGoal.setAssignedJob(collectItemJob);
//
//		Entity itemEntity = createPileOfLogs();
//		when(mockEntityStore.getById(7L)).thenReturn(itemEntity);
//
//		assertThat(translator.getCurrentGoalDescription(entity, mockGameContext)).isEqualTo("Hauling 5 oaken logs");
//	}

	@Test
	public void getValueForKey() throws Exception {
		assertThat(translator.getTranslatedString("GUI.ORDERS_LABEL").toString()).isEqualTo("Orders");
	}

	@Test
	public void getWallDescription_forAllMaterials() {
		when(mockMapTile.hasWall()).thenReturn(true);

		for (GameMaterial gameMaterial : gameMaterialDictionary.getAll()) {
			Wall testWall = new Wall(new WallLayout(0), mockWallType, gameMaterial);
			when(mockMapTile.getWall()).thenReturn(testWall);
			I18nText description = translator.getWallDescription(mockMapTile);
			assertThat(description).isNotNull();
			assertThat(description.toString().length()).isGreaterThan(0);
		}

	}

	@Test
	public void getWallConstructionDescription_doesNotShowMaterialType_whenNoMaterialSelected() {
		WallConstruction wallConstruction = createWallConstruction(NULL_MATERIAL);

		I18nText description = translator.getDescription(wallConstruction);

		assertThat(description.toString()).isEqualTo("Construction of smooth stone wall");
	}

	@Test
	public void getWallConstructionDescription_doesShowMaterialType_whenMaterialIsSelected() {
		WallConstruction wallConstruction = createWallConstruction(gameMaterialDictionary.getByName("Dolostone"));

		I18nText description = translator.getDescription(wallConstruction);

		assertThat(description.toString()).isEqualTo("Construction of dolostone smooth stone wall");
	}

	@Test
	public void getConstructionStatusDescription() {
		WallConstruction wallConstruction = createWallConstruction(NULL_MATERIAL);

		wallConstruction.setState(ConstructionState.CLEARING_WORK_SITE);

		assertThat(translator.getConstructionStatusDescription(wallConstruction).toString()).isEqualTo("Removing other items");

		wallConstruction.setState(ConstructionState.SELECTING_MATERIALS);

		assertThat(translator.getConstructionStatusDescription(wallConstruction).toString()).isEqualTo("Waiting for more stone blocks to be available");

		wallConstruction.setState(ConstructionState.WAITING_FOR_RESOURCES);

		assertThat(translator.getConstructionStatusDescription(wallConstruction).toString()).isEqualTo("Waiting for resources to arrive");

		wallConstruction.setState(ConstructionState.WAITING_FOR_COMPLETION);

		assertThat(translator.getConstructionStatusDescription(wallConstruction).toString()).isEqualTo("Under construction");
	}

	@Test
	public void getDateTimeString() {
		GameClock gameClock = new GameClock();

		I18nText result = translator.getDateTimeString(gameClock);

		assertThat(result.toString()).isEqualTo("08:00, day 1, spring");
	}

	private Entity createTree() throws IOException {
		PlantEntityAttributesFactory factory = new PlantEntityAttributesFactory(new PlantSpeciesDictionary(gameMaterialDictionary, itemTypeDictionary));
		Random random = new RandomXS128(1L);
		PlantSpeciesDictionary speciesDictionary = new PlantSpeciesDictionary(gameMaterialDictionary, itemTypeDictionary);
		PlantEntityAttributes attributes = factory.createBySpecies(speciesDictionary.getByName("Oak"), random);

		return new PlantEntityFactory(mockMessageDispatcher, mockEntityAssetUpdater, mockJobTypeDictionary).create(attributes, new GridPoint2(), mockGameContext);
	}

	public Entity createShrub() throws IOException {
		PlantEntityAttributesFactory factory = new PlantEntityAttributesFactory(new PlantSpeciesDictionary(gameMaterialDictionary, itemTypeDictionary));
		Random random = new RandomXS128(1L);
		PlantSpeciesDictionary speciesDictionary = new PlantSpeciesDictionary(gameMaterialDictionary, itemTypeDictionary);
		PlantEntityAttributes attributes = factory.createBySpecies(speciesDictionary.getByName("Tamarillo bush"), random);

		return new PlantEntityFactory(mockMessageDispatcher, mockEntityAssetUpdater, mockJobTypeDictionary).create(attributes, new GridPoint2(), mockGameContext);
	}

	public Entity createPileOfLogs() {
		ItemEntityAttributes attributes = new ItemEntityAttributes(1);
		attributes.setQuantity(5);
		attributes.setItemType(itemTypeDictionary.getByName("Resource-Logs"));
		attributes.setMaterial(gameMaterialDictionary.getByName("Oak"));

		return new ItemEntityFactory(mockItemEntityAttributesFactory, mockMessageDispatcher, gameMaterialDictionary, mockEntityAssetUpdater).create(attributes, new GridPoint2(), true, mockGameContext);
	}

	private WallConstruction createWallConstruction(GameMaterial material) {
		when(mockItemType.getI18nKey()).thenReturn("RESOURCE.STONE.BLOCK");
		when(mockItemType.getPrimaryMaterialType()).thenReturn(GameMaterialType.STONE);

		Map<GameMaterialType, List<QuantifiedItemType>> requirements = new HashMap<>();
		QuantifiedItemType item = new QuantifiedItemType();
		item.setQuantity(3);
		item.setItemType(mockItemType);
		List<QuantifiedItemType> items = Arrays.asList(item);
		requirements.put(GameMaterialType.STONE, items);
		when(mockWallType.getRequirements()).thenReturn(requirements);
		when(mockWallType.getMaterialType()).thenReturn(GameMaterialType.STONE);
		return new WallConstruction(new GridPoint2(), mockWallType, material);
	}

}