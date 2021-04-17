package technology.rocketjump.undermount.mapping;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.GridPoint2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileNeighbours;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.undermount.rooms.RoomFactory;
import technology.rocketjump.undermount.rooms.RoomStore;
import technology.rocketjump.undermount.rooms.StockpileComponentUpdater;
import technology.rocketjump.undermount.ui.GameInteractionStateContainer;
import technology.rocketjump.undermount.zones.Zone;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class MapMessageHandlerTest {

	@Mock
	private FloorType mockFloorType;
	@Mock
	private WallType mockWallType;
	@Mock
	private MessageDispatcher mockMessageDispatcher;
	@Mock
	private OutdoorLightProcessor mockOutdoorLightProcessor;
	@Mock
	private GameInteractionStateContainer mockInteractionStateContainer;
	@Mock
	private RoomFactory mockRoomfactory;
	@Mock
	private RoomStore mockRoomStore;
	@Mock
	private JobStore mockJobStore;
	@Mock
	private StockpileComponentUpdater mockStockpileComponentUpdater;
	@Mock
	private ParticleEffectTypeDictionary mockParticleEffectTypeDictionary;
	@Mock
	private SoundAssetDictionary mockSoundAssetDictionary;

	@Test
	public void removeWall_joinsRegions_keepsZones() {
		TiledMap map = new TiledMap(1, 5, 5, mockFloorType, GameMaterial.NULL_MATERIAL);

		int region1 = map.createNewRegionId();
		int region2 = map.createNewRegionId();
		int region3 = map.createNewRegionId();

		map.getTile(0, 0).setRegionId(region1);
		map.getTile(0, 1).setRegionId(region1);
		map.getTile(0, 2).setRegionId(region1);
		map.getTile(0, 3).setRegionId(region1);
		map.getTile(0, 4).setRegionId(region1);
		map.getTile(1, 0).setRegionId(region1);
		map.getTile(1, 1).setRegionId(region1);
		map.getTile(1, 2).setRegionId(region1);
		map.getTile(1, 3).setRegionId(region1);
		map.getTile(1, 4).setRegionId(region1);

		map.getTile(2, 0).addWall(new TileNeighbours(), GameMaterial.NULL_MATERIAL, mockWallType);
		map.getTile(2, 0).setRegionId(region2);
		map.getTile(2, 1).addWall(new TileNeighbours(), GameMaterial.NULL_MATERIAL, mockWallType);
		map.getTile(2, 1).setRegionId(region2);
		map.getTile(2, 2).addWall(new TileNeighbours(), GameMaterial.NULL_MATERIAL, mockWallType);
		map.getTile(2, 2).setRegionId(region2);
		map.getTile(2, 3).addWall(new TileNeighbours(), GameMaterial.NULL_MATERIAL, mockWallType);
		map.getTile(2, 3).setRegionId(region2);
		map.getTile(2, 4).addWall(new TileNeighbours(), GameMaterial.NULL_MATERIAL, mockWallType);
		map.getTile(2, 4).setRegionId(region2);


		map.getTile(3, 0).setRegionId(region3);
		map.getTile(3, 1).setRegionId(region3);
		map.getTile(3, 2).setRegionId(region3);
		map.getTile(3, 3).setRegionId(region3);
		map.getTile(3, 4).setRegionId(region3);
		map.getTile(4, 0).setRegionId(region3);
		map.getTile(4, 1).setRegionId(region3);
		map.getTile(4, 2).setRegionId(region3);
		map.getTile(4, 3).setRegionId(region3);
		map.getTile(4, 4).setRegionId(region3);

		Zone leftEdgeZone = new Zone();
		leftEdgeZone.add(
				map.getTile(1, 0), map.getTile(0, 0)
		);
		leftEdgeZone.add(
				map.getTile(1, 1), map.getTile(0, 1)
		);
		leftEdgeZone.add(
				map.getTile(1, 2), map.getTile(0, 2)
		);
		leftEdgeZone.add(
				map.getTile(1, 3), map.getTile(0, 3)
		);
		leftEdgeZone.add(
				map.getTile(1, 4), map.getTile(0, 4)
		);
		map.addZone(leftEdgeZone);


		Zone rightEdgeZone = new Zone();
		rightEdgeZone.add(
				map.getTile(3, 0), map.getTile(4, 0)
		);
		rightEdgeZone.add(
				map.getTile(3, 1), map.getTile(4, 1)
		);
		rightEdgeZone.add(
				map.getTile(3, 2), map.getTile(4, 2)
		);
		rightEdgeZone.add(
				map.getTile(3, 3), map.getTile(4, 3)
		);
		rightEdgeZone.add(
				map.getTile(3, 4), map.getTile(4, 4)
		);
		map.addZone(rightEdgeZone);

		MapMessageHandler mapMessageHandler = new MapMessageHandler(mockMessageDispatcher, mockOutdoorLightProcessor,
				mockInteractionStateContainer, mockRoomfactory, mockRoomStore, mockJobStore, mockStockpileComponentUpdater,
				mockParticleEffectTypeDictionary, mockSoundAssetDictionary);
		GameContext gameContext = new GameContext();
		gameContext.setAreaMap(map);
		mapMessageHandler.onContextChange(gameContext);

		Telegram telegram = new Telegram();
		telegram.message = MessageType.REMOVE_WALL;
		telegram.extraInfo = new GridPoint2(2, 2);

		mapMessageHandler.handleMessage(telegram);

		MapTile leftTile = map.getTile(1, 0);
		MapTile rightTile = map.getTile(3, 0);
		assertThat(leftTile.getRegionId()).isEqualTo(rightTile.getRegionId());

		assertThat(leftTile.getZones()).hasSize(1);
		assertThat(rightTile.getZones()).hasSize(1);
	}

}