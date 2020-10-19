package technology.rocketjump.undermount.mapping.tile;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.doors.Doorway;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.DoorwayEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesType;
import technology.rocketjump.undermount.mapping.tile.designation.TileDesignation;
import technology.rocketjump.undermount.mapping.tile.floor.FloorOverlap;
import technology.rocketjump.undermount.mapping.tile.floor.OverlapLayout;
import technology.rocketjump.undermount.mapping.tile.floor.TileFloor;
import technology.rocketjump.undermount.mapping.tile.layout.WallConstructionLayout;
import technology.rocketjump.undermount.mapping.tile.layout.WallLayout;
import technology.rocketjump.undermount.mapping.tile.wall.Wall;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;
import technology.rocketjump.undermount.rooms.RoomTile;
import technology.rocketjump.undermount.rooms.constructions.Construction;
import technology.rocketjump.undermount.rooms.constructions.ConstructionType;
import technology.rocketjump.undermount.rooms.constructions.WallConstruction;
import technology.rocketjump.undermount.zones.Zone;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static technology.rocketjump.undermount.entities.model.Entity.NULL_ENTITY;
import static technology.rocketjump.undermount.mapping.tile.TileExploration.EXPLORED;
import static technology.rocketjump.undermount.mapping.tile.TileExploration.UNEXPLORED;

/**
 * This class represents each tile on the 2D area map
 */
public class MapTile implements Persistable {

	private final long seed;
	private final GridPoint2 tilePosition;
	private int regionId = -1; // -1 for unset

	private Map<Long, Entity> entities = new ConcurrentHashMap<>(); // Concurrent for access by PathfindingTask

	private TileRoof roof = TileRoof.MOUNTAIN_ROOF;
	private GameMaterial roofMaterial = null;
	private TileFloor floor;
	private Wall wall = null;
	private Doorway doorway = null;

	private TileDesignation designation = null;
	private RoomTile roomTile = null;
	private Set<Zone> zones = new HashSet<>();
	private Construction construction = null;
	private TileExploration exploration = UNEXPLORED;

	public MapTile(long seed, int tileX, int tileY, FloorType floorType, GameMaterial floorMaterial) {
		this.seed = seed;
		this.tilePosition = new GridPoint2(tileX, tileY);
		this.floor = new TileFloor(floorType, floorMaterial);

		if (GlobalSettings.MAP_REVEALED) {
			exploration = EXPLORED;
		}
	}

	public void update(TileNeighbours neighbours, MapVertex[] vertexNeighboursOfCell) {
		if (hasWall()) {
			WallLayout newLayout = new WallLayout(neighbours);
			wall.setTrueLayout(newLayout);
		} else if (hasFloor()) {
			Set<FloorOverlap> overlaps = new TreeSet<>(new FloorType.FloorDefinitionComparator());
			for (MapTile neighbour : neighbours.values()) {
				if (neighbour.hasFloor() && neighbour.getFloor().getFloorType().getLayer()  > this.floor.getFloorType().getLayer()) {
					OverlapLayout layout = OverlapLayout.fromNeighbours(neighbours, neighbour.getFloor().getFloorType());
					overlaps.add(new FloorOverlap(layout, neighbour.getFloor().getFloorType(), neighbour.getFloor().getMaterial(), vertexNeighboursOfCell));
				}
			}

			floor.getOverlaps().clear();
			// For sort
			for (FloorOverlap overlap : overlaps) {
				floor.getOverlaps().add(overlap);
			}

			if (floor.getFloorType().isUseMaterialColor()) {
				Color floorMaterialColor = floor.getMaterial().getColor();
				floor.vertexColors[0] = floorMaterialColor;
				floor.vertexColors[1] = floorMaterialColor;
				floor.vertexColors[2] = floorMaterialColor;
				floor.vertexColors[3] = floorMaterialColor;
			} else {
				floor.vertexColors[0] = floor.getFloorType().getColorForHeightValue(vertexNeighboursOfCell[0].getHeightmapValue());
				floor.vertexColors[1] = floor.getFloorType().getColorForHeightValue(vertexNeighboursOfCell[1].getHeightmapValue());
				floor.vertexColors[2] = floor.getFloorType().getColorForHeightValue(vertexNeighboursOfCell[2].getHeightmapValue());
				floor.vertexColors[3] = floor.getFloorType().getColorForHeightValue(vertexNeighboursOfCell[3].getHeightmapValue());
			}

		}

		if (hasConstruction()) {
			if (construction.getConstructionType().equals(ConstructionType.WALL_CONSTRUCTION)) {
				WallConstruction wallConstruction = (WallConstruction) construction;
				wallConstruction.setLayout(new WallConstructionLayout(neighbours));
			}
		}
	}

	public Collection<Entity> getEntities() {
		return entities.values();
	}

	public List<Long> getEntityIds() {
		return new ArrayList<>(entities.keySet());
	}

	public TileRoof getRoof() {
		return roof;
	}

	public void setRoof(TileRoof roof) {
		this.roof = roof;
	}

	public boolean hasWall() {
		return this.wall != null;
	}

	public boolean isNavigable() {
		return isNavigable(null);
	}

	public boolean isNavigable(MapTile startingPoint) {
		if (this.equals(startingPoint)) {
			// Can always navigate if this tile is the starting point
			return true;
		} else if (floor.isRiverTile() && !floor.isBridgeNavigable()) {
			if (startingPoint != null && startingPoint.getFloor().isRiverTile()) {
				return true; // Can navigate from a river tile to another river tile
			} else {
				return false; // Otherwise rivers are not navigable
			}
		} else if (!hasWall() && !hasTree()) {
			if (floor.hasBridge() && !floor.isBridgeNavigable()) {
				return false;
			}
			for (Entity entity : getEntities()) {
				if (entity.getType().equals(EntityType.FURNITURE)) {
					if (entity.getPhysicalEntityComponent().getAttributes() instanceof DoorwayEntityAttributes) {
						continue;
					}
					FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					if (attributes.getFurnitureType().getFurnitureCategory().isBlocksMovement()) {
						// This piece of furniture blocks movement but if it is also in the startingPoint, ignore it
						if (startingPoint != null) {
							boolean startingPointHasSameEntity = startingPoint.getEntity(entity.getId()) != null;
							if (startingPointHasSameEntity) {
								continue; // go on to next entity
							} else {
								// Not also in starting point, so this blocks movement
								return false;
							}
						} else {
							return false;
						}
					}
				} else if (entity.getType().equals(EntityType.ITEM)) {
					ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					if (attributes.getItemType().blocksMovement()) {
						return false;
					}
				}
			}
			return true;
		} else if (startingPoint != null && startingPoint.hasWall() && this.hasWall()) {
			// FIXME Possibly bug-prone hack allowing navigation through walls if starting inside one
			return true;
		} else {
			return false;
		}
	}

	public boolean hasFloor() {
		return this.wall == null;
	}

	public Wall getWall() {
		return wall;
	}

	public void setWall(Wall wall, TileRoof roof) {
		this.wall = wall;
		setRoof(roof);
	}

	public void addWall(TileNeighbours neighbours, GameMaterial material, WallType wallType) {
		this.wall = new Wall(new WallLayout(neighbours), wallType, material);
	}

	public long getSeed() {
		return seed;
	}

	public TileFloor getFloor() {
		return floor;
	}

	public int getTileX() {
		return tilePosition.x;
	}

	public int getTileY() {
		return tilePosition.y;
	}

	/**
	 * This method returns a world position representing the center of a tile
	 */
	public Vector2 getWorldPositionOfCenter() {
		return new Vector2(0.5f + tilePosition.x, 0.5f + tilePosition.y);
	}

	public Entity removeEntity(long entityId) {
		return entities.remove(entityId);
	}

	public void addEntity(Entity entity) {
		entities.put(entity.getId(), entity);
	}

	public boolean hasPlant() {
		for (Entity entity : entities.values()) {
			if (entity.getType().equals(EntityType.PLANT)) {
				return true;
			}
		}
		return false;
	}

	public Entity getPlant() {
		for (Entity entity : entities.values()) {
			if (entity.getType().equals(EntityType.PLANT)) {
				return entity;
			}
		}
		return null;
	}

	public boolean hasTree() {
		// Currently stuff can go behind trees, to disable this, check for trees in tiles to south as well
		for (Entity entity : entities.values()) {
			if (entity.getType().equals(EntityType.PLANT)) {
				PlantEntityAttributes attributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getSpecies().getPlantType().equals(PlantSpeciesType.TREE) || attributes.getSpecies().getPlantType().equals(PlantSpeciesType.MUSHROOM_TREE)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasShrub() {
		for (Entity entity : entities.values()) {
			if (entity.getType().equals(EntityType.PLANT)) {
				PlantEntityAttributes attributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getSpecies().getPlantType().equals(PlantSpeciesType.SHRUB)) {
					return true;
				}
			}
		}
		return false;
	}

	public TileDesignation getDesignation() {
		return designation;
	}

	public void setDesignation(TileDesignation designation) {
		this.designation = designation;
	}

	public boolean hasRoom() {
		return roomTile != null;
	}

	public RoomTile getRoomTile() {
		return roomTile;
	}

	public void setRoomTile(RoomTile roomTile) {
		this.roomTile = roomTile;
	}

	public GridPoint2 getTilePosition() {
		return tilePosition;
	}

	public boolean hasItem() {
		for (Entity entity : entities.values()) {
			if (entity.getType().equals(EntityType.ITEM)) {
				return true;
			}
		}
		return false;
	}

	public Entity getItemMatching(ItemEntityAttributes attributesToMatch) {
		for (Entity entity : getEntities()) {
			if (entity.getType().equals(EntityType.ITEM)) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getItemType().equals(attributesToMatch.getItemType())) {
					for (GameMaterialType gameMaterialType : attributes.getItemType().getMaterialTypes()) {
						if (attributes.getMaterial(gameMaterialType).equals(attributesToMatch.getMaterial(gameMaterialType))) {
							return entity;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Empty of entities such as items and plants
	 */
	public boolean isEmpty() {
		if (!this.isEmptyExceptEntities()) {
			return false;
		}
		for (Entity entity : this.entities.values()) {
			if (entity.getType().equals(EntityType.ITEM) || entity.getType().equals(EntityType.PLANT) || entity.getType().equals(EntityType.FURNITURE)) {
				return false;
			}
		}
		return true;
	}

	public boolean isEmptyExceptItemsAndPlants() {
		if (!this.isEmptyExceptEntities()) {
			return false;
		}
		for (Entity entity : this.entities.values()) {
			if (entity.getType().equals(EntityType.FURNITURE)) {
				return false;
			}
		}
		return true;
	}

	public boolean isEmptyExceptEntities() {
		return !(this.hasWall() || this.hasDoorway() || this.hasConstruction() || this.floor.isRiverTile());
	}

	public Entity getEntity(long entityId) {
		return entities.get(entityId);
	}

	public Entity getFirstItem() {
		for (Entity entity : entities.values()) {
			if (entity.getType().equals(EntityType.ITEM)) {
				return entity;
			}
		}
		return null;
	}

	public boolean hasDoorway() {
		return doorway != null;
	}

	public Doorway getDoorway() {
		return doorway;
	}

	public void setDoorway(Doorway doorway) {
		this.doorway = doorway;
	}

	public boolean hasConstruction() {
		return construction != null;
	}

	public Construction getConstruction() {
		return construction;
	}

	public void setConstruction(Construction construction) {
		this.construction = construction;
	}

	public boolean isWaterSource() {
		return floor.getRiverTile() != null;
	}

	public int getRegionId() {
		return regionId;
	}

	public void setRegionId(int regionId) {
		this.regionId = regionId;
	}

	public void addToZone(Zone zone) {
		this.zones.add(zone);
	}

	public void removeFromZone(Zone zone) {
		this.zones.remove(zone);
	}

	public RegionType getRegionType() {
		if (floor.isRiverTile()) {
			return RegionType.RIVER;
		} else if (hasWall()) {
			return RegionType.WALL;
		} else {
			return RegionType.GENERIC;
		}
	}

	public Set<Zone> getZones() {
		return zones;
	}

	public GameMaterial getRoofMaterial() {
		return roofMaterial;
	}

	public void setRoofMaterial(GameMaterial roofMaterial) {
		this.roofMaterial = roofMaterial;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		// Don't need to check if already in state holder?
		JSONObject asJson = new JSONObject(true);

		asJson.put("regionId", regionId);

		if (!entities.isEmpty()) {
			JSONArray entities = new JSONArray();
			entities.addAll(this.entities.keySet());
			asJson.put("entities", entities);
		}

		if (!roof.equals(TileRoof.MOUNTAIN_ROOF)) {
			asJson.put("roof", roof.name());
		}
		if (roofMaterial != null) {
			asJson.put("roofMaterial", roofMaterial.getMaterialName());
		}

		if (floor != null) {
			JSONObject floorJson = new JSONObject(true);
			floor.writeTo(floorJson, savedGameStateHolder);
			asJson.put("floor", floorJson);
		}

		if (wall != null) {
			JSONObject wallJson = new JSONObject(true);
			wall.writeTo(wallJson, savedGameStateHolder);
			asJson.put("wall", wallJson);
		}

		if (doorway != null) {
			JSONObject doorwayJson = new JSONObject(true);
			doorway.writeTo(doorwayJson, savedGameStateHolder);
			asJson.put("door", doorwayJson);
		}

		if (designation != null) {
			asJson.put("designation", designation.getDesignationName());
		}

		if (roomTile != null) {
			JSONObject roomTileJson = new JSONObject(true);
			roomTile.writeTo(roomTileJson, savedGameStateHolder);
			asJson.put("roomTile", roomTileJson);
		}

		if (!zones.isEmpty()) {
			JSONArray zonesJson = new JSONArray();
			for (Zone zone : zones) {
				zone.writeTo(savedGameStateHolder);
				zonesJson.add(zone.getZoneId());
			}
			asJson.put("zones", zonesJson);
		}

		if (construction != null) {
			construction.writeTo(savedGameStateHolder);
			asJson.put("construction", construction.getId());
		}

		if (!exploration.equals(EXPLORED)) {
			asJson.put("exploration", exploration.name());
		}

		savedGameStateHolder.tiles.put(tilePosition, this);
		savedGameStateHolder.tileJson.add(asJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.regionId = asJson.getIntValue("regionId");

		JSONArray entityIds = asJson.getJSONArray("entities");
		if (entityIds != null) {
			for (int cursor = 0; cursor < entityIds.size(); cursor++) {
				this.entities.put(entityIds.getLongValue(cursor), NULL_ENTITY); // Placing null for now for entities to be added later
			}
		}

		this.roof = EnumParser.getEnumValue(asJson, "roof", TileRoof.class, TileRoof.MOUNTAIN_ROOF);
		String roofMaterialName = asJson.getString("roofMaterial");
		if (roofMaterialName != null) {
			this.roofMaterial = relatedStores.gameMaterialDictionary.getByName(roofMaterialName);
			if (this.roofMaterial == null) {
				throw new InvalidSaveException("Could not find material with name " + roofMaterialName);
			}
		}

		JSONObject floorJson = asJson.getJSONObject("floor");
		if (floorJson != null) {
			this.floor = new TileFloor();
			this.floor.readFrom(floorJson, savedGameStateHolder, relatedStores);
		}

		JSONObject wallJson = asJson.getJSONObject("wall");
		if (wallJson != null) {
			this.wall = new Wall();
			this.wall.readFrom(wallJson, savedGameStateHolder, relatedStores);
		}

		JSONObject doorJson = asJson.getJSONObject("door");
		if (doorJson != null) {
			this.doorway = new Doorway();
			this.doorway.readFrom(doorJson, savedGameStateHolder, relatedStores);
		}

		String designationName = asJson.getString("designation");
		if (designationName != null) {
			this.designation = relatedStores.tileDesignationDictionary.getByName(designationName);
			if (this.designation == null) {
				throw new InvalidSaveException("Could not find tile designation by name " + designationName);
			}
		}

		JSONObject roomTileJson = asJson.getJSONObject("roomTile");
		if (roomTileJson != null) {
			roomTile = new RoomTile();
			roomTile.readFrom(roomTileJson, savedGameStateHolder, relatedStores);
			roomTile.setTile(this);
		}

		JSONArray zones = asJson.getJSONArray("zones");
		if (zones != null) {
			for (int cursor = 0; cursor < zones.size(); cursor++) {
				long zoneId = zones.getLongValue(cursor);
				Zone zone = savedGameStateHolder.zones.get(zoneId);
				if (zone == null) {
					throw new InvalidSaveException("Could not find zone by ID " + zoneId);
				} else {
					this.zones.add(zone);
				}
			}
		}

		Long constructionId = asJson.getLong("construction");
		if (constructionId != null) {
			this.construction = savedGameStateHolder.constructions.get(constructionId);
			if (this.construction == null) {
				throw new InvalidSaveException("Could not find construction by ID " + constructionId);
			}
		}

		this.exploration = EnumParser.getEnumValue(asJson, "exploration", TileExploration.class, TileExploration.EXPLORED);

		savedGameStateHolder.tiles.put(tilePosition, this);
	}

	public TileExploration getExploration() {
		return exploration;
	}

	public void setExploration(TileExploration exploration) {
		this.exploration = exploration;
	}

	public enum RegionType {
		RIVER, WALL, GENERIC
	}
}
