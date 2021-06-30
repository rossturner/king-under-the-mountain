package technology.rocketjump.undermount.ui;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesGrowthStage;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.designation.TileDesignation;
import technology.rocketjump.undermount.mapping.tile.designation.TileDesignationDictionary;
import technology.rocketjump.undermount.rooms.RoomType;

import static technology.rocketjump.undermount.entities.model.EntityType.FURNITURE;
import static technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesGrowthStage.PlantSpeciesHarvestType.FORAGING;
import static technology.rocketjump.undermount.mapping.tile.TileExploration.EXPLORED;
import static technology.rocketjump.undermount.mapping.tile.TileRoof.OPEN;

// MODDING extract this enum to data-driven set of behaviours (when we know how to)
public enum GameInteractionMode {

	DEFAULT(null, null, null, false),
	DESIGNATE_MINING("mining", "MINING", mapTile -> ((!mapTile.getExploration().equals(EXPLORED) && mapTile.getDesignation() == null) || (mapTile.hasWall() && mapTile.getDesignation() == null)), true),
	DESIGNATE_CHOP_WOOD("logging", "CHOP_WOOD", mapTile -> (mapTile.getExploration().equals(EXPLORED) && mapTile.hasTree() && mapTile.getDesignation() == null), true),
	DESIGNATE_CLEAR_GROUND("spade", "CLEAR_GROUND", mapTile -> {
		Entity plant = mapTile.getPlant();
		if (plant != null && mapTile.getExploration().equals(EXPLORED) && mapTile.getDesignation() == null) {
			PlantEntityAttributes attributes = (PlantEntityAttributes) plant.getPhysicalEntityComponent().getAttributes();
			return attributes.getSpecies().getPlantType().removalJobTypeName.equals("CLEAR_GROUND");
		} else {
			return false;
		}
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
	DESIGNATE_ROOFING("roofing", "ADD_ROOF", mapTile -> mapTile.getRoof().equals(OPEN) && mapTile.getDesignation() == null, true),
	REMOVE_DESIGNATIONS("cancel", null, mapTile -> mapTile.getDesignation() != null, true),
	PLACE_ROOM("rooms", null, mapTile -> mapTile.getExploration().equals(EXPLORED) && !mapTile.hasWall() &&
			!mapTile.hasRoom() && !mapTile.hasDoorway() && !mapTile.isWaterSource() && !mapTile.getFloor().hasBridge(), true),
	PLACE_FURNITURE("zones", null, null, false),
	PLACE_DOOR("door", null, null, false),
	PLACE_WALLS("walls", null, null, true),
	PLACE_BRIDGE("bridge", null, mapTile -> mapTile.getExploration().equals(EXPLORED) && !mapTile.hasWall() &&
			!mapTile.hasDoorway() && !mapTile.hasRoom() && !mapTile.hasConstruction(), true),
	REMOVE_ROOMS("cancel", "REMOVE_ROOMS", MapTile::hasRoom, true),
	SET_JOB_PRIORITY("priority", null, null, true),
	REMOVE_CONSTRUCTIONS("cancel", "REMOVE_CONSTRUCTIONS", MapTile::hasConstruction, true),
	DECONSTRUCT("deconstruct", "DECONSTRUCT", mapTile -> {
		return mapTile.getFloor().hasBridge() || mapTile.hasDoorway() || mapTile.getEntities().stream().anyMatch(e -> e.getType().equals(FURNITURE)) ||
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

}
