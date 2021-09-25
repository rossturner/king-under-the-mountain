package technology.rocketjump.undermount.ui;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesGrowthStage;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.designation.TileDesignation;
import technology.rocketjump.undermount.mapping.tile.designation.TileDesignationDictionary;
import technology.rocketjump.undermount.mapping.tile.underground.PipeConstructionState;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.rooms.RoomType;

import java.util.HashMap;
import java.util.Map;

import static technology.rocketjump.undermount.entities.model.EntityType.*;
import static technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesGrowthStage.PlantSpeciesHarvestType.FORAGING;
import static technology.rocketjump.undermount.mapping.tile.TileExploration.EXPLORED;
import static technology.rocketjump.undermount.mapping.tile.roof.RoofConstructionState.NONE;
import static technology.rocketjump.undermount.mapping.tile.roof.TileRoofState.CONSTRUCTED;
import static technology.rocketjump.undermount.mapping.tile.roof.TileRoofState.OPEN;

// MODDING extract this enum to data-driven set of behaviours (when we know how to)
public enum GameInteractionMode {

	DEFAULT(null, null, null, false),
	DESIGNATE_MINING("mining", "MINING", mapTile -> ((!mapTile.getExploration().equals(EXPLORED) && mapTile.getDesignation() == null) || (mapTile.hasWall() && mapTile.getDesignation() == null)), true),
	DESIGNATE_CHOP_WOOD("logging", "CHOP_WOOD", mapTile -> (mapTile.getExploration().equals(EXPLORED) && mapTile.hasTree() && mapTile.getDesignation() == null), true),
	DESIGNATE_DIG_CHANNEL("spade", "DIG_CHANNEL", mapTile -> {
		if (!mapTile.getExploration().equals(EXPLORED) || mapTile.getDesignation() != null || mapTile.hasChannel() || isRiverEdge(mapTile)) {
			return false;
		}
		for (Entity entity : mapTile.getEntities()) {
			if (entity.getType().equals(FURNITURE)) {
				return false;
			}
		}
		return mapTile.hasFloor() && mapTile.getFloor().getMaterial().getMaterialType().equals(GameMaterialType.EARTH);
	}, true),
	DESIGNATE_CLEAR_GROUND("spade", "CLEAR_GROUND", mapTile -> {
		if (!mapTile.getExploration().equals(EXPLORED) || mapTile.getDesignation() != null) {
			return false;
		}
		for (Entity entity : mapTile.getEntities()) {
			if (entity.getType().equals(ITEM)) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getItemType().getStockpileGroup() == null) {
					return true;
				}
			} else if (entity.getType().equals(PLANT)) {
				PlantEntityAttributes attributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getSpecies().getPlantType().removalJobTypeName.equals("CLEAR_GROUND")) {
					return true;
				}
			}
		}
		return false;
	}, true),
	DESIGNATE_HARVEST_PLANTS("sickle", "HARVEST", mapTile -> {
		Entity plant = mapTile.getPlant();
		if (plant != null && mapTile.getExploration().equals(EXPLORED) && mapTile.getDesignation() == null) {
			PlantEntityAttributes attributes = (PlantEntityAttributes) plant.getPhysicalEntityComponent().getAttributes();
			PlantSpeciesGrowthStage growthStage = attributes.getSpecies().getGrowthStages().get(attributes.getGrowthStageCursor());
			return FORAGING.equals(growthStage.getHarvestType());
		} else {
			return false;
		}
	}, true),
	DESIGNATE_EXTINGUISH_FLAMES("splash", "EXTINGUISH_FLAMES", mapTile -> {
		if (!mapTile.getExploration().equals(EXPLORED) || mapTile.getDesignation() != null) {
			return false;
		}
		for (Entity entity : mapTile.getEntities()) {
			if (STATIC_ENTITY_TYPES.contains(entity.getType()) && entity.isOnFire()) {
				return true;
			} else if (entity.getType().equals(ONGOING_EFFECT)) {
				OngoingEffectAttributes attributes = (OngoingEffectAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getType().isCanBeExtinguished()) {
					return true;
				}
			}
		}
		return false;
	}, true),

	DESIGNATE_ROOFING("roofing", null, mapTile -> mapTile.getExploration().equals(EXPLORED) &&
			mapTile.getRoof().getState().equals(OPEN) && mapTile.getRoof().getConstructionState().equals(NONE), true),
	CANCEL_ROOFING("cancel", null, mapTile -> mapTile.getExploration().equals(EXPLORED) &&
			mapTile.getRoof().getState().equals(OPEN) && !mapTile.getRoof().getConstructionState().equals(NONE), true),
	DECONSTRUCT_ROOFING("deconstruct", null, mapTile -> mapTile.getExploration().equals(EXPLORED) &&
			mapTile.getRoof().getState().equals(CONSTRUCTED) && mapTile.getRoof().getConstructionState().equals(NONE), true),

	DESIGNATE_PIPING("splash", null, mapTile ->  mapTile.getExploration().equals(EXPLORED) &&
			!isRiverEdge(mapTile) && !mapTile.getFloor().isRiverTile() && !mapTile.hasPipe(), true),
	CANCEL_PIPING("cancel", null, mapTile -> mapTile.getExploration().equals(EXPLORED) &&
			mapTile.getUnderTile() != null && mapTile.getUnderTile().getPipeConstructionState().equals(PipeConstructionState.READY_FOR_CONSTRUCTION), true),
	DECONSTRUCT_PIPING("deconstruct", null, mapTile -> mapTile.getExploration().equals(EXPLORED) && mapTile.hasPipe(), true),

	DESIGNATE_MECHANISMS("gears", null, mapTile -> mapTile.getExploration().equals(EXPLORED) &&
			!mapTile.getFloor().isRiverTile() && !mapTile.hasPowerMechanism() && (mapTile.getUnderTile() == null || mapTile.getUnderTile().getQueuedMechanismType() == null), false),
	CANCEL_MECHANISMS("cancel", null, mapTile -> mapTile.getExploration().equals(EXPLORED) &&
			mapTile.getUnderTile() != null && mapTile.getUnderTile().getQueuedMechanismType() != null, true),
	DECONSTRUCT_MECHANISMS("deconstruct", null, mapTile -> mapTile.getExploration().equals(EXPLORED) && mapTile.hasPowerMechanism(), true),

	REMOVE_DESIGNATIONS("cancel", null, mapTile -> mapTile.getDesignation() != null, true),
	PLACE_ROOM("rooms", null, mapTile -> mapTile.getExploration().equals(EXPLORED) && !mapTile.hasWall() &&
			!mapTile.hasRoom() && !mapTile.hasDoorway() && !mapTile.isWaterSource() && !mapTile.getFloor().hasBridge(), true),
	PLACE_FURNITURE("zones", null, null, false),
	PLACE_DOOR("door", null, null, false),
	PLACE_WALLS("walls", null, null, true),
	PLACE_BRIDGE("bridge", null, mapTile -> mapTile.getExploration().equals(EXPLORED) && !mapTile.hasWall() &&
			!mapTile.hasDoorway() && !mapTile.hasRoom() && !mapTile.hasConstruction(), true),
	PLACE_FLOORING("flooring", "FLOORING", mapTile -> mapTile.hasFloor() && !mapTile.getFloor().isRiverTile(), true),
	REMOVE_ROOMS("cancel", "REMOVE_ROOMS", MapTile::hasRoom, true),
	SET_JOB_PRIORITY("priority", null, null, true),
	REMOVE_CONSTRUCTIONS("cancel", "REMOVE_CONSTRUCTIONS", tile -> tile.hasConstruction() || tile.getDesignation() != null, true),
	DECONSTRUCT("deconstruct", "DECONSTRUCT", mapTile -> {
		return mapTile.getFloor().hasBridge() || mapTile.hasDoorway() || mapTile.getEntities().stream().anyMatch(e -> e.getType().equals(FURNITURE)) ||
				mapTile.hasChannel() || (mapTile.hasFloor() && mapTile.getFloor().getFloorType().isConstructed()) ||
				(mapTile.hasWall() && mapTile.getWall().getWallType().isConstructed());
	}, true);


	public final String cursorName;

	public final String designationName;
	private TileDesignation designationToApply;
	public final DesignationCheck designationCheck;
	private RoomType roomType;
	private FurnitureType furnitureType;
	public final boolean isDraggable;

	GameInteractionMode(String cursorName, String designationName, DesignationCheck designationCheck, boolean isDraggable) {
		this.cursorName = cursorName;
		this.designationName = designationName;
		this.designationCheck = designationCheck;
		this.isDraggable = isDraggable;
	}

	public static void init(TileDesignationDictionary designationDictionary) {
		for (GameInteractionMode interactionMode : values()) {
			if (interactionMode.designationName != null) {
				TileDesignation designation = designationDictionary.getByName(interactionMode.designationName);
				if (designation == null) {
					throw new RuntimeException("No designation found by name: " + interactionMode.designationName);
				}
				interactionMode.designationToApply = designation;
			}
		}

		PLACE_ROOM.roomType = new RoomType();
	}

	private static Map<String, GameInteractionMode> byDesignationName = new HashMap<>();

	static {
		for (GameInteractionMode interactionMode : GameInteractionMode.values()) {
			byDesignationName.put(interactionMode.designationName, interactionMode);
		}
	}
	public static GameInteractionMode getByDesignationName(String designationName) {
		return byDesignationName.get(designationName);
	}
	public TileDesignation getDesignationToApply() {
		return designationToApply;
	}

	public boolean isDesignation() {
		return REMOVE_DESIGNATIONS.equals(this) || designationName != null;
	}

	public RoomType getRoomType() {
		return roomType;
	}

	public void setRoomType(RoomType roomType) {
		this.roomType = roomType;
	}

	public void setFurnitureType(FurnitureType furnitureType) {
		this.furnitureType = furnitureType;
	}

	public FurnitureType getFurnitureType() {
		return furnitureType;
	}

	public interface DesignationCheck {

		boolean shouldDesignationApply(MapTile mapTile);
	}

	public static boolean isRiverEdge(MapTile mapTile) {
		return mapTile.getAllFloors().stream().anyMatch(f -> f.getFloorType().getFloorTypeName().equals("river-edge-dirt"));
	}
}
