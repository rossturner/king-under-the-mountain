package technology.rocketjump.undermount.entities.model.physical.humanoid;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.entities.components.ItemAllocation;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.EntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class HaulingComponent implements EntityComponent, Destructible {

	private Entity hauledEntity;

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (hauledEntity != null) {
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, hauledEntity);
		}
	}

	public Entity getHauledEntity() {
		return hauledEntity;
	}

	public void setHauledEntity(Entity hauledEntity, MessageDispatcher messageDispatcher, Entity parentEntity) {
		this.hauledEntity = hauledEntity;
		EntityAttributes attributes = hauledEntity.getPhysicalEntityComponent().getAttributes();
		if (attributes instanceof ItemEntityAttributes) {
			ItemEntityAttributes itemAttributes = (ItemEntityAttributes) attributes;
			itemAttributes.setItemPlacement(ItemPlacement.BEING_CARRIED);
			messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, hauledEntity);

			ItemAllocationComponent itemAllocationComponent = hauledEntity.getOrCreateComponent(ItemAllocationComponent.class);
			itemAllocationComponent.createAllocation(itemAttributes.getQuantity(), parentEntity, ItemAllocation.Purpose.HAULING);
		}
		hauledEntity.getLocationComponent().setWorldPosition(null, false);
		hauledEntity.getLocationComponent().setContainerEntity(parentEntity);
	}

	public void clearHauledEntity() {
		hauledEntity.getLocationComponent().setContainerEntity(null);
		if (hauledEntity.getType().equals(EntityType.ITEM)) {
			ItemAllocationComponent itemAllocationComponent = hauledEntity.getOrCreateComponent(ItemAllocationComponent.class);
			itemAllocationComponent.cancelAll(ItemAllocation.Purpose.HAULING);
		}
		hauledEntity = null;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		HaulingComponent clonedComponent = new HaulingComponent();
		if (hauledEntity != null) {
			Logger.warn("Cloning " + this.getClass().getSimpleName() + " but not cloning hauled entity");
		}
		return clonedComponent;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (hauledEntity != null) {
			hauledEntity.writeTo(savedGameStateHolder);
			asJson.put("entity", hauledEntity.getId());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		Long entityId = asJson.getLong("entity");
		if (entityId != null) {
			this.hauledEntity = savedGameStateHolder.entities.get(entityId);
			if (this.hauledEntity == null) {
				throw new InvalidSaveException("Could not find entity with ID " + entityId);
			}
		}
	}
}
