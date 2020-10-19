package technology.rocketjump.undermount.settlement;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.assets.entities.item.*;
import technology.rocketjump.undermount.entities.EntityAssetUpdater;
import technology.rocketjump.undermount.entities.factories.ItemEntityAttributesFactory;
import technology.rocketjump.undermount.entities.factories.ItemEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.guice.UndermountGuiceModule;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ItemTrackerTest {

	private ItemTracker itemTracker;
	private ItemTypeDictionary itemTypeDictionary;
	private GameMaterialDictionary gameMaterialDictionary;
	@Mock
	private EntityAssetUpdater mockAssetUpdater;
	@Mock
	private GameContext mockContext;
	@Mock
	private ItemEntityAssetDictionary mockItemEntityAssetDictionary;
	@Mock
	private EntityAssetUpdater mockEntityAssetUpdater;
	@Mock
	private ItemEntityAssetsByQuantity mockQuantityMap;
	@Mock
	private ItemEntityAssetsByAssetType mockAssetTypeMap;
	@Mock
	private ItemEntityAssetsByItemType mockItemTypeMap;
	private ItemEntityAssetsBySize blankMap;
	@Mock
	private ItemEntityAttributesFactory mockItemEntityAttributesFactory;

	@Before
	public void setUp() throws Exception {
		itemTracker = new ItemTracker();

		Injector injector = Guice.createInjector(new UndermountGuiceModule());
		itemTypeDictionary = injector.getInstance(ItemTypeDictionary.class);
		gameMaterialDictionary = injector.getInstance(GameMaterialDictionary.class);
		blankMap = new ItemEntityAssetsBySize();

		when(mockItemEntityAssetDictionary.getQuantityMap()).thenReturn(mockQuantityMap);
		when(mockQuantityMap.getAssetTypeMapByQuantity(anyInt())).thenReturn(mockAssetTypeMap);
		when(mockAssetTypeMap.getItemTypeMapByAssetType(any())).thenReturn(mockItemTypeMap);
		when(mockItemTypeMap.getSizeMapByItemType(any())).thenReturn(blankMap);
	}

	@Test
	public void itemRemoved_cleansUpTree_whenItemRemoved() {
		Entity plankItem = buildItem("Resource-Planks", "Oak");

		itemTracker.itemAdded(plankItem);

		assertThat(itemTracker.getAllByItemType().values()).hasSize(1);

		itemTracker.itemRemoved(plankItem);

		assertThat(itemTracker.getAllByItemType().values()).isEmpty();
	}

	private Entity buildItem(String itemTypeName, String... materialNames) {
		ItemType itemType = itemTypeDictionary.getByName(itemTypeName);
		assertThat(itemType).isNotNull();

		GameMaterial[] materialArray = new GameMaterial[materialNames.length];
		for (int cursor = 0; cursor < materialNames.length; cursor++) {
			materialArray[cursor] = gameMaterialDictionary.getByName(materialNames[cursor]);
			assertThat(materialArray[cursor]).isNotNull();
		}

		ItemEntityAttributes itemAttributes = new ItemEntityAttributesFactory(mockItemEntityAssetDictionary, mockEntityAssetUpdater).createItemAttributes(itemType, 1, materialArray);
		return new ItemEntityFactory(mockItemEntityAttributesFactory, new MessageDispatcher(), gameMaterialDictionary, mockAssetUpdater).create(
				itemAttributes, new GridPoint2(), true, mockContext
		);
	}
}