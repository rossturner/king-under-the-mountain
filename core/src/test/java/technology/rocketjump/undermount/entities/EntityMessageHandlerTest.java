package technology.rocketjump.undermount.entities;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.entities.factories.ItemEntityAttributesFactory;
import technology.rocketjump.undermount.entities.factories.ItemEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.LocationComponent;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.JobFactory;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.undermount.rooms.RoomStore;
import technology.rocketjump.undermount.settlement.FurnitureTracker;
import technology.rocketjump.undermount.settlement.ItemTracker;
import technology.rocketjump.undermount.settlement.OngoingEffectTracker;
import technology.rocketjump.undermount.settlement.SettlerTracker;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EntityMessageHandlerTest {

	private static final long TARGET_ENTITY_ID = 7L;
	private EntityMessageHandler entityMessageHandler;
	private MessageDispatcher messageDispatcher;

	private GameContext gameContext;
	@Mock
	private EntityStore mockEntityStore;
	@Mock
	private TiledMap mockAreaMap;
	@Mock
	private Entity mockEntity;
	@Mock
	private LocationComponent mockLocationComponent;
	@Mock
	private MapTile mockTile;
	@Mock
	private EntityAssetUpdater mockAssetUpdater;
	@Mock
	private JobFactory mockJobFactory;
	@Mock
	private ItemTracker mockItemTracker;
	@Mock
	private FurnitureTracker mockFurnitureTracker;
	@Mock
	private SettlerTracker mockSettlerTracker;
	@Mock
	private RoomStore mockRoomStore;
	@Mock
	private ItemEntityAttributesFactory mockItemEntityAttributesFactory;
	@Mock
	private ItemEntityFactory mockItemEntityFactory;
	@Mock
	private ItemTypeDictionary mockItemTypeDictionary;
	@Mock
	private I18nTranslator mockI18nTranslator;
	@Mock
	private JobStore mockJobStore;
	@Mock
	private SoundAssetDictionary mockSoundAssetDictionary;
	@Mock
	private ParticleEffectTypeDictionary mockParticleEffectDictionary;
	@Mock
	private OngoingEffectTracker mockOngoingEffectTracker;
	@Mock
	private GameMaterialDictionary mockMaterialDictionary;

	@Before
	public void setUp() throws Exception {
		messageDispatcher = new MessageDispatcher();

		entityMessageHandler = new EntityMessageHandler(messageDispatcher, mockAssetUpdater, mockJobFactory,
				mockEntityStore, mockItemTracker, mockFurnitureTracker, mockSettlerTracker, mockOngoingEffectTracker, mockRoomStore,
				mockItemEntityAttributesFactory, mockItemEntityFactory, mockItemTypeDictionary, mockI18nTranslator, mockJobStore,
				mockMaterialDictionary, mockSoundAssetDictionary, mockParticleEffectDictionary);

		gameContext = new GameContext();
		gameContext.setAreaMap(mockAreaMap);

		entityMessageHandler.onContextChange(gameContext);
	}

	@Test
	public void handles_DestroyEntityMessage() {
		when(mockEntityStore.remove(TARGET_ENTITY_ID)).thenReturn(mockEntity);
		Vector2 entityWorldPosition = new Vector2(0.5f, 0.5f);
		when(mockEntity.getId()).thenReturn(TARGET_ENTITY_ID);
		when(mockEntity.getLocationComponent()).thenReturn(mockLocationComponent);
		when(mockEntity.getType()).thenReturn(EntityType.CREATURE);
		when(mockLocationComponent.getWorldPosition()).thenReturn(entityWorldPosition);
		when(mockAreaMap.getTile(entityWorldPosition)).thenReturn(mockTile);
		when(mockEntityStore.getById(TARGET_ENTITY_ID)).thenReturn(mockEntity);

		messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, mockEntity);

		verify(mockEntityStore).remove(TARGET_ENTITY_ID);
		verify(mockTile).removeEntity(TARGET_ENTITY_ID);
	}

}