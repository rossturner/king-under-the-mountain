package technology.rocketjump.undermount.rooms.components;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.rooms.RoomTile;
import technology.rocketjump.undermount.rooms.StockpileAllocation;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;

@RunWith(MockitoJUnitRunner.class)
public class StockpileComponentTest {

	private StockpileComponent stockpileComponent;

	@Mock
	private Room mockRoom;
	private Map<GridPoint2, RoomTile> roomTiles;
	@Mock
	private TiledMap mockMap;
	@Mock
	private Entity mockItem;
	@Mock
	private PhysicalEntityComponent mockPhysicalComponent;
	private ItemEntityAttributes itemAttributes;
	@Mock
	private ItemType mockItemType;
	private GridPoint2 position1;
	private GridPoint2 position2;
	@Mock
	private MapTile mockTile1;
	@Mock
	private MapTile mockTile2;
	@Mock
	private MessageDispatcher mockMessageDispatcher;

	@Before
	public void setUp() throws Exception {
		stockpileComponent = new StockpileComponent(mockRoom, mockMessageDispatcher);

		roomTiles = new HashMap<>();

		RoomTile tile1 = new RoomTile();
		position1 = new GridPoint2(0, 0);
		roomTiles.put(position1, tile1);
		RoomTile tile2 = new RoomTile();
		position2 = new GridPoint2(0, 1);
		roomTiles.put(position2, tile2);

		when(mockRoom.getRoomTiles()).thenReturn(roomTiles);

		when(mockItem.getPhysicalEntityComponent()).thenReturn(mockPhysicalComponent);
		itemAttributes = new ItemEntityAttributes(0L);
		itemAttributes.setItemType(mockItemType);
		itemAttributes.setMaterial(NULL_MATERIAL);
		itemAttributes.setQuantity(10);
		when(mockPhysicalComponent.getAttributes()).thenReturn(itemAttributes);

		when(mockItemType.getMaxStackSize()).thenReturn(100);
		when(mockItemType.getMaxHauledAtOnce()).thenReturn(100);
		when(mockItemType.getPrimaryMaterialType()).thenReturn(NULL_MATERIAL.getMaterialType());

		when(mockMap.getTile(position1)).thenReturn(mockTile1);
		when(mockMap.getTile(position2)).thenReturn(mockTile2);

		when(mockTile1.isEmpty()).thenReturn(true);
		when(mockTile2.isEmpty()).thenReturn(true);

		when(mockItem.getType()).thenReturn(EntityType.ITEM);
		ItemAllocationComponent itemAllocationComponent = new ItemAllocationComponent();
		itemAllocationComponent.init(mockItem, mockMessageDispatcher, null);
		when(mockItem.getComponent(ItemAllocationComponent.class)).thenReturn(itemAllocationComponent);
	}

	@Test
	public void allocate_stacks_items_into_same_tile() {

		for (int i = 1; i <= 10; i++) {
			stockpileComponent.requestAllocation(mockItem, mockMap);
		}

		StockpileAllocation allocation = stockpileComponent.getAllocationAt(new GridPoint2(0, 0));
		if (allocation == null) {
			allocation = stockpileComponent.getAllocationAt(new GridPoint2(0, 1));
		}

		assertThat(allocation.getTotalQuantity()).isEqualTo(100);
	}

}