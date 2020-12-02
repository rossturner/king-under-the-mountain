package technology.rocketjump.undermount.entities;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.components.ItemAllocation;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.components.LiquidAllocation;
import technology.rocketjump.undermount.entities.components.LiquidContainerComponent;
import technology.rocketjump.undermount.entities.factories.ItemEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.jobs.JobTypeDictionary;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.jobs.model.JobState;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.ItemCreationRequestMessage;
import technology.rocketjump.undermount.messaging.types.LookupMessage;
import technology.rocketjump.undermount.messaging.types.RequestHaulingAllocationMessage;
import technology.rocketjump.undermount.messaging.types.RequestHaulingMessage;
import technology.rocketjump.undermount.rooms.HaulingAllocation;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.rooms.RoomStore;
import technology.rocketjump.undermount.rooms.StockpileAllocationResponse;
import technology.rocketjump.undermount.rooms.components.StockpileComponent;
import technology.rocketjump.undermount.settlement.ItemTracker;

import java.util.*;
import java.util.stream.Collectors;

import static technology.rocketjump.undermount.entities.behaviour.furniture.CraftingStationBehaviour.getAnyNavigableWorkspace;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.rooms.HaulingAllocation.AllocationPositionType.*;

@Singleton
public class ItemEntityMessageHandler implements GameContextAware, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final ItemEntityFactory itemEntityFactory;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final RoomStore roomStore;
	private final ItemTracker itemTracker;
	private GameContext gameContext;
	private JobType haulingJobType;
	private final ItemTypeDictionary itemTypeDictionary;

	@Inject
	public ItemEntityMessageHandler(MessageDispatcher messageDispatcher,
									ItemEntityFactory itemEntityFactory, GameMaterialDictionary gameMaterialDictionary,
									JobTypeDictionary jobTypeDictionary, RoomStore roomStore, ItemTracker itemTracker, ItemTypeDictionary itemTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.itemEntityFactory = itemEntityFactory;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.haulingJobType = jobTypeDictionary.getByName("HAULING");
		this.roomStore = roomStore;
		this.itemTracker = itemTracker;
		this.itemTypeDictionary = itemTypeDictionary;
		messageDispatcher.addListener(this, MessageType.ITEM_CREATION_REQUEST);
		messageDispatcher.addListener(this, MessageType.REQUEST_ITEM_HAULING);
		messageDispatcher.addListener(this, MessageType.REQUEST_HAULING_ALLOCATION);
		messageDispatcher.addListener(this, MessageType.LOOKUP_ITEM_TYPE);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.ITEM_CREATION_REQUEST: {
				return handle((ItemCreationRequestMessage)msg.extraInfo);
			}
			case MessageType.REQUEST_ITEM_HAULING: {
				return handle((RequestHaulingMessage)msg.extraInfo);
			}
			case MessageType.REQUEST_HAULING_ALLOCATION: {
				return handle((RequestHaulingAllocationMessage)msg.extraInfo);
			}
			case MessageType.LOOKUP_ITEM_TYPE: {
				return handle((LookupMessage)msg.extraInfo);
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private boolean handle(LookupMessage itemTypeLookupMessage) {
		if (itemTypeLookupMessage.entityType.equals(EntityType.ITEM)) {
			ItemType itemType = itemTypeDictionary.getByName(itemTypeLookupMessage.typeName);
			itemTypeLookupMessage.callback.itemTypeFound(Optional.ofNullable(itemType));
			return true;
		} else {
			return false;
		}
	}

	private boolean handle(RequestHaulingAllocationMessage message) {
		int requesterRegionId = gameContext.getAreaMap().getTile(message.requesterPosition).getRegionId();
		List<Entity> unallocatedItems;
		if (message.requiredMaterial != null) {
			unallocatedItems = itemTracker.getItemsByTypeAndMaterial(message.requiredItemType, message.requiredMaterial, true);
		} else {
			unallocatedItems = itemTracker.getItemsByType(message.requiredItemType, true);
		}

		Collections.sort(unallocatedItems, new NearestDistanceSorter(message.requesterPosition));

		HaulingAllocation allocation = new HaulingAllocation();
		for (Entity unallocatedItem : unallocatedItems) {
			MapTile itemTile = gameContext.getAreaMap().getTile(unallocatedItem.getLocationComponent().getWorldOrParentPosition());
			if (itemTile == null || itemTile.getRegionId() != requesterRegionId) {
				// Item not found or in different region
				continue;
			}

			LiquidContainerComponent liquidContainerComponent = unallocatedItem.getComponent(LiquidContainerComponent.class);
			if (liquidContainerComponent != null && liquidContainerComponent.getLiquidQuantity() > 0) {
				if (message.requiredContainedLiquid == null) {
					// Not requesting item to contain a specific liquid so don't use this one
					continue;
				} else if (!message.requiredContainedLiquid.equals(liquidContainerComponent.getTargetLiquidMaterial()) || liquidContainerComponent.getNumUnallocated() <= 0) {
					continue;
				}
			} else if (message.requiredContainedLiquid != null) {
				// No liquid container component or quantity and this request specifies a liquid
				continue;
			}

			Entity containerEntity = unallocatedItem.getLocationComponent().getContainerEntity();
			if (containerEntity != null) {
				if (message.includeFromFurniture) {
					if (containerEntity.getType().equals(EntityType.FURNITURE)) {
						allocation.setSourcePositionType(HaulingAllocation.AllocationPositionType.FURNITURE);
						allocation.setSourcePosition(itemTile.getTilePosition());
						allocation.setSourceContainerId(containerEntity.getId());
					} else {
						Logger.info("Not yet implemented: Requesting item from non-furniture container");
						continue;
					}
				} else {
					// This request does not want items from other containers
					continue;
				}
			} else {
				// Not in a container
				allocation.setSourcePositionType(HaulingAllocation.AllocationPositionType.FLOOR);
				allocation.setSourcePosition(itemTile.getTilePosition());
			}

			ItemEntityAttributes attributes = (ItemEntityAttributes) unallocatedItem.getPhysicalEntityComponent().getAttributes();
			allocation.setHauledEntityId(unallocatedItem.getId());

			ItemAllocationComponent itemAllocationComponent = unallocatedItem.getOrCreateComponent(ItemAllocationComponent.class);

			int numToAllocate = Math.min(itemAllocationComponent.getNumUnallocated(), attributes.getItemType().getMaxHauledAtOnce());
			if (message.maxAmountRequired != null) {
				numToAllocate = Math.min(numToAllocate, message.maxAmountRequired);
			}
			Entity requestingEntity = message.requestingEntity != null ? message.requestingEntity : unallocatedItem;
			ItemAllocation itemAllocation = itemAllocationComponent.createAllocation(numToAllocate, requestingEntity, ItemAllocation.Purpose.DUE_TO_BE_HAULED);
			if (itemAllocation != null) {
				allocation.setItemAllocation(itemAllocation);
				if (liquidContainerComponent != null && message.requiredContainedLiquid != null) {
					LiquidAllocation liquidAllocation = liquidContainerComponent.createAllocationDueToParentHauling(liquidContainerComponent.getNumUnallocated(), message.requestingEntity);
					allocation.setLiquidAllocation(liquidAllocation);
				}
				message.allocationCallback.allocationFound(allocation);
				return true;
			} else {
				Logger.error("Could not create item allocation");
			}
		}

		message.allocationCallback.allocationFound(null);
		return true;
	}

	public static HaulingAllocation findStockpileAllocation(TiledMap areaMap, Entity itemEntity, RoomStore roomStore, Entity requestingEntity) {
		// FIXME #89 - Stockpile priorityRank and filtering - select stockpile based on priorityRank and legality rather than only distance
		Vector2 itemPosition = itemEntity.getLocationComponent().getWorldOrParentPosition();
		ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
		ItemType itemType = attributes.getItemType();
		int sourceRegionId = areaMap.getTile(itemPosition).getRegionId();
		Map<Float, Room> stockpilesByDistance = new TreeMap<>(Comparator.comparingInt(o -> (int) (o * 10)));


		for (Room stockpile : roomStore.getByComponent(StockpileComponent.class)) {
			if (stockpile.getComponent(StockpileComponent.class).canHold(attributes)) {
				int roomRegionId = stockpile.getRoomTiles().values().iterator().next().getTile().getRegionId();
				if (sourceRegionId == roomRegionId) {
					stockpilesByDistance.put(itemPosition.dst2(stockpile.getAvgWorldPosition()), stockpile);
				}
			}
		}

		for (Room room : stockpilesByDistance.values()) {
			StockpileAllocationResponse stockpileAllocationResponse = room.getComponent(StockpileComponent.class).requestAllocation(itemEntity, areaMap);
			if (stockpileAllocationResponse != null) {
				HaulingAllocation allocation = new HaulingAllocation();
				allocation.setTargetPosition(stockpileAllocationResponse.position);
				allocation.setTargetPositionType(ROOM);
				allocation.setTargetId(room.getRoomId());

				ItemAllocation itemAllocation = itemEntity.getOrCreateComponent(ItemAllocationComponent.class).createAllocation(stockpileAllocationResponse.quantity,
						requestingEntity, ItemAllocation.Purpose.DUE_TO_BE_HAULED);
				allocation.setItemAllocation(itemAllocation);

				return allocation;
			}
		}
		return null;
	}

	public static Job createHaulingJob(HaulingAllocation haulingAllocation, Entity itemEntity, JobType haulingJobType, JobPriority jobPriority) {
		Job haulingJob = new Job(haulingJobType);
		haulingJob.setTargetId(itemEntity.getId());
		haulingJob.setJobLocation(toGridPoint(itemEntity.getLocationComponent().getWorldOrParentPosition()));
		haulingJob.setHaulingAllocation(haulingAllocation);
		haulingJob.setJobPriority(jobPriority);
		return haulingJob;
	}

	private boolean handle(RequestHaulingMessage message) {
		HaulingAllocation haulingAllocation = findStockpileAllocation(gameContext.getAreaMap(), message.getItemToBeMoved(), roomStore, message.requestingEntity);

		if (haulingAllocation == null && message.forceHaulingEvenWithoutStockpile()) {
			ItemEntityAttributes itemAttributes = (ItemEntityAttributes) message.getItemToBeMoved().getPhysicalEntityComponent().getAttributes();
			ItemAllocationComponent itemAllocationComponent = message.getItemToBeMoved().getOrCreateComponent(ItemAllocationComponent.class);

			int quantityToAllocate = Math.min(itemAllocationComponent.getNumUnallocated(), itemAttributes.getItemType().getMaxHauledAtOnce());
			if (quantityToAllocate == 0) {
				Logger.error(this.getClass().getSimpleName() + " handled with allocatable quantity of 0, investigate how or why this would happen");
				return true;
			}


			haulingAllocation = new HaulingAllocation();
			haulingAllocation.setSourcePositionType(FLOOR); // May be overridden below
			haulingAllocation.setTargetPosition(null);
			haulingAllocation.setHauledEntityId(message.getItemToBeMoved().getId());

			ItemAllocation itemAllocation = itemAllocationComponent.createAllocation(quantityToAllocate, message.getItemToBeMoved(), ItemAllocation.Purpose.DUE_TO_BE_HAULED);
			if (itemAllocation != null) {
				haulingAllocation.setItemAllocation(itemAllocation);
			}
		}

		if (haulingAllocation != null) {
			Job haulingJob = createHaulingJob(haulingAllocation, message.getItemToBeMoved(), haulingJobType, message.jobPriority);

			Entity itemToBeMoved = message.getItemToBeMoved();
			Entity containerEntity = itemToBeMoved.getLocationComponent().getContainerEntity();
			if (containerEntity != null) {
				if (!containerEntity.getType().equals(EntityType.FURNITURE)) {
					Logger.error("Not yet implemented: Hauling out of non-furniture container entity");
					messageDispatcher.dispatchMessage(MessageType.JOB_CANCELLED, haulingJob);
					message.callback.jobCreated(null);
					return true;
				}
				haulingAllocation.setSourcePositionType(FURNITURE);
				haulingAllocation.setSourcePosition(toGridPoint(containerEntity.getLocationComponent().getWorldPosition()));
				haulingAllocation.setSourceContainerId(containerEntity.getId());

				FurnitureLayout.Workspace navigableWorkspace = getAnyNavigableWorkspace(containerEntity, gameContext.getAreaMap());
				if (navigableWorkspace == null) {
					Logger.error("Item created but not accessible to collect - investigate and fix");
				} else {
					haulingJob.setJobLocation(navigableWorkspace.getAccessedFrom());
					haulingJob.setJobState(JobState.ASSIGNABLE);
				}
			}

			if (message.getSpecificProfessionRequired() != null) {
				haulingJob.setRequiredProfession(message.getSpecificProfessionRequired());
			}

			messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJob);
			if (message.callback != null) {
				message.callback.jobCreated(haulingJob);
			}
 		}

		return true;
	}

	private boolean handle(ItemCreationRequestMessage message) {
		ItemEntityAttributes attributes = message.getAttributes();
		if (attributes == null) {
			attributes = new ItemEntityAttributes(gameContext.getRandom().nextLong());
			attributes.setItemType(message.getRequiredItemType());

			for (GameMaterialType requiredMaterialType : message.getRequiredItemType().getMaterialTypes()) {
				List<GameMaterial> materialsToPickFrom = gameMaterialDictionary.getByType(requiredMaterialType).stream()
						.filter(GameMaterial::isUseInRandomGeneration)
						.collect(Collectors.toList());
				if (!materialsToPickFrom.isEmpty()) {
					GameMaterial material = materialsToPickFrom.get(gameContext.getRandom().nextInt(materialsToPickFrom.size()));
					attributes.setMaterial(material);
				}
			}
			attributes.setQuantity(1);
		}

		Entity item = itemEntityFactory.create(attributes, null, message.isAddToGameContext(), gameContext);
		message.getCallback().entityCreated(item);
		return true;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

	private static final Vector2 FAR_AWAY = new Vector2(-100f, -100f);
	public static class NearestDistanceSorter implements Comparator<Entity> {

		private final Vector2 requesterPosition;

		public NearestDistanceSorter(Vector2 requesterPosition) {
			this.requesterPosition = requesterPosition;
		}

		@Override
		public int compare(Entity o1, Entity o2) {
			Vector2 position1 = o1.getLocationComponent().getWorldOrParentPosition();
			if (position1 == null) {
				position1 = FAR_AWAY;
			}
			Vector2 position2 = o2.getLocationComponent().getWorldOrParentPosition();
			if (position2 == null) {
				position2 = FAR_AWAY;
			}

			return Math.round((position1.dst2(requesterPosition) - position2.dst2(requesterPosition)) * 10000f);
		}
	}
}
