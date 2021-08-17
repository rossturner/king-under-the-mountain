package technology.rocketjump.undermount.materials.model;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.junit.Before;
import org.mockito.Mock;
import technology.rocketjump.undermount.assets.FloorTypeDictionary;
import technology.rocketjump.undermount.assets.WallTypeDictionary;
import technology.rocketjump.undermount.assets.entities.CompleteAssetDictionary;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.cooking.CookingRecipeDictionary;
import technology.rocketjump.undermount.crafting.CraftingRecipeDictionary;
import technology.rocketjump.undermount.entities.ai.goap.GoalDictionary;
import technology.rocketjump.undermount.entities.ai.goap.ScheduleDictionary;
import technology.rocketjump.undermount.entities.ai.goap.actions.ActionDictionary;
import technology.rocketjump.undermount.entities.components.ComponentDictionary;
import technology.rocketjump.undermount.entities.components.StatusEffectDictionary;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureLayoutDictionary;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.undermount.entities.tags.TagDictionary;
import technology.rocketjump.undermount.environment.DailyWeatherTypeDictionary;
import technology.rocketjump.undermount.environment.WeatherTypeDictionary;
import technology.rocketjump.undermount.jobs.CraftingTypeDictionary;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.jobs.JobTypeDictionary;
import technology.rocketjump.undermount.jobs.ProfessionDictionary;
import technology.rocketjump.undermount.mapping.tile.designation.TileDesignationDictionary;
import technology.rocketjump.undermount.materials.DynamicMaterialFactory;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rooms.RoomStore;
import technology.rocketjump.undermount.rooms.RoomTypeDictionary;
import technology.rocketjump.undermount.rooms.StockpileGroupDictionary;
import technology.rocketjump.undermount.rooms.components.RoomComponentDictionary;
import technology.rocketjump.undermount.sprites.BridgeTypeDictionary;

public class PersistenceTestHarness {

	protected  SavedGameStateHolder stateHolder;

	protected SavedGameDependentDictionaries dictionaries;
	@Mock
	protected DynamicMaterialFactory mockDynamicMaterialFactory;
	@Mock
	protected MessageDispatcher mockMessageDispatcher;
	@Mock
	protected GameMaterialDictionary mockMaterialDictionary;
	@Mock
	protected ProfessionDictionary mockProfessionDictionary;
	@Mock
	protected ItemTypeDictionary mockItemTypeDictionary;
	@Mock
	protected CookingRecipeDictionary mockCookingRecipeDictionary;
	@Mock
	protected FloorTypeDictionary mockFloorTypeDictionary;
	@Mock
	protected ComponentDictionary mockComponentDictionary;
	@Mock
	protected CraftingTypeDictionary mockCraftingTypeDictionary;
	@Mock
	protected CraftingRecipeDictionary mockCraftingRecipeDictionary;
	@Mock
	protected CompleteAssetDictionary mockCompleteAssetDictionary;
	@Mock
	protected GoalDictionary mockGoalDictionary;
	@Mock
	protected ScheduleDictionary mockScheduleDictionary;
	@Mock
	protected RoomStore mockRoomStore;
	@Mock
	protected ActionDictionary mockActionDictionary;
	@Mock
	protected FurnitureTypeDictionary mockFurnitureTypeDictionary;
	@Mock
	protected FurnitureLayoutDictionary mockFurnitureLayoutDictionary;
	@Mock
	protected PlantSpeciesDictionary mockPlantSpeciesDictionary;
	@Mock
	protected WallTypeDictionary mockWallTypeDictionary;
	@Mock
	protected RoomTypeDictionary mockRoomTypeDictionary;
	@Mock
	protected RoomComponentDictionary mockRoomComponentDictionary;
	@Mock
	protected TileDesignationDictionary mockTileDesignationDictionary;
	@Mock
	protected StockpileGroupDictionary mockStockpileGroupDictionary;
	@Mock
	protected TagDictionary mockTagDictionary;
	@Mock
	protected JobTypeDictionary mockJobTypeDictionary;
	@Mock
	protected StatusEffectDictionary mockStatusEffectDictionary;
	@Mock
	protected SoundAssetDictionary mockSoundAssetDictionary;
	@Mock
	protected BridgeTypeDictionary mockBridgeTypeDictionary;
	@Mock
	protected JobStore mockJobStore;
	@Mock
	private ParticleEffectTypeDictionary mockParticleEffectTypeDictionary;
	@Mock
	private OngoingEffectTypeDictionary mockOngoingEffectTypeDictionary;
	@Mock
	private WeatherTypeDictionary mockWeatherTypeDictionary;
	@Mock
	private DailyWeatherTypeDictionary mockDailyWeatherTypeDictionary;
	@Mock
	private MechanismTypeDictionary mockMechanismTypeDictionary;

	@Before
	public void setup() {
		stateHolder = new SavedGameStateHolder();

		dictionaries = new SavedGameDependentDictionaries(
				mockDynamicMaterialFactory,
				mockMaterialDictionary,
				mockMessageDispatcher,
				mockProfessionDictionary,
				mockJobTypeDictionary,
				mockItemTypeDictionary,
				mockFloorTypeDictionary,
				mockCookingRecipeDictionary,
				mockComponentDictionary,
				mockStatusEffectDictionary,
				mockCraftingTypeDictionary,
				mockCraftingRecipeDictionary,
				mockCompleteAssetDictionary,
				mockGoalDictionary,
				mockScheduleDictionary,
				mockRoomStore,
				mockActionDictionary,
				mockFurnitureTypeDictionary,
				mockFurnitureLayoutDictionary,
				mockPlantSpeciesDictionary,
				mockWallTypeDictionary,
				mockRoomTypeDictionary,
				mockRoomComponentDictionary,
				mockTileDesignationDictionary,
				mockStockpileGroupDictionary,
				mockTagDictionary,
				mockSoundAssetDictionary,
				mockBridgeTypeDictionary,
				mockJobStore,
				mockParticleEffectTypeDictionary,
				mockOngoingEffectTypeDictionary,
				mockWeatherTypeDictionary,
				mockDailyWeatherTypeDictionary,
				mockMechanismTypeDictionary);

	}

}
