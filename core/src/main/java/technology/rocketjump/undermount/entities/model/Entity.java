package technology.rocketjump.undermount.entities.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.undermount.entities.components.furniture.DecorationInventoryComponent;
import technology.rocketjump.undermount.entities.model.physical.AttachedEntity;
import technology.rocketjump.undermount.entities.model.physical.LocationComponent;
import technology.rocketjump.undermount.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.humanoid.EquippedItemComponent;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HaulingComponent;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemHoldPosition;
import technology.rocketjump.undermount.entities.tags.Tag;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.*;

import static technology.rocketjump.undermount.entities.model.EntityType.HUMANOID;
import static technology.rocketjump.undermount.entities.model.EntityType.ITEM;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;

public class Entity implements Persistable, Disposable {

	private long id;
	private EntityType type;

	private PhysicalEntityComponent physicalEntityComponent;
	private LocationComponent locationComponent;
	private BehaviourComponent behaviourComponent;

	private final EntityComponentMap componentMap = new EntityComponentMap();
	private Set<Tag> tags = new LinkedHashSet<>();

	public static final Entity NULL_ENTITY = new Entity();

	public Entity() {

	}

	public Entity(EntityType type, PhysicalEntityComponent physicalEntityComponent, BehaviourComponent behaviourComponent,
				  LocationComponent locationComponent, MessageDispatcher messageDispatcher, GameContext gameContext) {
		id = SequentialIdGenerator.nextId();
		this.type = type;
		this.physicalEntityComponent = physicalEntityComponent;
		this.locationComponent = locationComponent;

		componentMap.add(physicalEntityComponent);
		componentMap.add(locationComponent);

		if (gameContext.getAreaMap() != null && behaviourComponent != null) {
			this.behaviourComponent = behaviourComponent;
			behaviourComponent.init(this, messageDispatcher, gameContext);
			componentMap.add(behaviourComponent);
		} else {
			this.behaviourComponent = null;
		}

		locationComponent.init(this, messageDispatcher, gameContext);
	}

	public Entity(long id, EntityType type, List<EntityComponent> components) {
		this.id = id;
		this.type = type;
		for (EntityComponent component : components) {
			if (component instanceof PhysicalEntityComponent) {
				physicalEntityComponent = (PhysicalEntityComponent) component;
			} else if (component instanceof LocationComponent) {
				locationComponent = (LocationComponent) component;
			} else if (component instanceof  BehaviourComponent) {
				behaviourComponent = (BehaviourComponent) component;
			}
			componentMap.add(component);
		}
	}

	public void init(MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (gameContext.getAreaMap() == null) {
			return;
		}
		for (EntityComponent component : componentMap.values()) {
			if (component instanceof ParentDependentEntityComponent) {
				((ParentDependentEntityComponent)component).init(this, messageDispatcher, gameContext);
			}
		}
	}

	public void destroy(MessageDispatcher messageDispatcher, GameContext gameContext) {
		for (Class componentClass : new HashSet<>(componentMap.keySet())) {
			EntityComponent entityComponent = componentMap.get(componentClass);
			if (entityComponent instanceof Destructible) {
				((Destructible) entityComponent).destroy(this, messageDispatcher, gameContext);
			}
		}
	}

	@Override
	public void dispose() {
		for (Class componentClass : new HashSet<>(componentMap.keySet())) {
			EntityComponent entityComponent = componentMap.get(componentClass);
			if (entityComponent instanceof Disposable) {
				((Disposable) entityComponent).dispose();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public Entity clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		Entity cloned = new Entity(type, physicalEntityComponent.clone(messageDispatcher, gameContext),
				behaviourComponent == null ? null : (BehaviourComponent) behaviourComponent.clone(messageDispatcher, gameContext),
				locationComponent.clone(messageDispatcher, gameContext),
				messageDispatcher, gameContext);

		for (Class componentClass : this.componentMap.keySet()) {
			EntityComponent clonedComponent = cloned.componentMap.get(componentClass);
			if (clonedComponent == null) {
				EntityComponent thisComponent = this.componentMap.get(componentClass);
				clonedComponent = thisComponent.clone(messageDispatcher, gameContext);
				cloned.componentMap.add(clonedComponent);
				if (clonedComponent instanceof BehaviourComponent) {
					cloned.replaceBehaviourComponent((BehaviourComponent)clonedComponent);
				}
			}
			if (clonedComponent instanceof ParentDependentEntityComponent) {
				((ParentDependentEntityComponent)clonedComponent).init(cloned, messageDispatcher, gameContext);
			}
		}

		return cloned;
	}
	private List<AttachedEntity> attachedEntities = new ArrayList<>();

	private Map<ItemHoldPosition, AttachedEntity> workspaceItems = new HashMap<>();

	public List<AttachedEntity> getAttachedEntities() {
		attachedEntities.clear(); // Avoiding new instance on each call, is this a good idea or bad idea?

		if (type.equals(EntityType.HUMANOID)) {
			HaulingComponent haulingComponent = getComponent(HaulingComponent.class);
			if (haulingComponent != null && haulingComponent.getHauledEntity() != null) {
				Entity hauledEntity = haulingComponent.getHauledEntity();
				if (hauledEntity.getType().equals(ITEM)) {
					ItemEntityAttributes attributes = (ItemEntityAttributes) hauledEntity.getPhysicalEntityComponent().getAttributes();
					attachedEntities.add(new AttachedEntity(hauledEntity, attributes.getItemType().getHoldPosition()));
				} else if (hauledEntity.getType().equals(HUMANOID)) {
					attachedEntities.add(new AttachedEntity(hauledEntity, ItemHoldPosition.IN_FRONT));
				}
			} else {
				// Only check for equipped items if not hauling
				EquippedItemComponent equippedItemComponent = getComponent(EquippedItemComponent.class);
				if (equippedItemComponent != null) {
					Entity equippedItem = equippedItemComponent.getEquippedItem();
					if (equippedItem != null) {
						ItemEntityAttributes attributes = (ItemEntityAttributes) equippedItem.getPhysicalEntityComponent().getAttributes();
						attachedEntities.add(new AttachedEntity(equippedItem, attributes.getItemType().getHoldPosition()));
					}
				}
			}
		} else if (type.equals(EntityType.FURNITURE)) {
			DecorationInventoryComponent decorationInventoryComponent = getComponent(DecorationInventoryComponent.class);
			if (decorationInventoryComponent != null) {
				List<Entity> decorationItems = new ArrayList<>(decorationInventoryComponent.getDecorationEntities());
				if (decorationItems.size() >= 1) {
					attachedEntities.add(new AttachedEntity(decorationItems.get(0), ItemHoldPosition.DECORATION_1));
				}
				if (decorationItems.size() >= 2) {
					attachedEntities.add(new AttachedEntity(decorationItems.get(1), ItemHoldPosition.DECORATION_2));
				}
				if (decorationItems.size() >= 3) {
					attachedEntities.add(new AttachedEntity(decorationItems.get(2), ItemHoldPosition.DECORATION_3));
				}
			}

			InventoryComponent inventoryComponent = getComponent(InventoryComponent.class);
			if (inventoryComponent != null) {
				workspaceItems.clear();



				for (InventoryComponent.InventoryEntry inventoryItem : inventoryComponent.getInventoryEntries()) {
					if (inventoryItem.getPreferredPosition() != null) {
						workspaceItems.put(inventoryItem.getPreferredPosition(), new AttachedEntity(inventoryItem.entity, inventoryItem.getPreferredPosition()));
					} else {
						// Find free workspace
						for (ItemHoldPosition workspaceHoldPosition : ItemHoldPosition.WORKSPACES) {
							if (!workspaceItems.containsKey(workspaceHoldPosition)) {
								workspaceItems.put(workspaceHoldPosition, new AttachedEntity(inventoryItem.entity, workspaceHoldPosition));
								break;
							}
						}
						// Else no space to add/display item
					}
					attachedEntities.addAll(workspaceItems.values());
				}
			}
		}

		return attachedEntities;
	}

	public void update(float deltaTime, GameContext gameContext) {
		behaviourComponent.update(deltaTime, gameContext);
    }

	public void infrequentUpdate(GameContext gameContext) {
		behaviourComponent.infrequentUpdate(gameContext);
	}

	public boolean isUpdateEveryFrame() {
		return behaviourComponent.isUpdateEveryFrame();
	}

	public boolean isUpdateInfrequently() {
		return behaviourComponent.isUpdateInfrequently();
	}

    public long getId() {
        return id;
    }

    public PhysicalEntityComponent getPhysicalEntityComponent() {
        return physicalEntityComponent;
    }

	public LocationComponent getLocationComponent() {
		return locationComponent;
	}

	public BehaviourComponent getBehaviourComponent() {
		return behaviourComponent;
	}

	public void replaceBehaviourComponent(BehaviourComponent behaviourComponent) {
		if (this.behaviourComponent != null) {
			componentMap.remove(this.behaviourComponent.getClass());
		}
		this.behaviourComponent = behaviourComponent;
		if (this.behaviourComponent != null) {
			componentMap.add(this.behaviourComponent);
		}
	}

	public EntityType getType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Entity entity = (Entity) o;
		return id == entity.id;
	}

	@Override
	public int hashCode() {
		return (int)id;
	}

	public boolean isJobAssignable() {
		return behaviourComponent.isJobAssignable();
	}

	public <T extends EntityComponent> T getComponent(Class<T> classType) {
		return componentMap.get(classType);
	}

	public <T extends EntityComponent> T getOrCreateComponent(Class<T> classType) {
		T component = componentMap.get(classType);
		if (classType.getSimpleName().contains("ItemAllocation") && !this.type.equals(ITEM)) {
			// TEMP remove me
			Logger.error("Creating invalid component");
		}
		if (component == null) {
			try {
				component = classType.getDeclaredConstructor().newInstance();
				componentMap.add(component);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}
		return component;
	}

	public void addComponent(EntityComponent component) {
		componentMap.add(component);
	}

	public void removeComponent(Class<? extends EntityComponent> componentClass) {
		componentMap.remove(componentClass);
	}

	private static final List<GridPoint2> EMPTY_LIST = new LinkedList<>();

	public List<GridPoint2> calculateOtherTilePositions() {
		return calculateOtherTilePositions(locationComponent.getWorldPosition());
	}

	public List<GridPoint2> calculateOtherTilePositions(Vector2 worldPosition) {
		if (type.equals(EntityType.FURNITURE)) {
			List<GridPoint2> otherPositions = new LinkedList<>();
			FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) physicalEntityComponent.getAttributes();
			if (attributes.getCurrentLayout() != null) {
				GridPoint2 primaryTilePosition = toGridPoint(worldPosition);
				for (GridPoint2 extraTileOffset : attributes.getCurrentLayout().getExtraTiles()) {
					otherPositions.add(primaryTilePosition.cpy().add(extraTileOffset));
				}
			}
			return otherPositions;
		} else {
			return EMPTY_LIST;
		}
	}

	public Set<Tag> getTags() {
		return tags;
	}

	public void setTags(Set<Tag> tags) {
		this.tags = tags;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Entity{");
		sb.append("id=").append(id);
		sb.append(", type=").append(type);
		sb.append(", physical=").append(physicalEntityComponent);
		sb.append('}');
		return sb.toString();
	}

	public Collection<EntityComponent> allComponents() {
		return componentMap.values();
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.entities.containsKey(id)) {
			return;
		}

		JSONObject asJson = new JSONObject(true);
		asJson.put("id", id);
		asJson.put("type", type.name());

		JSONArray componentsJson = new JSONArray();
		for (EntityComponent component : componentMap.values()) {
			JSONObject componentJson = new JSONObject(true);
			componentJson.put("_class", component.getClass().getSimpleName());
			component.writeTo(componentJson, savedGameStateHolder);
			componentsJson.add(componentJson);
		}
		asJson.put("components", componentsJson);

		if (!tags.isEmpty()) {
			JSONObject tagsJson = new JSONObject(true);
			for (Tag tag : tags) {
				tagsJson.put(tag.getTagName(), tag.getArgs());
			}
			asJson.put("tags", tagsJson);
		}

		savedGameStateHolder.entitiesJson.add(asJson);
		savedGameStateHolder.entities.put(id, this);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.id = asJson.getLongValue("id");
		this.type = EnumParser.getEnumValue(asJson, "type", EntityType.class, null);
		savedGameStateHolder.entities.put(id, this);

		JSONArray componentsJson = asJson.getJSONArray("components");
		for (int cursor = 0; cursor < componentsJson.size(); cursor++) {
			JSONObject componentJson = componentsJson.getJSONObject(cursor);

			Class<? extends EntityComponent> componentClass = relatedStores.componentDictionary.getByName(componentJson.getString("_class"));
			try {
				EntityComponent entityComponent = componentClass.getDeclaredConstructor().newInstance();
				entityComponent.readFrom(componentJson, savedGameStateHolder, relatedStores);
				componentMap.add(entityComponent);
				if (entityComponent instanceof BehaviourComponent) {
					this.behaviourComponent = (BehaviourComponent) entityComponent;
				} else if (entityComponent instanceof LocationComponent) {
					this.locationComponent = (LocationComponent) entityComponent;
				} else if (entityComponent instanceof PhysicalEntityComponent) {
					this.physicalEntityComponent = (PhysicalEntityComponent) entityComponent;
				}
			} catch (ReflectiveOperationException e) {
				throw new InvalidSaveException("Failed to instantiate component " + componentClass.getSimpleName() + "\n" + e.getMessage());
			}
		}

		// Tags are loaded but not re-applied
		JSONObject tagsJson = asJson.getJSONObject("tags");
		if (tagsJson != null) {
			for (Map.Entry<String, Object> tagEntry : tagsJson.entrySet()) {
				String tagName = tagEntry.getKey();
				JSONArray tagArgsJson = (JSONArray) tagEntry.getValue();

				Tag tag = relatedStores.tagDictionary.newInstanceByName(tagName);
				if (tag == null) {
					throw new InvalidSaveException("Could not create tag with name " + tagName);
				} else {
					List<String> tagArgs = new ArrayList<>();
					for (int cursor = 0; cursor < tagArgsJson.size(); cursor++) {
						tagArgs.add(tagArgsJson.getString(cursor));
					}
					tag.setArgs(tagArgs);
				}

				this.tags.add(tag);
			}
		}

	}

	public <T> T getTag(Class<T> tagClass) {
		for (Tag tag : tags) {
			if (tagClass.isInstance(tag)) {
				return (T) tag;
			}
		}
		return null;
	}

	/**
	 * This method is intended for temporary overriding of LocationComponent for rendering an entity as part of the UI
	 */
	public void setLocationComponent(LocationComponent overrideLocationComponent) {
		this.locationComponent = overrideLocationComponent;
	}

}
