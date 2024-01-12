package technology.rocketjump.undermount.mapgen.model.output;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.mapgen.model.FloorType;
import technology.rocketjump.undermount.mapgen.model.RockGroup;
import technology.rocketjump.undermount.mapgen.model.RoofType;
import technology.rocketjump.undermount.mapgen.model.input.*;

import java.util.Objects;
import java.util.Random;

public class GameMapTile {

	private final boolean isBorderTile;

	private float heightMapValue;
	private float noisyHeightValue;
	private final GridPoint2 position;

	private TileType tileType;
	private TileSubType tileSubType;
	private RoofType roofType = RoofType.Underground;
	private FloorType floorType = FloorType.Outdoor;

	private RockGroup rockGroup = RockGroup.None;
	private RockType rockType = null;
	private OreType ore;
	private GemType gem;

	private MapRegion region;
	private MapSubRegion subRegion;

	private TreeType treeType;
	private ShrubType shrubType;
	private MushroomType mushroomType;
	private boolean hasRiver;
	private float debugValue;

	public GameMapTile(GridPoint2 position, boolean isBorderTile) {
		this.position = position;
		this.isBorderTile = isBorderTile;
	}

	public GameMapTile(GameMapTile other) {
		this.position = other.position.cpy();
		this.isBorderTile = other.isBorderTile;
		// Remember to clone any non-primitives
		this.floorType = other.floorType;
		this.roofType = other.roofType;
		this.tileType = other.tileType;
		this.tileSubType = other.tileSubType;
		this.heightMapValue = other.heightMapValue;
		this.noisyHeightValue = other.noisyHeightValue;
		this.debugValue = other.debugValue;
		this.rockGroup = other.rockGroup;
		this.rockType = other.rockType;
		this.hasRiver = other.hasRiver;
		this.treeType = other.treeType;

		if (other.region != null) {
			this.region = other.region;
			other.region.remove(other);
			this.region.add(this, null);

			if (other.subRegion != null) {
				this.subRegion = other.subRegion;
				other.subRegion.remove(other);
				this.subRegion.add(this);
			}
		}
	}

	public float getHeightMapValue() {
		return heightMapValue;
	}

	public void setHeightMapValue(float height) {
		this.heightMapValue = height;
	}

	public RoofType getRoofType() {
		return roofType;
	}

	public void setRoofType(RoofType roofType) {
		this.roofType = roofType;
	}


	public FloorType getFloorType() {
		return floorType;
	}

	public void setFloorType(FloorType floorType) {
		this.floorType = floorType;
	}

	public void setAsCave() {
		this.tileType = TileType.MOUNTAIN;
		this.roofType = RoofType.Underground;
		this.floorType = FloorType.Rock;
		this.tileSubType = TileSubType.STONE_FLOOR_CAVE;
	}

	public void setAsMountain() {
		this.tileType = TileType.MOUNTAIN;
		this.roofType = RoofType.Underground;
		this.floorType = FloorType.None;
		this.tileSubType = TileSubType.MOUNTAIN_ROCK;
	}

	public void setAsOutside() {
		this.tileType = TileType.OUTSIDE;
		this.roofType = RoofType.Outside;
		this.floorType = FloorType.Outdoor;
	}

	public TileType getTileType() {
		return tileType;
	}

	public GridPoint2 getPosition() {
		return position;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GameMapTile that = (GameMapTile) o;
		return Objects.equals(position, that.position);
	}

	@Override
	public int hashCode() {
		return Objects.hash(position);
	}

	public boolean isBorderTile() {
		return isBorderTile;
	}

	public MapRegion getRegion() {
		return region;
	}

	public void setRegion(MapRegion region) {
		this.region = region;
	}

	public boolean hasOre() {
		return ore != null;
	}
	
	public void setOre(OreType ore, Random random) {
		if (this.ore == null) {
			this.ore = ore;
		} else {
			// 50/50 chance to override existing ore
			if (random.nextBoolean()) {
				this.ore = ore;
			}
		}
	}

	public OreType getOre() {
		return ore;
	}

	public void setGem(GemType gem, Random random) {
		if (this.gem == null) {
			this.gem = gem;
		} else {
			// 50/50 chance to override existing gem
			if (random.nextBoolean()) {
				this.gem = gem;
			}
		}
	}

	public GemType getGem() {
		return gem;
	}

	public float getNoisyHeightValue() {
		return noisyHeightValue;
	}

	public void setNoisyHeightValue(float noisyHeightValue) {
		this.noisyHeightValue = noisyHeightValue;
		this.tileSubType = pickTileSubType();
	}

	public MapSubRegion getSubRegion() {
		return subRegion;
	}

	public void setSubRegion(MapSubRegion subRegion) {
		this.subRegion = subRegion;
	}

	public TileSubType getTileSubType() {
		return tileSubType;
	}

	public void setTileSubType(TileSubType tileSubType) {
		this.tileSubType = tileSubType;
	}

	private TileSubType pickTileSubType() {
		TileSubType subRegionType;
		if (this.getTileType().equals(TileType.MOUNTAIN)) {
			if (this.getFloorType().equals(FloorType.None)) {
				subRegionType = TileSubType.MOUNTAIN_ROCK;
			} else {
				subRegionType = TileSubType.STONE_FLOOR_CAVE;
			}
		} else {
			subRegionType = decideOnOutdoorBiome();
		}
		return subRegionType;
	}

	private TileSubType decideOnOutdoorBiome() {
		if (noisyHeightValue < 0.25f) {
			return TileSubType.FOREST;
		} else if (noisyHeightValue < 0.45f) {
			return TileSubType.GRASSLAND;
		} else if (noisyHeightValue < 0.65f) {
			return TileSubType.PLAINS;
		} else {
			return TileSubType.TUNDRA;
		}
	}

	public boolean hasTree() {
		return treeType != null;
	}

	public TreeType getTree() {
		return treeType;
	}

	public void setTree(TreeType treeType) {
		this.treeType = treeType;
	}

	public boolean hasRiver() {
		return hasRiver;
	}

	public void setRiver(boolean river) {
		this.hasRiver = river;
		if (river) {
			this.tileType = TileType.RIVER;
		} else {
			this.tileType = TileType.OUTSIDE;
		}
	}

	public boolean isNavigableByRiver() {
		return tileType.equals(TileType.OUTSIDE);
	}

	public Vector2 getWorldPositionOfCenter() {
		return new Vector2(position.x + 0.5f, position.y + 0.5f);
	}

	public RockGroup getRockGroup() {
		return rockGroup;
	}

	public void setRockGroup(RockGroup rockGroup) {
		this.rockGroup = rockGroup;
	}

	public RockType getRockType() {
		return rockType;
	}

	public void setRockType(RockType rockType) {
		this.rockType = rockType;
	}

	public void setDebugValue(float debugValue) {
		this.debugValue = debugValue;
	}

	public float getDebugValue() {
		return debugValue;
	}

	public ShrubType getShrubType() {
		return shrubType;
	}

	public void setShrubType(ShrubType shrubType) {
		this.shrubType = shrubType;
	}

	public boolean hasShrub() {
		return this.shrubType != null;
	}

	public boolean hasMushroom() {
		return this.mushroomType != null;
	}

	public MushroomType getMushroomType() {
		return mushroomType;
	}

	public void setMushroom(MushroomType mushroomType) {
		this.mushroomType = mushroomType;
	}
}
