package technology.rocketjump.undermount.mapping;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.FloorTypeDictionary;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.entities.behaviour.DoNothingBehaviour;
import technology.rocketjump.undermount.entities.behaviour.furniture.Prioritisable;
import technology.rocketjump.undermount.entities.factories.MechanismEntityAttributesFactory;
import technology.rocketjump.undermount.entities.factories.MechanismEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismTypeDictionary;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.jobs.model.JobTarget;
import technology.rocketjump.undermount.mapping.tile.*;
import technology.rocketjump.undermount.mapping.tile.designation.TileDesignation;
import technology.rocketjump.undermount.mapping.tile.floor.TileFloor;
import technology.rocketjump.undermount.mapping.tile.layout.WallLayout;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoof;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;
import technology.rocketjump.undermount.mapping.tile.underground.ChannelLayout;
import technology.rocketjump.undermount.mapping.tile.underground.UnderTile;
import technology.rocketjump.undermount.mapping.tile.wall.Wall;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.*;
import technology.rocketjump.undermount.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;
import technology.rocketjump.undermount.rooms.*;
import technology.rocketjump.undermount.rooms.components.StockpileComponent;
import technology.rocketjump.undermount.settlement.notifications.Notification;
import technology.rocketjump.undermount.ui.GameInteractionMode;
import technology.rocketjump.undermount.ui.GameInteractionStateContainer;
import technology.rocketjump.undermount.zones.Zone;

import java.util.*;

import static technology.rocketjump.undermount.settlement.notifications.NotificationType.AREA_REVEALED;

@Singleton
public class MapMessageHandler implements Telegraph, GameContextAware {

	private final MessageDispatcher messageDispatcher;
	private final OutdoorLightProcessor outdoorLightProcessor;
	private final GameInteractionStateContainer interactionStateContainer;
	private final RoomFactory roomFactory;
	private final RoomStore roomStore;
	private final JobStore jobStore;
	private final StockpileComponentUpdater stockpileComponentUpdater;
	private final RoofConstructionManager roofConstructionManager;
	private final MechanismTypeDictionary mechanismTypeDictionary;
	private final MechanismEntityAttributesFactory mechanismEntityAttributesFactory;
	private final MechanismEntityFactory mechanismEntityFactory;
	private final MechanismType pipeMechanismType;

	private GameContext gameContext;

	private ParticleEffectType wallRemovedParticleEffectType;
	private SoundAsset wallRemovedSoundAsset;
	private Map<ItemType, FloorType> floorTypesByInputRequirement = new HashMap<>();

	@Inject
	public MapMessageHandler(MessageDispatcher messageDispatcher, OutdoorLightProcessor outdoorLightProcessor,
							 GameInteractionStateContainer interactionStateContainer, RoomFactory roomFactory,
							 RoomStore roomStore, JobStore jobStore, StockpileComponentUpdater stockpileComponentUpdater,
							 RoofConstructionManager roofConstructionManager, ParticleEffectTypeDictionary particleEffectTypeDictionary,
							 SoundAssetDictionary soundAssetDictionary, FloorTypeDictionary floorTypeDictionary,
							 MechanismTypeDictionary mechanismTypeDictionary, MechanismEntityAttributesFactory mechanismEntityAttributesFactory,
							 MechanismEntityFactory mechanismEntityFactory) {
		this.messageDispatcher = messageDispatcher;
		this.outdoorLightProcessor = outdoorLightProcessor;
		this.interactionStateContainer = interactionStateContainer;
		this.roomFactory = roomFactory;
		this.roomStore = roomStore;
		this.jobStore = jobStore;
		this.stockpileComponentUpdater = stockpileComponentUpdater;
		this.roofConstructionManager = roofConstructionManager;

		this.wallRemovedParticleEffectType = particleEffectTypeDictionary.getByName("Dust cloud"); // MODDING expose this
		this.wallRemovedSoundAsset = soundAssetDictionary.getByName("Mining Drop");
		this.mechanismTypeDictionary = mechanismTypeDictionary;
		this.mechanismEntityAttributesFactory = mechanismEntityAttributesFactory;
		this.mechanismEntityFactory = mechanismEntityFactory;
		this.pipeMechanismType = mechanismTypeDictionary.getByName("Pipe");

		for (FloorType floorType : floorTypeDictionary.getAllDefinitions()) {
			if (floorType.isConstructed()) {
				floorTypesByInputRequirement.put(floorType.getRequirements().get(floorType.getMaterialType()).get(0).getItemType(), floorType);
			}
		}

		messageDispatcher.addListener(this, MessageType.ENTITY_POSITION_CHANGED);
		messageDispatcher.addListener(this, MessageType.AREA_SELECTION);
		messageDispatcher.addListener(this, MessageType.ROOM_PLACEMENT);
		messageDispatcher.addListener(this, MessageType.ADD_WALL);
		messageDispatcher.addListener(this, MessageType.REMOVE_WALL);
		messageDispatcher.addListener(this, MessageType.REMOVE_ROOM);
		messageDispatcher.addListener(this, MessageType.REMOVE_ROOM_TILES);
		messageDispatcher.addListener(this, MessageType.REPLACE_FLOOR);
		messageDispatcher.addListener(this, MessageType.UNDO_REPLACE_FLOOR);
		messageDispatcher.addListener(this, MessageType.REPLACE_REGION);
		messageDispatcher.addListener(this, MessageType.FLOORING_CONSTRUCTED);
		messageDispatcher.addListener(this, MessageType.ADD_CHANNEL);
		messageDispatcher.addListener(this, MessageType.REMOVE_CHANNEL);
		messageDispatcher.addListener(this, MessageType.ADD_PIPE);
		messageDispatcher.addListener(this, MessageType.REMOVE_PIPE);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.AREA_SELECTION: {
				return handle((AreaSelectionMessage) msg.extraInfo);
			}
			case MessageType.ROOM_PLACEMENT: {
				return handle((RoomPlacementMessage) msg.extraInfo);
			}
			case MessageType.ENTITY_POSITION_CHANGED: {
				return handle((EntityPositionChangedMessage) msg.extraInfo);
			}
			case MessageType.ADD_WALL: {
				AddWallMessage message = (AddWallMessage) msg.extraInfo;
				return addWall(message.location, message.material, message.wallType);
			}
			case MessageType.REMOVE_WALL: {
				GridPoint2 location = (GridPoint2) msg.extraInfo;
				return handleRemoveWall(location);
			}
			case MessageType.ADD_CHANNEL: {
				GridPoint2 location = (GridPoint2) msg.extraInfo;
				return handleAddChannel(location);
			}
			case MessageType.REMOVE_CHANNEL: {
				GridPoint2 location = (GridPoint2) msg.extraInfo;
				return handleRemoveChannel(location);
			}
			case MessageType.ADD_PIPE: {
				PipeConstructionMessage message = (PipeConstructionMessage) msg.extraInfo;
				return handleAddPipe(message);
			}
			case MessageType.REMOVE_PIPE: {
				GridPoint2 location = (GridPoint2) msg.extraInfo;
				return handleRemovePipe(location);
			}
			case MessageType.REMOVE_ROOM: {
				Room roomToRemove = (Room) msg.extraInfo;
				this.removeRoomTiles(new HashSet<>(roomToRemove.getRoomTiles().keySet()));
				return true;
			}
			case MessageType.REMOVE_ROOM_TILES: {
				Set<GridPoint2> roomTilesToRemove = (Set) msg.extraInfo;
				this.removeRoomTiles(roomTilesToRemove);
				return true;
			}
			case MessageType.REPLACE_FLOOR: {
				ReplaceFloorMessage message = (ReplaceFloorMessage) msg.extraInfo;
				this.replaceFloor(message.targetLocation, message.newFloorType, message.newMaterial);
				return true;
			}
			case MessageType.UNDO_REPLACE_FLOOR: {
				GridPoint2 location = (GridPoint2) msg.extraInfo;
				this.undoReplaceFloor(location);
				return true;
			}
			case MessageType.REPLACE_REGION: {
				ReplaceRegionMessage message = (ReplaceRegionMessage) msg.extraInfo;
				replaceRegion(message.tileToReplace, message.replacementRegionId);
				return true;
			}
			case MessageType.FLOORING_CONSTRUCTED: {
				FloorConstructionMessage message = (FloorConstructionMessage) msg.extraInfo;
				FloorType floorType = floorTypesByInputRequirement.get(message.constructionItem);
				if (floorType != null) {
					replaceFloor(message.location, floorType, message.constructionMaterial);
				} else {
					Logger.error("Could not look up floor type constructed by " + message.constructionItem.getItemTypeName());
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private boolean handleAddPipe(PipeConstructionMessage message) {
		MapTile tile = gameContext.getAreaMap().getTile(message.tilePosition);
		if (tile != null) {
			UnderTile underTile = tile.getOrCreateUnderTile();
			if (underTile.getPipeEntity() == null) {
				MechanismEntityAttributes attributes = mechanismEntityAttributesFactory.byType(pipeMechanismType, message.material);
				Entity pipeEntity = mechanismEntityFactory.create(attributes, message.tilePosition, new DoNothingBehaviour(), gameContext);
				underTile.setPipeEntity(pipeEntity);
				updateTile(tile, gameContext, messageDispatcher);
				messageDispatcher.dispatchMessage(MessageType.PIPE_ADDED, message.tilePosition);
			}
		}
		return true;
	}

	private boolean handleRemovePipe(GridPoint2 location) {
		MapTile tile = gameContext.getAreaMap().getTile(location);
		if (tile != null && tile.hasPipe()) {
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, tile.getUnderTile().getPipeEntity());
			updateTile(tile, gameContext, messageDispatcher);
		}
		return true;
	}

	private boolean handle(RoomPlacementMessage roomPlacementMessage) {
		// Need to create separate rooms if across different areas
		Map<GridPoint2, RoomTile> roomTilesToPlace = roomPlacementMessage.getRoomTiles();

		List<Room> newRooms = new LinkedList<>();

		while (!roomTilesToPlace.isEmpty()) {
			Room newRoom = roomFactory.create(roomPlacementMessage.getRoomType(), roomTilesToPlace);
			StockpileComponent stockpileComponent = newRoom.getComponent(StockpileComponent.class);
			if (stockpileComponent != null) {
				stockpileComponentUpdater.toggleGroup(stockpileComponent, roomPlacementMessage.stockpileGroup, true, true);
			}
			newRooms.add(newRoom);
		}

		for (Room newRoom : newRooms) {
			long thisRoomId = newRoom.getRoomId();
			Set<Long> roomsToMergeFrom = new HashSet<>();
			for (Map.Entry<GridPoint2, RoomTile> entry : newRoom.entrySet()) {
				if (entry.getValue().isAtRoomEdge()) {
					for (MapTile neighbourTile : gameContext.getAreaMap().getOrthogonalNeighbours(entry.getKey().x, entry.getKey().y).values()) {
						if (neighbourTile.hasRoom()) {
							Room neighbourRoom = neighbourTile.getRoomTile().getRoom();
							if (neighbourRoom.getRoomId() != thisRoomId && neighbourRoom.getRoomType().equals(newRoom.getRoomType())) {
								// Different room of same type
								roomsToMergeFrom.add(neighbourRoom.getRoomId());
							}
						}
					}
				}
			}

			if (!roomsToMergeFrom.isEmpty()) {
				while (!roomsToMergeFrom.isEmpty()) {
					long roomId = roomsToMergeFrom.iterator().next();
					roomsToMergeFrom.remove(roomId);
					Room roomToMergeFrom = roomStore.getById(roomId);
					newRoom.mergeFrom(roomToMergeFrom);
					roomStore.remove(roomToMergeFrom);
				}

				// Update all tile layouts
				newRoom.updateLayout(gameContext.getAreaMap());
			}
		}

		return true;
	}

	private boolean handle(AreaSelectionMessage areaSelectionMessage) {
		GridPoint2 minTile = new GridPoint2(MathUtils.floor(areaSelectionMessage.getMinPoint().x), MathUtils.floor(areaSelectionMessage.getMinPoint().y));
		GridPoint2 maxTile = new GridPoint2(MathUtils.floor(areaSelectionMessage.getMaxPoint().x), MathUtils.floor(areaSelectionMessage.getMaxPoint().y));

		Set<GridPoint2> roomTilesToRemove = new HashSet<>();

		for (int x = minTile.x; x <= maxTile.x; x++) {
			for (int y = minTile.y; y <= maxTile.y; y++) {
				MapTile tile = gameContext.getAreaMap().getTile(x, y);
				if (tile != null) {
					if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.REMOVE_DESIGNATIONS)) {
						if (tile.getDesignation() != null) {
							messageDispatcher.dispatchMessage(MessageType.REMOVE_DESIGNATION, new RemoveDesignationMessage(tile, tile.getDesignation()));
						}
					} else if (interactionStateContainer.getInteractionMode().designationCheck != null &&
							interactionStateContainer.getInteractionMode().getDesignationToApply() != null) {

						if (interactionStateContainer.getInteractionMode().designationCheck.shouldDesignationApply(tile)) {
							if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.REMOVE_ROOMS)) {
								roomTilesToRemove.add(tile.getTilePosition());
							} else {
								TileDesignation designationToApply = interactionStateContainer.getInteractionMode().getDesignationToApply();
								if (tile.getDesignation() != null) {
									messageDispatcher.dispatchMessage(MessageType.REMOVE_DESIGNATION, new RemoveDesignationMessage(tile, tile.getDesignation()));
								}
								tile.setDesignation(designationToApply);
								messageDispatcher.dispatchMessage(MessageType.DESIGNATION_APPLIED, new ApplyDesignationMessage(tile, designationToApply, interactionStateContainer.getInteractionMode()));
							}
						}
					} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.SET_JOB_PRIORITY)) {
						JobPriority priorityToApply = interactionStateContainer.getJobPriorityToApply();

						for (Job job : jobStore.getJobsAtLocation(tile.getTilePosition())) {
							job.setJobPriority(priorityToApply);
						}

						if (tile.hasConstruction()) {
							tile.getConstruction().setPriority(priorityToApply, messageDispatcher);
						}

						for (Entity entity : tile.getEntities()) {
							if (entity.getBehaviourComponent() instanceof Prioritisable) {
								Prioritisable prioritisableBehaviour = (Prioritisable) entity.getBehaviourComponent();
								prioritisableBehaviour.setPriority(priorityToApply);
							}
						}

					} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.DESIGNATE_ROOFING)) {
						messageDispatcher.dispatchMessage(MessageType.ROOF_CONSTRUCTION_QUEUE_CHANGE, new RoofConstructionQueueMessage(tile, true));
					} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.CANCEL_ROOFING)) {
						messageDispatcher.dispatchMessage(MessageType.ROOF_CONSTRUCTION_QUEUE_CHANGE, new RoofConstructionQueueMessage(tile, false));
						messageDispatcher.dispatchMessage(MessageType.ROOF_DECONSTRUCTION_QUEUE_CHANGE, new RoofDeconstructionQueueMessage(tile, false));
					} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.DECONSTRUCT_ROOFING)) {
						messageDispatcher.dispatchMessage(MessageType.ROOF_DECONSTRUCTION_QUEUE_CHANGE, new RoofDeconstructionQueueMessage(tile, true));
					} else {
						Logger.warn("Unhandled area selection message in " + getClass().getSimpleName());
					}
				}
			}
		}

		if (!roomTilesToRemove.isEmpty()) {
			removeRoomTiles(roomTilesToRemove);
		}
		return true;
	}

	private void removeRoomTiles(Set<GridPoint2> roomTilesToRemove) {
		Set<Room> roomsWithRemovedTiles = new HashSet<>();

		for (GridPoint2 tileLocation : roomTilesToRemove) {
			MapTile tile = gameContext.getAreaMap().getTile(tileLocation);
			if (tile != null) {
				RoomTile roomTile = tile.getRoomTile();
				if (roomTile != null) {
					roomsWithRemovedTiles.add(roomTile.getRoom());
					roomTile.getRoom().removeTile(tileLocation);
					tile.setRoomTile(null);
				}
			}
		}

		for (Room modifiedRoom : roomsWithRemovedTiles) {
			if (modifiedRoom.isEmpty()) {
				roomStore.remove(modifiedRoom);
			} else {
				// Need to see if this room has been split into more than 1 section
				splitRoomIfNecessary(modifiedRoom);
			}

		}

	}

	private void splitRoomIfNecessary(Room modifiedRoom) {
		Set<GridPoint2> traversed = new HashSet<>();
		IntMap<Set<GridPoint2>> tileGroups = new IntMap<>();
		int cursor = 1;

		Set<GridPoint2> allRoomTiles = modifiedRoom.keySet();
		for (GridPoint2 roomTile : allRoomTiles) {
			if (!traversed.contains(roomTile)) {
				Set<GridPoint2> roomTileGroup = new HashSet<>();
				tileGroups.put(cursor, roomTileGroup);
				cursor++;

				addAdjacentTilesToGroup(roomTile, traversed, roomTileGroup, modifiedRoom);
			}
		}

		if (tileGroups.size > 1) {
			// Need to create new room for each other section
			for (int groupCursor = 1; groupCursor < tileGroups.size; groupCursor++) {
				Set<GridPoint2> newRoomGroup = tileGroups.get(groupCursor);

				Room newRoom = roomFactory.createBasedOn(modifiedRoom);

				for (GridPoint2 positionToMove : newRoomGroup) {
					RoomTile roomTileToMove = modifiedRoom.removeTile(positionToMove);
					roomTileToMove.setRoom(newRoom);
					newRoom.addTile(roomTileToMove);
				}
				newRoom.updateLayout(gameContext.getAreaMap());
			}
		}
		modifiedRoom.updateLayout(gameContext.getAreaMap());
	}

	private void addAdjacentTilesToGroup(GridPoint2 currentTile, Set<GridPoint2> traversed, Set<GridPoint2> roomTileGroup, Room currentRoom) {
		roomTileGroup.add(currentTile);
		traversed.add(currentTile);

		for (MapTile neighbourTile : gameContext.getAreaMap().getOrthogonalNeighbours(currentTile.x, currentTile.y).values()) {
			if (traversed.contains(neighbourTile.getTilePosition())) {
				continue;
			}
			if (neighbourTile.hasRoom() && neighbourTile.getRoomTile().getRoom().getRoomId() == currentRoom.getRoomId()) {
				addAdjacentTilesToGroup(neighbourTile.getTilePosition(), traversed, roomTileGroup, currentRoom);
			}
		}
	}

	private boolean handle(EntityPositionChangedMessage message) {
		Entity entity = message.movingEntity;
		if (entity == null) {
			Logger.error(message.getClass().getSimpleName() + " handled with null entity");
			return true;
		}
		if (message.oldPosition != null) {
			MapTile oldCell = gameContext.getAreaMap().getTile(message.oldPosition);
			if (oldCell != null) {
				Entity removed = oldCell.removeEntity(entity.getId());
				if (removed == null) {
					Logger.error("Could not find entity " + entity.toString() + " in tile at " + message.oldPosition);
				}

				for (GridPoint2 otherTilePosition : entity.calculateOtherTilePositions()) {
					MapTile otherTile = gameContext.getAreaMap().getTile(otherTilePosition);
					if (otherTile != null) {
						otherTile.removeEntity(entity.getId());
					}
				}
			}
		}

		if (message.newPosition != null) {
			MapTile newCell = gameContext.getAreaMap().getTile(message.newPosition);
			if (newCell == null) {
				Logger.error("Entity " + entity.toString() + " appears to have moved off the map and/or a tile has disappeared, needs investigating");
			} else {
				newCell.addEntity(entity);
				for (GridPoint2 otherTilePosition : entity.calculateOtherTilePositions(message.newPosition)) {
					MapTile otherTile = gameContext.getAreaMap().getTile(otherTilePosition);
					if (otherTile != null) {
						otherTile.addEntity(entity);
					}
				}
			}

		}
		return true;
	}

	private boolean addWall(GridPoint2 location, GameMaterial wallMaterial, WallType wallType) {
		MapTile tileToAddWallTo = gameContext.getAreaMap().getTile(location);

		TileNeighbours tileNeighbours = gameContext.getAreaMap().getNeighbours(location);
		WallLayout wallLayout = new WallLayout(tileNeighbours);
		TileRoofState newRoofState = TileRoofState.CONSTRUCTED;
		if (tileToAddWallTo.getRoof().getState().equals(TileRoofState.MOUNTAIN_ROOF)) {
			newRoofState = TileRoofState.MOUNTAIN_ROOF;
		} else if (tileToAddWallTo.getRoof().getState().equals(TileRoofState.MINED)) {
			newRoofState = TileRoofState.MINED;
		}

		tileToAddWallTo.setWall(new Wall(wallLayout, wallType, wallMaterial), new TileRoof(newRoofState, wallMaterial));
		roofConstructionManager.supportConstructed(tileToAddWallTo);
		roofConstructionManager.roofConstructed(tileToAddWallTo);
		updateTile(tileToAddWallTo, gameContext, messageDispatcher);
		messageDispatcher.dispatchMessage(MessageType.WALL_CREATED, location);

		propagateDarknessFromTile(tileToAddWallTo, gameContext, outdoorLightProcessor);

		updateRegions(tileToAddWallTo, tileNeighbours);

		return true;
	}


	private boolean handleAddChannel(GridPoint2 location) {
		MapTile tileToAddChannelTo = gameContext.getAreaMap().getTile(location);

		TileNeighbours tileNeighbours = gameContext.getAreaMap().getNeighbours(location);
		ChannelLayout channelLayout = new ChannelLayout(tileNeighbours);

		if (tileToAddChannelTo.hasRoom()) {
			messageDispatcher.dispatchMessage(MessageType.REMOVE_ROOM_TILES, Set.of(location));
		}

		UnderTile underTile = tileToAddChannelTo.getOrCreateUnderTile();
		underTile.setChannelLayout(channelLayout);
		updateTile(tileToAddChannelTo, gameContext, messageDispatcher);

		updateRegions(tileToAddChannelTo, tileNeighbours);

		return true;
	}

	private void updateRegions(MapTile modifiedTile, TileNeighbours tileNeighbours) {
		MapTile north = tileNeighbours.get(CompassDirection.NORTH);
		MapTile south = tileNeighbours.get(CompassDirection.SOUTH);
		MapTile east = tileNeighbours.get(CompassDirection.EAST);
		MapTile west = tileNeighbours.get(CompassDirection.WEST);

		// Change the tile's region to be neighbouring same region type, or else a new region
		MapTile.RegionType myRegionType = modifiedTile.getRegionType();
		Integer neighbourRegionId = null;
		for (MapTile neighbourTile : Arrays.asList(north, south, east, west)) {
			if (neighbourTile != null && neighbourTile.getRegionType().equals(myRegionType)) {
				if (neighbourRegionId == null) {
					neighbourRegionId = neighbourTile.getRegionId();
					modifiedTile.setRegionId(neighbourRegionId);
				} else if (neighbourTile.getRegionId() != neighbourRegionId) {
					// Encountered a different neighbour region ID, merge together
					replaceRegion(neighbourTile, neighbourRegionId);
				}
			} else if (neighbourTile != null && neighbourTile.hasRoom()) {
				neighbourTile.getRoomTile().getRoom().checkIfEnclosed(gameContext.getAreaMap());
			}
		}
		if (neighbourRegionId == null) {
			neighbourRegionId = gameContext.getAreaMap().createNewRegionId();
			modifiedTile.setRegionId(neighbourRegionId);
		}

		// Figure out if new channel has split region into two - if so, create new region on one side
		boolean emptyEitherSide = false;
		MapTile sideA = null;
		MapTile sideB = null;

		List<List<MapTile>> pairings = Arrays.asList(
				Arrays.asList(north, south),
				Arrays.asList(east, west),

				Arrays.asList(north, west),
				Arrays.asList(north, east),
				Arrays.asList(south, east),
				Arrays.asList(south, west)
		);

		for (List<MapTile> pair : pairings) {
			if (pair.get(0) != null && !pair.get(0).getRegionType().equals(myRegionType) && pair.get(1) != null && !pair.get(1).getRegionType().equals(myRegionType) &&
					pair.get(0).getRegionType().equals(pair.get(1).getRegionType())) {
				emptyEitherSide = true;
				sideA = pair.get(0);
				sideB = pair.get(1);
				break;
			}
		}

		if (emptyEitherSide) {
			// Flood fill from one side until the other side is found, otherwise set all area of flood fill to new region
			Set<MapTile> explored = new HashSet<>();
			Deque<MapTile> frontier = new ArrayDeque<>();
			frontier.add(sideA);

			boolean otherSideFound = false;

			while (!frontier.isEmpty()) {
				MapTile current = frontier.pop();

				if (current.equals(sideB)) {
					otherSideFound = true;
					break;
				}

				explored.add(current);

				for (MapTile orthogonalNeighbour : gameContext.getAreaMap().getOrthogonalNeighbours(current.getTileX(), current.getTileY()).values()) {
					if (!explored.contains(orthogonalNeighbour) && !frontier.contains(orthogonalNeighbour)) {
						if (orthogonalNeighbour.getRegionType().equals(sideA.getRegionType())) {
							frontier.add(orthogonalNeighbour);
						}
					}
				}

			}

			if (!otherSideFound) {
				int newRegionId = gameContext.getAreaMap().createNewRegionId();
				replaceRegion(sideA, newRegionId);
			}
		}
	}

	public static void propagateDarknessFromTile(MapTile tile, GameContext gameContext, OutdoorLightProcessor outdoorLightProcessor) {
		EnumMap<CompassDirection, MapVertex> cellVertices = gameContext.getAreaMap().getVertexNeighboursOfCell(tile);
		for (MapVertex cellVertex : cellVertices.values()) {
			TileNeighbours neighboursOfCellVertex = gameContext.getAreaMap().getTileNeighboursOfVertex(cellVertex);
			boolean vertexSurroundedByIndoorCells = true;
			for (MapTile vertexNeighbour : neighboursOfCellVertex.values()) {
				if (vertexNeighbour != null && vertexNeighbour.getRoof().getState().equals(TileRoofState.OPEN)) {
					vertexSurroundedByIndoorCells = false;
					break;
				}
			}
			if (vertexSurroundedByIndoorCells) {
				outdoorLightProcessor.propagateDarknessFromVertex(gameContext.getAreaMap(), cellVertex);
			}
		}
	}

	private boolean handleRemoveWall(GridPoint2 location) {
		MapTile tile = gameContext.getAreaMap().getTile(location);
		if (tile != null && tile.hasWall()) {
			GameMaterial floorMaterial = tile.getWall().getMaterial();
			if (tile.getRoof().getRoofMaterial() != null && !tile.getRoof().getRoofMaterial().equals(GameMaterial.NULL_MATERIAL)) {
				floorMaterial = tile.getRoof().getRoofMaterial();
			}

			if (tile.getRoof().getState().equals(TileRoofState.MOUNTAIN_ROOF)) {
				tile.getRoof().setState(TileRoofState.MINED);
			}

			tile.setWall(null, tile.getRoof());
			TileDesignation tileDesignation = tile.getDesignation();
			if (tileDesignation != null) {
				messageDispatcher.dispatchMessage(MessageType.REMOVE_DESIGNATION, new RemoveDesignationMessage(tile, tile.getDesignation()));
			}
			tile.getFloor().setMaterial(floorMaterial);
			for (MapVertex vertex : gameContext.getAreaMap().getVertexNeighboursOfCell(tile).values()) {
				outdoorLightProcessor.propagateLightFromMapVertex(gameContext.getAreaMap(), vertex, vertex.getOutsideLightAmount());
			}

			updateTile(tile, gameContext, messageDispatcher);

			messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(wallRemovedParticleEffectType,
					Optional.empty(), Optional.of(new JobTarget(tile)), (p) -> {
			}));
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(wallRemovedSoundAsset, -1L,
					tile.getWorldPositionOfCenter(), null));

			Integer neighbourRegionId = null;
			MapTile unexploredTile = null;
			for (MapTile neighbourTile : gameContext.getAreaMap().getOrthogonalNeighbours(location.x, location.y).values()) {
				if (neighbourTile.hasFloor() && !neighbourTile.getFloor().isRiverTile()) {
					if (!neighbourTile.getExploration().equals(TileExploration.EXPLORED)) {
						unexploredTile = neighbourTile;
					}
					if (neighbourRegionId == null) {
						neighbourRegionId = neighbourTile.getRegionId();
						tile.setRegionId(neighbourRegionId);
					} else if (neighbourTile.getRegionId() != neighbourRegionId) {
						// Encountered a different neighbour region ID, merge together
						replaceRegion(neighbourTile, neighbourRegionId);
					}
				}
				if (neighbourTile.hasDoorway()) {
					messageDispatcher.dispatchMessage(MessageType.DECONSTRUCT_DOOR, neighbourTile.getDoorway());
				}
				if (neighbourTile.hasRoom()) {
					neighbourTile.getRoomTile().getRoom().checkIfEnclosed(gameContext.getAreaMap());
				}
			}
			if (unexploredTile != null) {
				Notification areaUncoveredNotification = new Notification(AREA_REVEALED, unexploredTile.getWorldPositionOfCenter());
				messageDispatcher.dispatchMessage(MessageType.POST_NOTIFICATION, areaUncoveredNotification);
			}
			if (neighbourRegionId == null) {
				neighbourRegionId = gameContext.getAreaMap().createNewRegionId();
				tile.setRegionId(neighbourRegionId);
			}
			messageDispatcher.dispatchMessage(MessageType.WALL_REMOVED, location);
		}
		return true;
	}


	private boolean handleRemoveChannel(GridPoint2 location) {
		MapTile tile = gameContext.getAreaMap().getTile(location);
		if (tile != null && tile.hasChannel()) {
			tile.getUnderTile().setChannelLayout(null);
			updateTile(tile, gameContext, messageDispatcher);

//			messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(wallRemovedParticleEffectType,
//					Optional.empty(), Optional.of(new JobTarget(tile)), (p) -> {}));
//			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(wallRemovedSoundAsset, -1L,
//					tile.getWorldPositionOfCenter(), null));
			updateRegions(tile, gameContext.getAreaMap().getNeighbours(location));
			;

			Integer neighbourRegionId = null;
			MapTile unexploredTile = null;
			for (MapTile neighbourTile : gameContext.getAreaMap().getOrthogonalNeighbours(location.x, location.y).values()) {
				if (neighbourTile.hasFloor() && !neighbourTile.getFloor().isRiverTile()) {
					if (!neighbourTile.getExploration().equals(TileExploration.EXPLORED)) {
						unexploredTile = neighbourTile;
					}
					if (neighbourRegionId == null) {
						neighbourRegionId = neighbourTile.getRegionId();
						tile.setRegionId(neighbourRegionId);
					} else if (neighbourTile.getRegionId() != neighbourRegionId) {
						// Encountered a different neighbour region ID, merge together
						replaceRegion(neighbourTile, neighbourRegionId);
					}
				}
				if (neighbourTile.hasDoorway()) {
					messageDispatcher.dispatchMessage(MessageType.DECONSTRUCT_DOOR, neighbourTile.getDoorway());
				}
				if (neighbourTile.hasRoom()) {
					neighbourTile.getRoomTile().getRoom().checkIfEnclosed(gameContext.getAreaMap());
				}
			}
			if (neighbourRegionId == null) {
				neighbourRegionId = gameContext.getAreaMap().createNewRegionId();
				tile.setRegionId(neighbourRegionId);
			}
			messageDispatcher.dispatchMessage(MessageType.WALL_REMOVED, location);
		}
		return true;
	}

	/**
	 * This method flood-fills the region specified in targetTile with replacementRegionId
	 */
	private void replaceRegion(MapTile initialTargetTile, int replacementRegionId) {
		int regionToReplace = initialTargetTile.getRegionId();
		Set<MapTile> visited = new HashSet<>();
		Queue<MapTile> frontier = new LinkedList<>();
		Set<Zone> zonesEncountered = new HashSet<>();
		frontier.add(initialTargetTile);

		while (!frontier.isEmpty()) {
			MapTile currentTile = frontier.poll();
			if (visited.contains(currentTile)) {
				continue;
			}
			currentTile.setRegionId(replacementRegionId);
			zonesEncountered.addAll(currentTile.getZones());
			visited.add(currentTile);

			for (MapTile neighbourTile : gameContext.getAreaMap().getOrthogonalNeighbours(currentTile.getTileX(), currentTile.getTileY()).values()) {
				if (visited.contains(neighbourTile)) {
					continue;
				}
				if (neighbourTile.getRegionId() == regionToReplace) {
					frontier.add(neighbourTile);
				}
			}
		}

		for (Zone movedZone : zonesEncountered) {
			gameContext.getAreaMap().removeZone(movedZone);
			movedZone.recalculate(gameContext.getAreaMap());
			movedZone.setRegionId(replacementRegionId);
			if (!movedZone.isEmpty()) {
				gameContext.getAreaMap().addZone(movedZone);
			}
		}
	}

	public static void updateTile(MapTile tile, GameContext gameContext, MessageDispatcher messageDispatcher) {
		TileNeighbours neighbours = gameContext.getAreaMap().getNeighbours(tile.getTileX(), tile.getTileY());
		tile.update(neighbours, gameContext.getAreaMap().getVertices(tile.getTileX(), tile.getTileY()), messageDispatcher);

		for (MapTile cellNeighbour : neighbours.values()) {
			cellNeighbour.update(gameContext.getAreaMap().getNeighbours(cellNeighbour.getTileX(), cellNeighbour.getTileY()),
					gameContext.getAreaMap().getVertices(cellNeighbour.getTileX(), cellNeighbour.getTileY()), messageDispatcher);
		}

		for (Zone zone : new ArrayList<>(tile.getZones())) {
			zone.recalculate(gameContext.getAreaMap());
			if (zone.isEmpty()) {
				gameContext.getAreaMap().removeZone(zone);
			}
		}

	}

	public void replaceFloor(GridPoint2 location, FloorType floorType, GameMaterial material) {
		MapTile mapTile = gameContext.getAreaMap().getTile(location);

		TileFloor newFloor = new TileFloor(floorType, material);
		mapTile.replaceFloor(newFloor);

		updateTile(mapTile, gameContext, messageDispatcher);
	}

	public void undoReplaceFloor(GridPoint2 location) {
		MapTile mapTile = gameContext.getAreaMap().getTile(location);
		mapTile.popFloor();

		updateTile(mapTile, gameContext, messageDispatcher);
	}

	public static void markAsOutside(MapTile tile, GameContext gameContext, OutdoorLightProcessor outdoorLightProcessor) {
		tile.getRoof().setState(TileRoofState.OPEN);
		tile.getRoof().setRoofMaterial(GameMaterial.NULL_MATERIAL);

		for (MapVertex vertex : gameContext.getAreaMap().getVertexNeighboursOfCell(tile).values()) {
			vertex.setOutsideLightAmount(1.0f);
			outdoorLightProcessor.propagateLightFromMapVertex(gameContext.getAreaMap(), vertex, 1f);
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
