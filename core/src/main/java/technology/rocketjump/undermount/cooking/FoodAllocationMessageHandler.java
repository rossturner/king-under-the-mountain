package technology.rocketjump.undermount.cooking;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.cooking.model.FoodAllocation;
import technology.rocketjump.undermount.entities.ai.goap.actions.CancelLiquidAllocationAction;
import technology.rocketjump.undermount.entities.components.*;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.FoodAllocationRequestMessage;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.settlement.ItemTracker;

import java.util.*;
import java.util.stream.Collectors;

import static technology.rocketjump.undermount.cooking.model.FoodAllocation.FoodAllocationType.*;
import static technology.rocketjump.undermount.entities.components.ItemAllocation.Purpose.FOOD_ALLOCATION;
import static technology.rocketjump.undermount.entities.components.ItemAllocation.Purpose.HELD_IN_INVENTORY;
import static technology.rocketjump.undermount.entities.model.EntityType.ITEM;
import static technology.rocketjump.undermount.settlement.ItemTracker.isItemEdible;

@Singleton
public class FoodAllocationMessageHandler implements Telegraph, GameContextAware {

	private static final float AMOUNT_LIQUID_FOOD_USED = 1f;
	private final ItemTracker itemTracker;
	private GameContext gameContext;

	@Inject
	public FoodAllocationMessageHandler(MessageDispatcher messageDispatcher, ItemTracker itemTracker) {
		this.itemTracker = itemTracker;

		messageDispatcher.addListener(this, MessageType.FOOD_ALLOCATION_REQUESTED);
		messageDispatcher.addListener(this, MessageType.FOOD_ALLOCATION_CANCELLED);
		messageDispatcher.addListener(this, MessageType.LIQUID_ALLOCATION_CANCELLED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.FOOD_ALLOCATION_REQUESTED: {
				handle((FoodAllocationRequestMessage) msg.extraInfo);
				return true;
			}
			case MessageType.FOOD_ALLOCATION_CANCELLED: {
				cancel((FoodAllocation)msg.extraInfo);
				return true;
			}
			case MessageType.LIQUID_ALLOCATION_CANCELLED: {
				cancel((LiquidAllocation)msg.extraInfo);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void cancel(FoodAllocation foodAllocation) {
		switch (foodAllocation.getType()) {
			case LIQUID_CONTAINER: {
				if (foodAllocation.getLiquidAllocation() != null) {
					this.cancel(foodAllocation.getLiquidAllocation());
				} else {
					Logger.error("Expected FoodAllocation of type LIQUID_CONTAINER to have a LiquidAllocation");
				}
				break;
			}
			case REQUESTER_INVENTORY: {
				// Do nothing
				break;
			}
			case FURNITURE_INVENTORY:
			case LOOSE_ITEM: {
				if (foodAllocation.getItemAllocaton() != null) {
					Entity itemEntity = foodAllocation.getTargetEntity();
					ItemAllocationComponent itemAllocationComponent = itemEntity.getOrCreateComponent(ItemAllocationComponent.class);
					itemAllocationComponent.cancel(foodAllocation.getItemAllocaton());
				} else {
					Logger.error("Food allocation cancelled with null itemAllocation");
				}
				break;
			}
			default:
				Logger.error("Not yet implemented: Cancelling food allocation of type " + foodAllocation.getType());
		}

	}


	private void cancel(LiquidAllocation liquidAllocation) {
		switch (liquidAllocation.getType()) {
			case FROM_RIVER: {
				// Nothing to do
				return;
			}
			case FROM_LIQUID_CONTAINER: {
				CancelLiquidAllocationAction.cancelLiquidAllocation(liquidAllocation, gameContext);
				break;
			}
			default:
				Logger.error("Not yet implemented: Cancelling liquid allocation of type " + liquidAllocation.getType());
		}

	}

	private void handle(FoodAllocationRequestMessage requestMessage) {
		FoodAllocation allocation = findAvailableFeastingHallFood(requestMessage.requestingEntity);
		if (allocation == null) {
			allocation = findAvailableRequesterInventoryFood(requestMessage.requestingEntity);
		}
		if (allocation == null) {
			allocation = findAnyAvailableFood(requestMessage.requestingEntity);
		}

		requestMessage.callback.foodAssigned(allocation);
	}

	private FoodAllocation findAvailableRequesterInventoryFood(Entity requester) {
		InventoryComponent inventoryComponent = requester.getComponent(InventoryComponent.class);
		if (inventoryComponent != null) {
			List<Entity> edibleEntities = inventoryComponent.getInventoryEntries().stream()
					.map(entry -> entry.entity)
					.filter(this::isEdible)
					.collect(Collectors.toList());

			if (!edibleEntities.isEmpty()) {
				Entity selected = edibleEntities.stream()
						.filter(e -> ((ItemEntityAttributes)e.getPhysicalEntityComponent().getAttributes()).getItemType().getItemTypeName().equals("Product-Ration"))
						.findAny().orElse(edibleEntities.get(0));
				ItemAllocationComponent itemAllocationComponent = selected.getOrCreateComponent(ItemAllocationComponent.class);

				ItemAllocation itemAllocation = itemAllocationComponent.getAllocationForPurpose(HELD_IN_INVENTORY);
				if (itemAllocation != null && itemAllocation.getAllocationAmount() > 0) {
					return new FoodAllocation(FoodAllocation.FoodAllocationType.REQUESTER_INVENTORY, selected, itemAllocation);
				}
			}
		}
		return null;
	}

	private boolean isEdible(Entity entity) {
		if (entity.getType().equals(ITEM)) {
			ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
			return isItemEdible(attributes);
		} else {
			return false;
		}
	}

	private FoodAllocation findAvailableFeastingHallFood(Entity requestingEntity) {
		Vector2 requesterPosition = requestingEntity.getLocationComponent().getWorldOrParentPosition();
		MapTile requesterTile = gameContext.getAreaMap().getTile(requesterPosition);
		if (requesterTile == null) {
			Logger.error("Requesting food from null tile");
			return null;
		}

		// FIXME should not be hard-coded to FEASTING_HALL room type

		List<Room> nearestFeastingHalls = gameContext.getRooms().values().stream()
				.filter(room -> room.getRoomType().getRoomName().equals("FEASTING_HALL"))
				.sorted((r1, r2) ->
						Math.round(r1.getAvgWorldPosition().dst2(requesterPosition) - r2.getAvgWorldPosition().dst2(requesterPosition))
				)
				.collect(Collectors.toList());

		FoodAllocation allocation = null;

		for (Room feastingHall : nearestFeastingHalls) {
			allocation = allocateFrom(feastingHall, requesterTile.getRegionId(), requestingEntity);
			if (allocation != null) {
				allocation.setPreparedMeal(true);
				break;
			}
		}
		return allocation;
	}

	private FoodAllocation allocateFrom(Room feastingHall, int requesterRegionId, Entity requestingEntity) {
		Set<Entity> furnitureEntities = new HashSet<>();
		for (GridPoint2 roomTileLocation : feastingHall.getRoomTiles().keySet()) {
			MapTile roomMapTile = gameContext.getAreaMap().getTile(roomTileLocation);
			if (roomMapTile.getRegionId() != requesterRegionId) {
				// Room is in a different region
				return null;
			}


			for (Entity entity : roomMapTile.getEntities()) {
				if (entity.getType().equals(EntityType.FURNITURE)) {
					furnitureEntities.add(entity);
				}
			}
		}

		List<Entity> shuffledEntities = new ArrayList<>(furnitureEntities);
		Collections.shuffle(shuffledEntities);

		for (Entity furnitureEntity : shuffledEntities) {
			// Check for edible liquid contents
			LiquidContainerComponent liquidContainerComponent = furnitureEntity.getComponent(LiquidContainerComponent.class);
			if (liquidContainerComponent != null && liquidContainerComponent.getTargetLiquidMaterial().isEdible() && liquidContainerComponent.getNumUnallocated() > 0) {
				// Can allocated from this
				LiquidAllocation liquidAllocation = liquidContainerComponent.createAllocation(AMOUNT_LIQUID_FOOD_USED, requestingEntity);
				if (liquidAllocation != null) {
					return new FoodAllocation(LIQUID_CONTAINER, furnitureEntity, liquidAllocation);
				}
			}

			InventoryComponent inventoryComponent = furnitureEntity.getComponent(InventoryComponent.class);
			if (inventoryComponent != null && !inventoryComponent.isEmpty()) {
				for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
					if (inventoryEntry.entity.getType().equals(ITEM)) {
						ItemAllocationComponent itemAllocationComponent = inventoryEntry.entity.getOrCreateComponent(ItemAllocationComponent.class);
						if (itemAllocationComponent.getNumUnallocated() > 0 && isEdible(inventoryEntry.entity)) {
							ItemAllocation itemAllocation = itemAllocationComponent.createAllocation(1, requestingEntity, ItemAllocation.Purpose.FOOD_ALLOCATION);
							if (itemAllocation != null) {
								return new FoodAllocation(FURNITURE_INVENTORY, inventoryEntry.entity, itemAllocation);
							}
						}
					}
				}

			}
		}

		return null;
	}

	private FoodAllocation findAnyAvailableFood(Entity requestingEntity) {
		Vector2 requesterPosition = requestingEntity.getLocationComponent().getWorldOrParentPosition();
		MapTile requesterTile = gameContext.getAreaMap().getTile(requesterPosition);
		if (requesterTile == null) {
			Logger.error("Requesting food from null tile");
			return null;
		}
		final int requesterRegionId = requesterTile.getRegionId();

		Optional<Entity> unallocatedEdibleItem = itemTracker.getUnallocatedEdibleItems().stream()
				.filter(item -> {
					Vector2 position = item.getLocationComponent().getWorldOrParentPosition();
					MapTile positionTile = gameContext.getAreaMap().getTile(position);
					return positionTile != null && positionTile.getRegionId() == requesterRegionId;
				})
				.sorted((i1, i2) ->
					Math.round(i1.getLocationComponent().getWorldOrParentPosition().dst2(requesterPosition) - i2.getLocationComponent().getWorldOrParentPosition().dst2(requesterPosition))
				)
				.findFirst();

		if (unallocatedEdibleItem.isPresent()) {
			Entity foodEntity = unallocatedEdibleItem.get();
			ItemAllocationComponent itemAllocationComponent = foodEntity.getOrCreateComponent(ItemAllocationComponent.class);
			ItemAllocation itemAllocation = itemAllocationComponent.createAllocation(1, requestingEntity, FOOD_ALLOCATION);
			return new FoodAllocation(LOOSE_ITEM, foodEntity, itemAllocation);
		} else {
			return null;
		}

	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
