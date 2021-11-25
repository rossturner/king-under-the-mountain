package technology.rocketjump.undermount.jobs;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.ItemEntityMessageHandler;
import technology.rocketjump.undermount.entities.components.ItemAllocation;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.components.LiquidAllocation;
import technology.rocketjump.undermount.entities.components.LiquidContainerComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RequestHaulingAllocationMessage;
import technology.rocketjump.undermount.messaging.types.RequestLiquidAllocationMessage;
import technology.rocketjump.undermount.messaging.types.RequestLiquidRemovalMessage;
import technology.rocketjump.undermount.messaging.types.RequestLiquidTransferMessage;
import technology.rocketjump.undermount.rooms.HaulingAllocation;
import technology.rocketjump.undermount.settlement.ItemTracker;
import technology.rocketjump.undermount.zones.Zone;
import technology.rocketjump.undermount.zones.ZoneTile;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static technology.rocketjump.undermount.entities.behaviour.furniture.CraftingStationBehaviour.getAnyNavigableWorkspace;
import static technology.rocketjump.undermount.entities.components.ItemAllocation.Purpose.CONTENTS_TO_BE_DUMPED;
import static technology.rocketjump.undermount.entities.components.ItemAllocation.Purpose.DUE_TO_BE_HAULED;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.rooms.HaulingAllocation.AllocationPositionType.*;
import static technology.rocketjump.undermount.zones.ZoneClassification.ZoneType.LIQUID_SOURCE;

@Singleton
public class LiquidMessageHandler implements GameContextAware, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final GameMaterialDictionary gameMaterialDictionary;
	private GameContext gameContext;
	private final JobType transferLiquidJobType;
	private final JobType moveLiquidInItemJobType;
	private final JobType removeLiquidJobType;
	private final JobType dumpLiquidJobType;
	private final ItemTracker itemTracker;

	@Inject
	public LiquidMessageHandler(MessageDispatcher messageDispatcher, GameMaterialDictionary gameMaterialDictionary,
	                            JobTypeDictionary jobTypeDictionary, ItemTracker itemTracker) {
		this.messageDispatcher = messageDispatcher;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.transferLiquidJobType = jobTypeDictionary.getByName("TRANSFER_LIQUID");
		this.moveLiquidInItemJobType = jobTypeDictionary.getByName("MOVE_LIQUID_IN_ITEM");
		this.removeLiquidJobType = jobTypeDictionary.getByName("REMOVE_LIQUID");
		this.dumpLiquidJobType = jobTypeDictionary.getByName("DUMP_LIQUID_FROM_CONTAINER");
		this.itemTracker = itemTracker;
		messageDispatcher.addListener(this, MessageType.REQUEST_LIQUID_TRANSFER);
		messageDispatcher.addListener(this, MessageType.REQUEST_LIQUID_ALLOCATION);
		messageDispatcher.addListener(this, MessageType.REQUEST_LIQUID_REMOVAL);
		messageDispatcher.addListener(this, MessageType.REQUEST_DUMP_LIQUID_CONTENTS);
	}


	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.REQUEST_LIQUID_TRANSFER: {
				return handle((RequestLiquidTransferMessage) msg.extraInfo);
			}
			case MessageType.REQUEST_LIQUID_ALLOCATION: {
				return handle((RequestLiquidAllocationMessage)msg.extraInfo);
			}
			case MessageType.REQUEST_LIQUID_REMOVAL: {
				return handle((RequestLiquidRemovalMessage)msg.extraInfo);
			}
			case MessageType.REQUEST_DUMP_LIQUID_CONTENTS: {
				Entity entity = (Entity) msg.extraInfo;
				requestDumpLiquidContents(entity);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private boolean handle(RequestLiquidTransferMessage message) {
		// First try to find an unassigned item containing the liquid already
		Job moveLiquidInItemJob = createMoveItemInLiquidJob(message);
		if (moveLiquidInItemJob != null) {
			message.jobCreatedCallback.jobCreated(moveLiquidInItemJob);
			return true;
		}

		// TODO This does not currently respect liquid quantities as transferring is currently always from an infinite source i.e. river
		MapTile requesterTile = gameContext.getAreaMap().getTile(message.requesterPosition);
		if (requesterTile == null) {
			Logger.error(this.getClass().getSimpleName() + " received request for off-map tile");
			message.jobCreatedCallback.jobCreated(null);
			return false;
		}

		int amountRequired = 1;
		List<String> liquidContainerArgs = message.liquidContainerItemType.getTags().get("LIQUID_CONTAINER");
		if (liquidContainerArgs != null) {
			try {
				amountRequired = Integer.valueOf(liquidContainerArgs.get(0));
			} catch (NumberFormatException e) {
				Logger.error("Could not parse amount from first argument of " + message.liquidContainerItemType.getItemTypeName() + " LIQUID_CONTAINER tag");
			}
		}
		final int finalAmountRequired = amountRequired;

		List<GameMaterial> applicableMaterials = new ArrayList<>(1);
		applicableMaterials.add(message.targetLiquidMaterial);

		// Pick source zone for filling
		final Optional<Zone> nearestApplicableZone = findNearestZonesOfCorrectType(gameContext,
				applicableMaterials, requesterTile.getRegionId(), message.requesterPosition, message.useSmallCapacityZones, amountRequired).findFirst();


		Job transferLiquidJob = new Job(transferLiquidJobType);
		if (nearestApplicableZone.isPresent()) {
			ZoneTile zoneTile = pickTileInZone(nearestApplicableZone.get(), gameContext.getRandom(), gameContext.getAreaMap());
			if (zoneTile != null) {
				MapTile accessTile = gameContext.getAreaMap().getTile(zoneTile.getAccessLocation());

				// Try to assign bucket for filling
				messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION, new RequestHaulingAllocationMessage(
						message.requesterEntity,
						message.requesterEntity.getLocationComponent().getWorldOrParentPosition(), message.liquidContainerItemType,
						null, // any material
						true, null, null, (allocation) -> {
							if (allocation != null) {
								allocation.setTargetPositionType(HaulingAllocation.AllocationPositionType.ZONE);
								allocation.setTargetPosition(accessTile.getTilePosition());

								transferLiquidJob.setJobPriority(message.jobPriority);
								transferLiquidJob.setJobLocation(requesterTile.getTilePosition());
								transferLiquidJob.setTargetId(message.requesterEntity.getId());
								transferLiquidJob.setHaulingAllocation(allocation);

								if (nearestApplicableZone.get().getClassification().isConstructed()) {
									LiquidContainerComponent furnitureLiquidContainer = getLiquidContainerFromFurnitureInTile(gameContext.getAreaMap().getTile(zoneTile.getTargetTile()));
									if (furnitureLiquidContainer != null) {
										transferLiquidJob.setLiquidAllocation(furnitureLiquidContainer.createAllocation(finalAmountRequired, message.requesterEntity));
									}
								} else {
									transferLiquidJob.setLiquidAllocation(LiquidAllocation.fromRiver(zoneTile, gameContext.getAreaMap()));
								}

							} // else could not find a suitable container
						}
				));
			}
		}

		if (transferLiquidJob.getJobLocation() == null) {
			message.jobCreatedCallback.jobCreated(null);
		} else {
			messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, transferLiquidJob);
			message.jobCreatedCallback.jobCreated(transferLiquidJob);
		}
		return true;
	}

	private Job createMoveItemInLiquidJob(RequestLiquidTransferMessage message) {
		List<Entity> unallocatedItems = itemTracker.getItemsByType(message.liquidContainerItemType, true);
		unallocatedItems.sort(new ItemEntityMessageHandler.NearestDistanceSorter(message.requesterPosition));

		for (Entity itemEntity : unallocatedItems) {
			LiquidContainerComponent liquidContainerComponent = itemEntity.getComponent(LiquidContainerComponent.class);
			ItemAllocationComponent itemAllocationComponent = itemEntity.getComponent(ItemAllocationComponent.class);
			if (liquidContainerComponent != null &&
					message.targetLiquidMaterial.equals(liquidContainerComponent.getTargetLiquidMaterial()) &&
					liquidContainerComponent.getLiquidQuantity() > 0) {
				// Use this unallocated item already containing liquid


				Job moveLiquidInItemJob = new Job(moveLiquidInItemJobType);
				moveLiquidInItemJob.setJobPriority(message.jobPriority);
				moveLiquidInItemJob.setTargetId(message.requesterEntity.getId());
				moveLiquidInItemJob.setJobLocation(toGridPoint(message.requesterPosition));

				HaulingAllocation haulingAllocation = new HaulingAllocation();
				haulingAllocation.setSourcePositionType(FLOOR); // May be overridden below
				haulingAllocation.setSourcePosition(toGridPoint(itemEntity.getLocationComponent().getWorldPosition()));
				haulingAllocation.setHauledEntityId(itemEntity.getId());

				Entity containerEntity = itemEntity.getLocationComponent().getContainerEntity();
				if (containerEntity != null) {
					if (!containerEntity.getType().equals(EntityType.FURNITURE)) {
						Logger.error("Not yet implemented: Hauling out of non-furniture container entity");
						continue;
					}
					haulingAllocation.setSourcePositionType(FURNITURE);
					haulingAllocation.setSourcePosition(toGridPoint(containerEntity.getLocationComponent().getWorldPosition()));
					haulingAllocation.setSourceContainerId(containerEntity.getId());

					FurnitureLayout.Workspace navigableWorkspace = getAnyNavigableWorkspace(containerEntity, gameContext.getAreaMap());
					if (navigableWorkspace == null) {
						Logger.error("Item not accessible to collect - investigate and fix");
						continue;
					} else {
						haulingAllocation.setSourcePosition(navigableWorkspace.getAccessedFrom());
					}
				}

				ItemAllocation itemAllocation = itemAllocationComponent.createAllocation(itemAllocationComponent.getNumUnallocated(),
						message.requesterEntity, DUE_TO_BE_HAULED);
				haulingAllocation.setItemAllocation(itemAllocation);

				haulingAllocation.setTargetPosition(toGridPoint(message.requesterPosition));

				moveLiquidInItemJob.setHaulingAllocation(haulingAllocation);

				messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, moveLiquidInItemJob);
				return moveLiquidInItemJob;
			}
		}

		return null;
	}

	private boolean handle(RequestLiquidAllocationMessage message) {
		Vector2 requesterPosition = message.requestingEntity.getLocationComponent().getWorldPosition();
		MapTile requesterTile = gameContext.getAreaMap().getTile(requesterPosition);
		if (requesterTile == null) {
			Logger.error(this.getClass().getSimpleName() + " received request for off-map tile");
			return false;
		}

		Collection<GameMaterial> applicableMaterials = gameMaterialDictionary.getThirstQuenchingMaterials();

		applicableMaterials = applicableMaterials.stream()
				.filter(material -> material.isAlcoholic() == message.isAlcoholic)
				.collect(Collectors.toList());

		List<Zone> nearestApplicableZones = findNearestZonesOfCorrectType(gameContext, applicableMaterials, requesterTile.getRegionId(), requesterPosition, true, message.amountRequired)
				.collect(Collectors.toList());

		Optional<LiquidAllocation> foundAllocation = Optional.empty();
		for (Zone zone : nearestApplicableZones) {
			ZoneTile zoneTile = pickTileInZone(zone, gameContext.getRandom(), gameContext.getAreaMap());
			if (zoneTile != null) {
				MapTile targetTile = gameContext.getAreaMap().getTile(zoneTile.getTargetTile());

				if (targetTile.getFloor() != null && targetTile.getFloor().isRiverTile()) {
					foundAllocation = Optional.of(LiquidAllocation.fromRiver(zoneTile, gameContext.getAreaMap()));
					break;
				} else {
					LiquidContainerComponent liquidContainerComponent = getLiquidContainerFromFurnitureInTile(targetTile);
					if (liquidContainerComponent != null) {
						LiquidAllocation allocation = liquidContainerComponent.createAllocation(message.amountRequired, message.requestingEntity);
						if (allocation != null) {
							foundAllocation = Optional.of(allocation);
							break;
						}
					} else {
						Logger.error("Not yet implemented - derivation of drinking allocation type");
					}
				}
			}
		}

		message.callback.allocationFound(foundAllocation);
		return true;
	}

	private void requestDumpLiquidContents(Entity entity) {
		if (entity.getType().equals(EntityType.ITEM)) {
			if (entity.getLocationComponent().getContainerEntity() != null) {
				Logger.error("Not yet implemented - dumping liquid contents from item within a container");
				return;
			}

			ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();

			ItemAllocationComponent itemAllocationComponent = entity.getOrCreateComponent(ItemAllocationComponent.class);
			ItemAllocation allocation = itemAllocationComponent.createAllocation(attributes.getQuantity(), entity, CONTENTS_TO_BE_DUMPED);

			if (allocation != null) {
				GridPoint2 tileLocation = toGridPoint(entity.getLocationComponent().getWorldPosition());

				HaulingAllocation haulingAllocation = new HaulingAllocation();
				haulingAllocation.setSourcePosition(tileLocation);
				haulingAllocation.setSourcePositionType(FLOOR);

				haulingAllocation.setItemAllocation(allocation);
				haulingAllocation.setHauledEntityType(EntityType.ITEM);
				haulingAllocation.setTargetId(entity.getId());
				haulingAllocation.setHauledEntityId(entity.getId());

				haulingAllocation.setTargetPosition(tileLocation);
				haulingAllocation.setTargetPositionType(FLOOR);

				Job job = new Job(dumpLiquidJobType);
				job.setJobPriority(JobPriority.NORMAL);
				job.setJobLocation(tileLocation);
				job.setTargetId(entity.getId());
				job.setHaulingAllocation(haulingAllocation);

				messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, job);
			} else {
				Logger.error("Could not create item allocation to dump liquid contents");
			}
		} else {
			Logger.error("Not yet implemented - dumping liquid contents from entity other than item");
		}
	}

	private Stream<Zone> findNearestZonesOfCorrectType(GameContext gameContext, Collection<GameMaterial> targetMaterials,
	                                                   int regionId, Vector2 requesterPosition, boolean useSmallCapacityZones, float amountRequired) {
		return gameContext.getAreaMap().getZonesInRegion(regionId).stream()
				.filter(zone -> zone.isActive() && LIQUID_SOURCE.equals(zone.getClassification().getZoneType()) &&
						targetMaterials.contains(zone.getClassification().getTargetMaterial()))
				.filter(zone -> {
					if (useSmallCapacityZones) {
						return true;
					} else {
						// this could just be return zone.getClassification().isHighCapacity() - || !isConstructed was to handle save compatibility between A7.0.2 and A7.03
						return zone.getClassification().isHighCapacity() || !zone.getClassification().isConstructed();
					}
				})
				.filter(zone -> {
					if (zone.getClassification().isConstructed()) {
						// Check there is enough to requestAllocation
						if (zone.isEmpty()) {
							return false;
						}
						MapTile targetTile = gameContext.getAreaMap().getTile(zone.iterator().next().getTargetTile());
						if (targetTile == null) {
							return false;
						} else {
							LiquidContainerComponent liquidContainerComponent = getLiquidContainerFromFurnitureInTile(targetTile);
							if (liquidContainerComponent == null) {
								return false;
							} else {
								return liquidContainerComponent.getNumUnallocated() >= amountRequired;
							}
						}
					} else {
						return true; // Include all non-constructed
					}
				})
				.sorted(
						(o1, o2) -> {
							// Using proper square-root calculated distance as dst2 seems to not be a good pick
							float distanceTo1 = o1.getAvgWorldPosition().dst(requesterPosition) * 1000;
							float distanceTo2 = o2.getAvgWorldPosition().dst(requesterPosition) * 1000;
							return Math.round(distanceTo1 - distanceTo2);
						}
				);
	}

	public static ZoneTile pickTileInZone(Zone zone, Random random, TiledMap areaMap) {
		List<ZoneTile> potentialTiles = new ArrayList<>();
		for (Iterator<ZoneTile> iter = zone.iterator(); iter.hasNext(); ) {
			ZoneTile zoneTile = iter.next();
			MapTile accessibleTile = areaMap.getTile(zoneTile.getAccessLocation());
			if (accessibleTile != null && accessibleTile.isNavigable()) {
				potentialTiles.add(zoneTile);
			}
		}

		if (potentialTiles.isEmpty()) {
			return null;
		} else {
			Collections.shuffle(potentialTiles, random);
			return potentialTiles.get(random.nextInt(potentialTiles.size()));
		}
	}

	public static LiquidContainerComponent getLiquidContainerFromFurnitureInTile(MapTile targetTile) {
		if (targetTile == null) {
			return null;
		}
		for (Entity entity : targetTile.getEntities()) {
			if (entity.getType().equals(EntityType.FURNITURE)) {
				LiquidContainerComponent liquidContainerComponent = entity.getComponent(LiquidContainerComponent.class);
				if (liquidContainerComponent != null) {
					return liquidContainerComponent;
				}
			}
		}
		return null;
	}

	private boolean handle(RequestLiquidRemovalMessage message) {
		if (!message.requesterEntity.getType().equals(EntityType.FURNITURE)) {
			Logger.error("Not yet implemented, remove liquid from entity type " + message.requesterEntity.getType());
			return true;
		}

		messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION, new RequestHaulingAllocationMessage(message.requesterEntity,
				message.requesterEntity.getLocationComponent().getWorldPosition(),
				message.containerItemType, null, true, 1, null, haulingAllocation -> {
			Job job = null;
			if (haulingAllocation != null) {

				LiquidContainerComponent liquidContainerComponent = message.requesterEntity.getComponent(LiquidContainerComponent.class);
				LiquidAllocation liquidAllocation = liquidContainerComponent.createAllocation(message.quantity, message.requesterEntity);
				if (liquidAllocation == null) {
					messageDispatcher.dispatchMessage(MessageType.HAULING_ALLOCATION_CANCELLED, haulingAllocation);
				} else {
					haulingAllocation.setTargetPositionType(ZONE);
					haulingAllocation.setTargetPosition(message.workspaceLocation);

					job = new Job(removeLiquidJobType);
					job.setJobPriority(message.jobPriority);
					job.setJobLocation(message.workspaceLocation);
					job.setTargetId(message.requesterEntity.getId());
					job.setHaulingAllocation(haulingAllocation);
					job.setLiquidAllocation(liquidAllocation);
				}
			}

			if (job != null) {
				messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, job);
				message.callback.jobCreated(job);
			}
		}));
		return true;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
