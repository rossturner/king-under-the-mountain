package technology.rocketjump.undermount.entities.behaviour.items;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.components.humanoid.StatusComponent;
import technology.rocketjump.undermount.entities.components.humanoid.SteeringComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.LocationComponent;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RequestHaulingMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.rooms.components.StockpileComponent;

public class ItemBehaviour implements BehaviourComponent {

	private LocationComponent locationComponent;
	private MessageDispatcher messageDispatcher;
	private Entity parentEntity;
	private double lastUpdateGameTime;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
		this.locationComponent = parentEntity.getLocationComponent();
		this.messageDispatcher = messageDispatcher;
		this.parentEntity = parentEntity;
	}

	@Override
	public ItemBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		ItemBehaviour cloned = new ItemBehaviour();
		cloned.init(parentEntity, messageDispatcher, gameContext);
		return cloned;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		// Do nothing, does not update every frame
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		ItemEntityAttributes attributes = (ItemEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		ItemAllocationComponent itemAllocationComponent = parentEntity.getOrCreateComponent(ItemAllocationComponent.class);
		if (locationComponent.getWorldPosition() != null && attributes.getItemPlacement().equals(ItemPlacement.ON_GROUND) && itemAllocationComponent.getNumUnallocated() > 0) {
			// Has some unallocated on ground
			MapTile tile = gameContext.getAreaMap().getTile(locationComponent.getWorldPosition());
			boolean inStockpile = false;
			if (tile.getRoomTile() != null) {
				Room room = tile.getRoomTile().getRoom();
				StockpileComponent stockpileComponent = room.getComponent(StockpileComponent.class);
				if (stockpileComponent != null && stockpileComponent.canHold(attributes)) {
					inStockpile = true;
				}
			}

			if (!inStockpile) {
				// Not in a stockpile and some unallocated, so see if we can be hauled to a stockpile
				messageDispatcher.dispatchMessage(MessageType.REQUEST_ITEM_HAULING, new RequestHaulingMessage(parentEntity, parentEntity, false, JobPriority.NORMAL, null));
			}

		}

		double gameTime = gameContext.getGameClock().getCurrentGameTime();
		double elapsed = gameTime - lastUpdateGameTime;
		lastUpdateGameTime = gameTime;
		StatusComponent statusComponent = parentEntity.getComponent(StatusComponent.class);
		if (statusComponent != null) {
			statusComponent.infrequentUpdate(elapsed);
		}
	}

	@Override
	public SteeringComponent getSteeringComponent() {
		return null;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return false;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return true;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state to read
	}
}
