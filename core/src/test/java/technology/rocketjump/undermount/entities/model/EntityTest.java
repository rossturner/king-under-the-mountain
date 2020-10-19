package technology.rocketjump.undermount.entities.model;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.entities.behaviour.items.ItemBehaviour;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.model.physical.EntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.LocationComponent;
import technology.rocketjump.undermount.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HaulingComponent;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EntityTest {

	@Mock
	private PhysicalEntityComponent mockPhysicalComponent;
	@Mock
	private BehaviourComponent mockBehaviourComponent;
	@Mock
	private MessageDispatcher mockMessageDispatcher;
	@Mock
	private TiledMap mockMap;
	@Mock
	private GameClock mockGameClock;
	@Mock
	private EntityAttributes mockAttributes;
	@Mock
	private GameContext mockGameContext;

	@Test
	public void testYDepthEntityComparator() {
		LocationComponent backLocation = new LocationComponent();
		backLocation.setWorldPosition(new Vector2(0f, 10f), false);
		Entity back = new Entity(EntityType.HUMANOID, mockPhysicalComponent, mockBehaviourComponent, backLocation, mockMessageDispatcher, mockGameContext);

		LocationComponent middleLocation = new LocationComponent();
		middleLocation.setWorldPosition(new Vector2(0f, 5f), false);
		Entity middle = new Entity(EntityType.HUMANOID, mockPhysicalComponent, mockBehaviourComponent, middleLocation, mockMessageDispatcher, mockGameContext);

		LocationComponent frontLocation = new LocationComponent();
		frontLocation.setWorldPosition(new Vector2(0f, 1f), false);
		Entity front = new Entity(EntityType.HUMANOID, mockPhysicalComponent, mockBehaviourComponent, frontLocation, mockMessageDispatcher, mockGameContext);

		List<Entity> testData = Arrays.asList(middle, back, front);
		PriorityQueue<Entity> renderingSort = new PriorityQueue<>(new Entity.YDepthEntityComparator());
		renderingSort.addAll(testData);

		assertThat(renderingSort.poll().getLocationComponent().getWorldPosition().y).isEqualTo(10f);
		assertThat(renderingSort.poll().getLocationComponent().getWorldPosition().y).isEqualTo(5f);
		assertThat(renderingSort.poll().getLocationComponent().getWorldPosition().y).isEqualTo(1f);
		assertThat(renderingSort).isEmpty();
	}

	@Mock
	private GameMaterial mockMaterial;

	@Mock
	private Entity mockItem;
	@Mock
	private Entity clonedItem;
	@Mock
	private Entity parentEntity;

	@Test
	public void testClone() {
		when(mockGameContext.getAreaMap()).thenReturn(mockMap);
		LocationComponent originalLocation = new LocationComponent();
		originalLocation.setWorldPosition(new Vector2(0f, 1f), false);

		PhysicalEntityComponent originalPhysicalComponent = new PhysicalEntityComponent();
		ItemEntityAttributes originalAttributes = new ItemEntityAttributes(1L);
		when(mockMaterial.getMaterialType()).thenReturn(GameMaterialType.STONE);
		originalAttributes.setMaterial(mockMaterial);
		originalPhysicalComponent.setAttributes(originalAttributes);

		ItemBehaviour itemBehaviour = new ItemBehaviour();

		Entity original = new Entity(EntityType.ITEM, originalPhysicalComponent, itemBehaviour, originalLocation, mockMessageDispatcher, mockGameContext);
		HaulingComponent otherComponent = new HaulingComponent();
		when(mockItem.getPhysicalEntityComponent()).thenReturn(mockPhysicalComponent);
		when(mockItem.getLocationComponent()).thenReturn(new LocationComponent());
		when(mockPhysicalComponent.getAttributes()).thenReturn(mockAttributes);
		otherComponent.setHauledEntity(mockItem, mockMessageDispatcher, parentEntity);
		original.addComponent(otherComponent);
		original.getLocationComponent().setContainerEntity(parentEntity);

		Entity cloned = original.clone(mockMessageDispatcher, mockGameContext);

		original.getLocationComponent().setWorldPosition(new Vector2(1, 1), false);

		assertThat(cloned.getLocationComponent().getWorldPosition()).isNotEqualTo(original.getLocationComponent().getWorldPosition());
		assertThat(cloned.getLocationComponent().getContainerEntity()).isEqualTo(parentEntity);

		assertThat(((ItemEntityAttributes)cloned.getPhysicalEntityComponent().getAttributes()).getMaterial(GameMaterialType.STONE)).isEqualTo(mockMaterial);

		assertThat(cloned.getBehaviourComponent()).isInstanceOf(ItemBehaviour.class);

		assertThat(original.getId()).isNotEqualTo(cloned.getId());
	}

}