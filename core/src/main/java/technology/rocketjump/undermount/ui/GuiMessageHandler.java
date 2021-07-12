package technology.rocketjump.undermount.ui;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.FloorTypeDictionary;
import technology.rocketjump.undermount.assets.WallTypeDictionary;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.entities.factories.FurnitureEntityAttributesFactory;
import technology.rocketjump.undermount.entities.factories.FurnitureEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.mapping.model.WallPlacementMode;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.designation.TileDesignationDictionary;
import technology.rocketjump.undermount.mapping.tile.roof.RoofConstructionState;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.*;
import technology.rocketjump.undermount.rooms.Bridge;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.rooms.StockpileGroup;
import technology.rocketjump.undermount.rooms.constructions.Construction;
import technology.rocketjump.undermount.sprites.BridgeTypeDictionary;
import technology.rocketjump.undermount.ui.views.FurnitureSelectionGuiView;
import technology.rocketjump.undermount.ui.views.GuiViewName;

import java.util.*;

import static technology.rocketjump.undermount.mapping.tile.TileExploration.EXPLORED;
import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;
import static technology.rocketjump.undermount.ui.Selectable.SelectableType.*;

@Singleton
public class GuiMessageHandler implements Telegraph, GameContextAware {

	private static final float ENTITY_SELECTION_RADIUS = 0.25f; // Small distance as this is added to radius
	private final MessageDispatcher messageDispatcher;
	private final GameInteractionStateContainer interactionStateContainer;

	private final FurnitureEntityAttributesFactory furnitureEntityAttributesFactory;
	private final FurnitureEntityFactory furnitureEntityFactory;
	private final FurnitureSelectionGuiView furnitureSelectionGuiView;
	private final BridgeTypeDictionary bridgeTypeDictionary;
	private GameContext gameContext;

	private Map<GameMaterialType, WallType> wallTypeMapping = new HashMap<>();
	private Map<GameMaterialType, FloorType> floorTypeMapping = new HashMap<>();

	@Inject
	public GuiMessageHandler(MessageDispatcher messageDispatcher, GameInteractionStateContainer interactionStateContainer,
							 TileDesignationDictionary tileDesignationDictionary, WallTypeDictionary wallTypeDictionary,
							 FurnitureEntityAttributesFactory furnitureEntityAttributesFactory, FurnitureEntityFactory furnitureEntityFactory,
							 FurnitureSelectionGuiView furnitureSelectionGuiView, BridgeTypeDictionary bridgeTypeDictionary,
							 FloorTypeDictionary floorTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.interactionStateContainer = interactionStateContainer;
		this.furnitureEntityAttributesFactory = furnitureEntityAttributesFactory;
		this.furnitureEntityFactory = furnitureEntityFactory;
		this.furnitureSelectionGuiView = furnitureSelectionGuiView;
		this.bridgeTypeDictionary = bridgeTypeDictionary;

		messageDispatcher.addListener(this, MessageType.MOUSE_DOWN);
		messageDispatcher.addListener(this, MessageType.MOUSE_UP);
		messageDispatcher.addListener(this, MessageType.MOUSE_MOVED);
		messageDispatcher.addListener(this, MessageType.CAMERA_MOVED);
		messageDispatcher.addListener(this, MessageType.FURNITURE_MATERIAL_SELECTED);
		messageDispatcher.addListener(this, MessageType.GUI_FURNITURE_TYPE_SELECTED);
		messageDispatcher.addListener(this, MessageType.ROTATE_FURNITURE);
		messageDispatcher.addListener(this, MessageType.DESTROY_ENTITY);
		messageDispatcher.addListener(this, MessageType.WALL_MATERIAL_SELECTED);
		messageDispatcher.addListener(this, MessageType.FLOOR_MATERIAL_SELECTED);
		messageDispatcher.addListener(this, MessageType.DOOR_MATERIAL_SELECTED);
		messageDispatcher.addListener(this, MessageType.WALL_PLACEMENT_SELECTED);
		messageDispatcher.addListener(this, MessageType.BRIDGE_MATERIAL_SELECTED);
		messageDispatcher.addListener(this, MessageType.CONSTRUCTION_REMOVED);
		messageDispatcher.addListener(this, MessageType.CONSTRUCTION_COMPLETED);
		messageDispatcher.addListener(this, MessageType.REMOVE_ROOM);
		messageDispatcher.addListener(this, MessageType.DECONSTRUCT_BRIDGE);
		messageDispatcher.addListener(this, MessageType.CHOOSE_SELECTABLE);
		messageDispatcher.addListener(this, MessageType.REPLACE_JOB_PRIORITY);
		messageDispatcher.addListener(this, MessageType.GUI_STOCKPILE_GROUP_SELECTED);
		// FIXME Should these really live here?
		for (WallType wallType : wallTypeDictionary.getAllDefinitions()) {
			if (wallType.isConstructed()) {
				wallTypeMapping.put(wallType.getMaterialType(), wallType);
			}
		}
		for (FloorType floorType : floorTypeDictionary.getAllDefinitions()) {
			if (floorType.isConstructed()) {
				floorTypeMapping.put(floorType.getMaterialType(), floorType);
			}
		}


		tileDesignationDictionary.init();
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.MOUSE_DOWN: {
				MouseChangeMessage mouseChangeMessage = (MouseChangeMessage) msg.extraInfo;
				// If state is designating something
				if (mouseChangeMessage.getButtonType().equals(MouseChangeMessage.MouseButtonType.PRIMARY_BUTTON)) {
					interactionStateContainer.setStartPoint(mouseChangeMessage.getWorldPosition());
					interactionStateContainer.setCurrentPoint(mouseChangeMessage.getWorldPosition());
					if (interactionStateContainer.getInteractionMode().isDraggable) {
						interactionStateContainer.setDragging(true);
					}
				}
				return true;
			}
			case MessageType.MOUSE_UP: {
				MouseChangeMessage mouseChangeMessage = (MouseChangeMessage) msg.extraInfo;
				if (mouseChangeMessage.getButtonType().equals(MouseChangeMessage.MouseButtonType.PRIMARY_BUTTON)) {
					primaryButtonClicked(mouseChangeMessage);
				} else if (mouseChangeMessage.getButtonType().equals(MouseChangeMessage.MouseButtonType.CANCEL_BUTTON)) {
					cancelButtonClicked();
				} else {
					messageDispatcher.dispatchMessage(MessageType.DEBUG_MESSAGE, new DebugMessage(mouseChangeMessage.getWorldPosition()));
				}
				return true;
			}
			case MessageType.MOUSE_MOVED: {
				MouseChangeMessage mouseChangeMessage = (MouseChangeMessage) msg.extraInfo;
				if (interactionStateContainer.isDragging()) {
					interactionStateContainer.setCurrentPoint(mouseChangeMessage.getWorldPosition());
				}
				return true;
			}
			case MessageType.CAMERA_MOVED: {
				if (interactionStateContainer.isDragging()) {
					CameraMovedMessage cameraMovedMessage = (CameraMovedMessage) msg.extraInfo;
					interactionStateContainer.setCurrentPoint(cameraMovedMessage.cursorWorldPosition);
				}
				return true;
			}
			case MessageType.FURNITURE_MATERIAL_SELECTED:
			case MessageType.GUI_FURNITURE_TYPE_SELECTED: {
				rebuildFurnitureEntity();
				return true;
			}
			case MessageType.ROTATE_FURNITURE: {
				if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.PLACE_FURNITURE)) {
					Entity furnitureEntity = interactionStateContainer.getFurnitureEntityToPlace();
					if (furnitureEntity != null) {
						FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();
						if (attributes.getCurrentLayout().getRotatesTo() != null) {
							attributes.setCurrentLayout(attributes.getCurrentLayout().getRotatesTo());
							messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, furnitureEntity);
						}
					}
				}
				return true;
			}
			case MessageType.DESTROY_ENTITY: { // Need to stop showing destroyed entities
				if (interactionStateContainer.getSelectable() != null && interactionStateContainer.getSelectable().type.equals(ENTITY)) {
					EntityMessage message = (EntityMessage) msg.extraInfo;
					if (message.getEntityId() == interactionStateContainer.getSelectable().getId()) {
						clearSelectable();
					}
				}
				return false;
			}
			case MessageType.DECONSTRUCT_BRIDGE: { // Need to stop showing destroyed entities
				if (interactionStateContainer.getSelectable() != null && interactionStateContainer.getSelectable().type.equals(BRIDGE)) {
					Bridge removedBridge = (Bridge) msg.extraInfo;
					if (removedBridge.getBridgeId() == interactionStateContainer.getSelectable().getId()) {
						clearSelectable();
					}
				}
				return false;
			}
			case MessageType.GUI_STOCKPILE_GROUP_SELECTED: {
				StockpileGroup stockpileGroup = (StockpileGroup) msg.extraInfo;
				interactionStateContainer.setSelectedStockpileGroup(stockpileGroup);
				return true;
			}
			case MessageType.DOOR_MATERIAL_SELECTED: {
				MaterialSelectionMessage materialSelectionMessage = (MaterialSelectionMessage) msg.extraInfo;
				interactionStateContainer.setDoorMaterialSelection(materialSelectionMessage);
				return true;
			}
			case MessageType.WALL_MATERIAL_SELECTED: {
				MaterialSelectionMessage materialSelectionMessage = (MaterialSelectionMessage) msg.extraInfo;
				interactionStateContainer.setWallMaterialSelection(materialSelectionMessage);
				interactionStateContainer.setWallTypeToPlace(wallTypeMapping.get(materialSelectionMessage.selectedMaterialType));
				return true;
			}
			case MessageType.FLOOR_MATERIAL_SELECTED: {
				MaterialSelectionMessage materialSelectionMessage = (MaterialSelectionMessage) msg.extraInfo;
				interactionStateContainer.setFloorMaterialSelection(materialSelectionMessage);
				interactionStateContainer.setFloorTypeToPlace(floorTypeMapping.get(materialSelectionMessage.selectedMaterialType));
				return true;
			}
			case MessageType.WALL_PLACEMENT_SELECTED: {
				WallPlacementMode wallPlacementMode = (WallPlacementMode) msg.extraInfo;
				interactionStateContainer.setWallPlacementMode(wallPlacementMode);
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_WALLS);
				return true;
			}
			case MessageType.BRIDGE_MATERIAL_SELECTED: {
				MaterialSelectionMessage materialSelectionMessage = (MaterialSelectionMessage) msg.extraInfo;
				interactionStateContainer.setBridgeMaterialSelection(materialSelectionMessage);
				interactionStateContainer.setBridgeTypeToPlace(bridgeTypeDictionary.getByMaterialType(materialSelectionMessage.selectedMaterialType));
				return true;
			}
			case MessageType.CONSTRUCTION_REMOVED:
			case MessageType.CONSTRUCTION_COMPLETED:
				Construction construction = (Construction) msg.extraInfo;
				if (interactionStateContainer.getSelectable() != null && interactionStateContainer.getSelectable().type.equals(CONSTRUCTION)
						&& interactionStateContainer.getSelectable().getConstruction().equals(construction)) {
					clearSelectable();
				}
				return true;
			case MessageType.REMOVE_ROOM: {
				Room removedRoom = (Room) msg.extraInfo;
				if (interactionStateContainer.getSelectable() != null && interactionStateContainer.getSelectable().type.equals(ROOM)
						&& interactionStateContainer.getSelectable().getRoom().equals(removedRoom)) {
					clearSelectable();
				}
				return false;
			}
			case MessageType.CHOOSE_SELECTABLE: {
				Selectable selectable = (Selectable) msg.extraInfo;
				chooseSelectable(selectable);
				return true;
			}
			case MessageType.REPLACE_JOB_PRIORITY: {
				JobPriority jobPriority = (JobPriority)msg.extraInfo;
				interactionStateContainer.setJobPriorityToApply(jobPriority);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void rebuildFurnitureEntity() {
		FurnitureType selectedFurnitureType = furnitureSelectionGuiView.getSelectedFurnitureType();
		GameMaterial materialForFurniture = furnitureSelectionGuiView.getSelectedMaterial();
		if (selectedFurnitureType == null || materialForFurniture == null) {
			return;
		}

		if (materialForFurniture.equals(NULL_MATERIAL)) {
			// Override null material with a new material of the right material type
			// Used to render the correct material type of furniture, not persisted so only for use in UI
			materialForFurniture = GameMaterial.nullMaterialWithType(furnitureSelectionGuiView.getSelectedMaterialType());
		}

		FurnitureEntityAttributes attributes = furnitureEntityAttributesFactory.byType(selectedFurnitureType, materialForFurniture);
		Entity furnitureEntity = furnitureEntityFactory.create(attributes, new GridPoint2(), null, gameContext);
		furnitureEntity.getLocationComponent().init(furnitureEntity, null, gameContext); // Remove messageDispatcher so position updates are not sent
		interactionStateContainer.setFurnitureEntityToPlace(furnitureEntity);
	}

	private void primaryButtonClicked(MouseChangeMessage mouseChangeMessage) {
		if (interactionStateContainer.isDragging()) {
			interactionStateContainer.setDragging(false);

			if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.PLACE_ROOM)) {
				RoomPlacementMessage roomPlacementMessage = new RoomPlacementMessage(interactionStateContainer.virtualRoom.getRoomTiles(),
						interactionStateContainer.getInteractionMode().getRoomType(), interactionStateContainer.getSelectedStockpileGroup());
				messageDispatcher.dispatchMessage(MessageType.ROOM_PLACEMENT, roomPlacementMessage);
			} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.PLACE_WALLS)) {
				WallsPlacementMessage message = new WallsPlacementMessage(new LinkedList<>(interactionStateContainer.getVirtualWallConstructions()));
				messageDispatcher.dispatchMessage(MessageType.WALL_PLACEMENT, message);
				interactionStateContainer.getVirtualWallConstructions().clear();

				for (GridPoint2 location : interactionStateContainer.getVirtualRoofConstructions()) {
					MapTile tile = gameContext.getAreaMap().getTile(location);
					if (tile != null && tile.getRoof().getState().equals(TileRoofState.OPEN) && tile.getRoof().getConstructionState().equals(RoofConstructionState.NONE)) {
						messageDispatcher.dispatchMessage(MessageType.ROOF_CONSTRUCTION_QUEUE_CHANGE,
								new RoofConstructionQueueMessage(tile, true));
					}
				}

			} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.PLACE_BRIDGE)) {
				if (interactionStateContainer.isValidBridgePlacement()) {
					messageDispatcher.dispatchMessage(MessageType.BRIDGE_PLACEMENT, interactionStateContainer.getVirtualBridgeConstruction().getBridge());
				}
			} else {
				AreaSelectionMessage areaSelectionMessage = new AreaSelectionMessage(interactionStateContainer.getMinPoint(), interactionStateContainer.getMaxPoint());
				messageDispatcher.dispatchMessage(AreaSelectionMessage.MESSAGE_TYPE, areaSelectionMessage);
			}

		} else {
			// Not dragging
			if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.DEFAULT) && gameContext != null) {
				defaultWorldClick(mouseChangeMessage);
			} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.PLACE_FURNITURE)) {
				if (interactionStateContainer.isValidFurniturePlacement()) {
					messageDispatcher.dispatchMessage(MessageType.FURNITURE_PLACEMENT, interactionStateContainer.getFurnitureEntityToPlace());
				}
			} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.PLACE_DOOR)) {
				if (interactionStateContainer.isValidDoorPlacement()) {
					messageDispatcher.dispatchMessage(MessageType.DOOR_PLACEMENT, interactionStateContainer.getVirtualDoorPlacement());
				}
			}

		}
	}

	private void defaultWorldClick(MouseChangeMessage mouseChangeMessage) {
		List<Selectable> selectables = new ArrayList<>();

		// See if an entity has been clicked on
		Vector2 worldClickPosition = mouseChangeMessage.getWorldPosition();
		for (MapTile nearbyTile : gameContext.getAreaMap().getNearestTiles(worldClickPosition)) {
			if (nearbyTile.getExploration().equals(EXPLORED)) {
				for (Entity entity : nearbyTile.getEntities()) {
					float distanceToEntity = Math.abs(entity.getLocationComponent().getWorldPosition().dst2(worldClickPosition) + (entity.getLocationComponent().getRadius() * entity.getLocationComponent().getRadius()));
					if (distanceToEntity < ENTITY_SELECTION_RADIUS) {
						selectables.add(new Selectable(entity, distanceToEntity));
					}
				}
			}
		}

		MapTile clickedTile = gameContext.getAreaMap().getTile(worldClickPosition);
		if (clickedTile != null) {
			if (clickedTile.getExploration().equals(EXPLORED)) {
				// Adding all entities in clicked tile to cover multi-tile entities like furniture
				for (Entity entity : clickedTile.getEntities()) {
					float distanceToEntity = Math.abs(entity.getLocationComponent().getWorldPosition().dst2(worldClickPosition) + (entity.getLocationComponent().getRadius() * entity.getLocationComponent().getRadius()));
					Selectable selectableEntity = new Selectable(entity, distanceToEntity);
					if (!selectables.contains(selectableEntity)) {
						selectables.add(selectableEntity);
					}
				}

				if (clickedTile.hasConstruction()) {
					selectables.add(new Selectable(clickedTile.getConstruction()));
				}

				if (clickedTile.hasDoorway()) {
					selectables.add(new Selectable(clickedTile.getDoorway()));
				}

				if (clickedTile.hasRoom()) {
					selectables.add(new Selectable(clickedTile.getRoomTile().getRoom()));
				}

				if (clickedTile.getFloor().hasBridge()) {
					selectables.add(new Selectable(clickedTile.getFloor().getBridge()));
				}
			}
			selectables.add(new Selectable(clickedTile));
		}

		if (!selectables.isEmpty()) {
			Collections.sort(selectables);

			Selectable selected = null;
			if (interactionStateContainer.getSelectable() == null) {
				// Nothing yet selected
				selected = selectables.get(0);
			} else {
				for (int cursor = 0; cursor < selectables.size(); cursor++) {
					Selectable nextToTry = selectables.get(cursor);
					if (nextToTry.equals(interactionStateContainer.getSelectable())) {
						// This is the one already selected
						if (cursor + 1 < selectables.size()) {
							selected = selectables.get(cursor + 1);
						} else {
							selected = selectables.get(0);
						}
						break;
					}
				}
				if (selected == null) {
					selected = selectables.get(0);
				}
			}
			messageDispatcher.dispatchMessage(MessageType.CHOOSE_SELECTABLE, selected);
		}
	}

	private void chooseSelectable(Selectable selected) {
		interactionStateContainer.setSelectable(selected);
		switch (selected.type) {
			case ENTITY:
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ENTITY_SELECTED);
				break;
			case CONSTRUCTION:
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.CONSTRUCTION_SELECTED);
				break;
			case DOORWAY:
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.DOORWAY_SELECTED);
				break;
			case TILE:
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.TILE_SELECTED);
				break;
			case ROOM:
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ROOM_SELECTED);
				break;
			case BRIDGE:
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.BRIDGE_SELECTED);
				break;
			default:
				Logger.error("Not yet implemented: UI selection of " + selected.type);
		}
	}

	private void cancelButtonClicked() {
		interactionStateContainer.setSelectable(null);
		if (interactionStateContainer.isDragging()) {
			interactionStateContainer.setDragging(false);
		} else {
			if (!interactionStateContainer.getInteractionMode().equals(GameInteractionMode.DEFAULT)) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
			} else {
				// In default interaction mode
				// Try going back a menu level
				messageDispatcher.dispatchMessage(MessageType.GUI_CANCEL_CURRENT_VIEW);
			}
		}
	}

	@Override
	public void clearContextRelatedState() {
		clearSelectable();
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	private void clearSelectable() {
		interactionStateContainer.setSelectable(null);
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.DEFAULT_MENU);
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
	}
}
