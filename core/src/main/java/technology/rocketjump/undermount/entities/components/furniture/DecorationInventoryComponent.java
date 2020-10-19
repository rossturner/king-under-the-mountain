package technology.rocketjump.undermount.entities.components.furniture;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.*;

import static technology.rocketjump.undermount.entities.model.EntityType.ITEM;

public class DecorationInventoryComponent implements ParentDependentEntityComponent {

	private Map<Long, Entity> decorationEntities = new LinkedHashMap<>();
	private Entity parentEntity;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		// Note that gameContext is passed in as null
		this.parentEntity = parentEntity;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		DecorationInventoryComponent cloned = new DecorationInventoryComponent();
		for (Entity decorationItem : decorationEntities.values()) {
			cloned.add(decorationItem.clone(messageDispatcher, gameContext));
		}
		return cloned;
	}

	public void clear() {
		for (Entity decorationEntity : decorationEntities.values()) {
			decorationEntity.getLocationComponent().setContainerEntity(null);
		}
		decorationEntities.clear();
		// Only expecting this to be used to lose any reference to decoration entities
	}

	public void add(Entity entity) {
		decorationEntities.put(entity.getId(), entity);
		entity.getLocationComponent().setWorldPosition(null, false, false);
		entity.getLocationComponent().setContainerEntity(parentEntity);
		entity.getLocationComponent().setOrientation(EntityAssetOrientation.DOWN);
	}

	public Collection<Entity> getDecorationEntities() {
		return decorationEntities.values();
	}

	public Entity findByItemType(ItemType itemType) {
		for (Entity item : decorationEntities.values()) {
			if (ITEM.equals(item.getType())) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) item.getPhysicalEntityComponent().getAttributes();
				if (itemType.equals(attributes.getItemType())) {
					return item;
				}
			}
		}
		return null;
	}

	public Entity findByItemTypeAndMaterial(ItemType itemType, GameMaterial material) {
		for (Entity item : decorationEntities.values()) {
			if (ITEM.equals(item.getType())) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) item.getPhysicalEntityComponent().getAttributes();
				if (itemType.equals(attributes.getItemType()) && material.equals(attributes.getMaterial(material.getMaterialType()))) {
					return item;
				}
			}
		}
		return null;
	}

	public Entity remove(long entityToRemoveId) {
		Entity removed = decorationEntities.remove(entityToRemoveId);
		if (removed == null) {
			return null;
		} else {
			removed.getLocationComponent().setContainerEntity(null);
			if (removed.getType().equals(ITEM)) {
				ItemAllocationComponent itemAllocationComponent = removed.getOrCreateComponent(ItemAllocationComponent.class);
				itemAllocationComponent.cancelAll();
			}
			return removed;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!decorationEntities.isEmpty()) {
			JSONArray decorationEntitiesJson = new JSONArray();
			for (Entity decorationItem : decorationEntities.values()) {
				decorationItem.writeTo(savedGameStateHolder);
				decorationEntitiesJson.add(decorationItem.getId());
			}
			asJson.put("decorations", decorationEntitiesJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONArray decorations = asJson.getJSONArray("decorations");
		if (decorations != null) {
			for (int cursor = 0; cursor < decorations.size(); cursor++) {
				Entity entity = savedGameStateHolder.entities.get(decorations.getLong(cursor));
				if (entity == null) {
					throw new InvalidSaveException("Could not find entity by ID " + decorations.getLong(cursor));
				} else {
					decorationEntities.put(entity.getId(), entity);
				}
			}
		}
	}
}
