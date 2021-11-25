package technology.rocketjump.undermount.mapping.tile;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.doors.Doorway;
import technology.rocketjump.undermount.entities.behaviour.creature.CorpseBehaviour;
import technology.rocketjump.undermount.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.DoorwayEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesType;
import technology.rocketjump.undermount.mapping.tile.designation.Designation;
import technology.rocketjump.undermount.mapping.tile.floor.FloorOverlap;
import technology.rocketjump.undermount.mapping.tile.floor.OverlapLayout;
import technology.rocketjump.undermount.mapping.tile.floor.TileFloor;
import technology.rocketjump.undermount.mapping.tile.layout.WallConstructionLayout;
import technology.rocketjump.undermount.mapping.tile.layout.WallLayout;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoof;
import technology.rocketjump.undermount.mapping.tile.underground.ChannelLayout;
import technology.rocketjump.undermount.mapping.tile.underground.PipeLayout;
import technology.rocketjump.undermount.mapping.tile.underground.UnderTile;
import technology.rocketjump.undermount.mapping.tile.wall.Wall;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
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

	private final Map<Long, Entity> entities = new ConcurrentHashMap<>(); // Concurrent for access by PathfindingTask
	private final Map<Long, ParticleEffectInstance> particleEffects = new HashMap<>();

	private TileRoof roof;
	private Wall wall = null;
	private Doorway doorway = null;
	private final Deque<TileFloor> floors = new ArrayDeque<>();
	private UnderTile underTile;

	private Designation designation = null;
	private RoomTile roomTile = null;
	private Set<Zone> zones = new HashSet<>();
	private Construction construction = null;
	private TileExploration exploration = UNEXPLORED;

	public static final MapTile NULL_TILE = new MapTile(-1L, 0, 0, FloorType.NULL_FLOOR, GameMaterial.NULL_MATERIAL);

	public MapTile(long seed, int tileX, int tileY, FloorType floorType, GameMaterial floorMaterial) {
		this.seed = seed;
		this.tilePosition = new GridPoint2(tileX, tileY);
		floors.push(new TileFloor(floorType, floorMaterial));
		this.roof = new TileRoof();

		if (GlobalSettings.MAP_REVEALED) {
			exploration = EXPLORED;
		}
	}

	public void update(TileNeighbours neighbours, MapVertex[] vertexNeighboursOfCell, MessageDispatcher messageDispatcher) {
		if (hasWall()) {
			WallLayout newLayout = new WallLayout(neighbours);
			wall.setTrueLayout(newLayout);
		}
		if (hasChannel()) {
			ChannelLayout newLayout = new ChannelLayout(neighbours);
			getUnderTile().setChannelLayout(newLayout);
		}
		if (hasPipe()) {
			PipeLayout newPipeLayout = new PipeLayout(neighbours);
			MechanismEntityAttributes attributes = (MechanismEntityAttributes) underTile.getPipeEntity().getPhysicalEntityComponent().getAttributes();
			if (!newPipeLayout.equals(attributes.getPipeLayout())) {
				attributes.setPipeLayout(newPipeLayout);
				if (messageDispatcher != null) {
					messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, underTile.getPipeEntity());
				}
			}
		}

		// Always update FloorOverlaps for all tiles
		Set<FloorOverlap> overlaps = new TreeSet<>(new FloorType.FloorDefinitionComparator());
		int thisLayer = getFloor().getFloorType().getLayer();
		if (this.hasWall()) {
			thisLayer = Integer.MIN_VALUE;
		}
		for (MapTile neighbour : neighbours.values()) {
			if (neighbour.hasFloor() && neighbour.getFloor().getFloorType().getLayer() > thisLayer) {
				OverlapLayout layout = OverlapLayout.fromNeighbours(neighbours, neighbour.getFloor().getFloorType());
				overlaps.add(new FloorOverlap(layout, neighbour.getFloor().getFloorType(), neighbour.getFloor().getMaterial(), vertexNeighboursOfCell));
			}
		}

		getFloor().getOverlaps().clear();
		// For sort
		for (FloorOverlap overlap : overlaps) {
			getFloor().getOverlaps().add(overlap);
		}

		if (getFloor().getFloorType().isUseMaterialColor()) {
			Color floorMaterialColor = getFloor().getMaterial().getColor();
			getFloor().vertexColors[0] = floorMaterialColor;
			getFloor().vertexColors[1] = floorMaterialColor;
			getFloor().vertexColors[2] = floorMaterialColor;
			getFloor().vertexColors[3] = floorMaterialColor;
		} else {
			getFloor().vertexColors[0] = getFloor().getFloorType().getColorForHeightValue(vertexNeighboursOfCell[0].getHeightmapValue());
			getFloor().vertexColors[1] = getFloor().getFloorType().getColorForHeightValue(vertexNeighboursOfCell[1].getHeightmapValue());
			getFloor().vertexColors[2] = getFloor().getFloorType().getColorForHeightValue(vertexNeighboursOfCell[2].getHeightmapValue());
			getFloor().vertexColors[3] = getFloor().getFloorType().getColorForHeightValue(vertexNeighboursOfCell[3].getHeightmapValue());
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
		} else if (getFloor().isRiverTile() && !getFloor().isBridgeNavigable()) {
			if (startingPoint != null && startingPoint.getFloor().isRiverTile()) {
				return true; // Can navigate from a river tile to another river tile
			} else {
				return false; // Otherwise rivers are not navigable
			}
		} else if (hasChannel() && !getFloor().isBridgeNavigable()) {
			if (startingPoint != null && startingPoint.hasChannel()) {
				return true; // Can navigate from a channel tile to another channel tile
			} else {
				return false; // Otherwise channels are not navigable
			}
		} else if (!hasWall() && !hasTree()) {
			if (getFloor().hasBridge() && !getFloor().isBridgeNavigable()) {
				return false;
			}
			for (Entity entity : getEntities()) {
				if (entity.getType().equals(EntityType.FURNITURE)) {
					if (entity.getPhysicalEntityComponent().getAttributes() instanceof DoorwayEntityAttributes) {
						if (entity.getBehaviourComponent() instanceof CreatureBehaviour) {
							// creatures can't path through doors
							return false;
						}
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
		this.roof = roof;
	}

	public void addWall(TileNeighbours neighbours, GameMaterial material, WallType wallType) {
		this.wall = new Wall(new WallLayout(neighbours), wallType, material);
	}

	public long getSeed() {
		return seed;
	}

	public TileFloor getFloor() {
		return floors.peek();
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
		if (hasPipe()) {
			Entity pipeEntity = underTile.getPipeEntity();
			if (pipeEntity.getId() == entityId) {
				underTile.setPipeEntity(null);
				return pipeEntity;
			}
		}
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

	public Designation getDesignation() {
		return designation;
	}

	public void setDesignation(Designation designation) {
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
			if (entity.getType().equals(EntityType.CREATURE) && entity.getBehaviourComponent() instanceof CorpseBehaviour) {
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
		return !(this.hasWall() || this.hasDoorway() || this.hasConstruction() || this.getFloor().isRiverTile() || this.hasChannel());
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

	public Entity getFirstCorpse() {
		for (Entity entity : entities.values()) {
			if (entity.getType().equals(EntityType.CREATURE) && entity.getBehaviourComponent() instanceof CorpseBehaviour) {
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
		return getFloor().getRiverTile() != null;
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
		if (getFloor().isRiverTile()) {
			return RegionType.RIVER;
		} else if (hasWall()) {
			return RegionType.WALL;
		} else if (hasChannel()) {
			return RegionType.CHANNEL;
		} else {
			return RegionType.GENERIC;
		}
	}

	public Set<Zone> getZones() {
		return zones;
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

		JSONObject roofJson = new JSONObject(true);
		roof.writeTo(roofJson, savedGameStateHolder);
		asJson.put("roof", roofJson);

		if (!floors.isEmpty()) {
			JSONArray floorsArray = new JSONArray();
			Iterator<TileFloor> descendingIterator = floors.descendingIterator();
			while (descendingIterator.hasNext()) {
				TileFloor floor = descendingIterator.next();
				JSONObject floorJson = new JSONObject(true);
				floor.writeTo(floorJson, savedGameStateHolder);
				floorsArray.add(floorJson);
			}
			asJson.put("floors", floorsArray);
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

		if (underTile != null) {
			JSONObject undertileJson = new JSONObject(true);
			underTile.writeTo(undertileJson, savedGameStateHolder);
			asJson.put("underTile", undertileJson);
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

		Object roofJson = asJson.get("roof");
		if (roofJson instanceof JSONObject) {
			this.roof.readFrom((JSONObject) roofJson, savedGameStateHolder, relatedStores);
		} else {
			// Old save version
			throw new InvalidSaveException("Map tile roof is old version");
		}

		JSONArray floorsJson = asJson.getJSONArray("floors");
		this.floors.clear();
		if (floorsJson != null) {
			for (int cursor = 0; cursor < floorsJson.size(); cursor++) {
				JSONObject floorJson = floorsJson.getJSONObject(cursor);
				TileFloor floor = new TileFloor();
				floor.readFrom(floorJson, savedGameStateHolder, relatedStores);
				this.floors.push(floor);
			}
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

		JSONObject underTileJson = asJson.getJSONObject("underTile");
		if (underTileJson != null) {
			this.underTile = new UnderTile();
			this.underTile.readFrom(underTileJson, savedGameStateHolder, relatedStores);
		}

		String designationName = asJson.getString("designation");
		if (designationName != null) {
			this.designation = relatedStores.designationDictionary.getByName(designationName);
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

	public Map<Long, ParticleEffectInstance> getParticleEffects() {
		return particleEffects;
	}

	public void replaceFloor(TileFloor newFloor) {
		this.floors.push(newFloor);
	}

	public void popFloor() {
		this.floors.pop();
	}

	public UnderTile getUnderTile() {
		return underTile;
	}

	public UnderTile getOrCreateUnderTile() {
		if (underTile == null) {
			underTile = new UnderTile();
		}
		return underTile;
	}

	public void setUnderTile(UnderTile underTile) {
		this.underTile = underTile;
	}

	public boolean hasChannel() {
		return underTile != null && underTile.getChannelLayout() != null;
	}

	public boolean hasPipe() {
		return underTile != null && underTile.getPipeEntity() != null;
	}

	public Deque<TileFloor> getAllFloors() {
		return floors;
	}

	public boolean hasPowerMechanism() {
		return underTile != null && underTile.getPowerMechanismEntity() != null;
	}

	public enum RegionType {
		RIVER, WALL, CHANNEL, GENERIC
	}
}
