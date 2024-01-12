package technology.rocketjump.undermount.mapgen.model.input;

import technology.rocketjump.undermount.mapgen.model.RockGroup;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GameMapGenerationParams {

	private final int targetWidth, targetHeight;
	private float heightMapVariance = 1f;
	private float heightMapRoughness = 0.6f;
	private float ratioOfMountains = 0.5f;

	private float requiredTotalOreRatio = 0.12f; // Ratio of ore to large mountain region tiles
	private float requiredTotalGemRatio = 0.01f; // Ratio of gems to large mountain region tiles
	private float requiredMushroomRatio = 0.04f; // Ratio of cave tiles to mushrooms

	private float ratioOfFruitingShrubs = 0.08f;

	private List<OreType> oreTypeList = new ArrayList<>();
	private float totalOfOreRequirementAmounts = 0f;
	private int minOreVeinLength = 5;
	private int maxOreVeinLength = 50;
	private float oreVeinWidthVariance = 4f;
	private Map<RockGroup, List<RockType>> rockTypeMap = new EnumMap<>(RockGroup.class);
	private Map<RockGroup, List<GemType>> gemTypeMap = new EnumMap<>(RockGroup.class);
	private final List<TreeType> treeTypes = new ArrayList<>();
	private final List<ShrubType> shrubTypes = new ArrayList<>();
	private final List<MushroomType> mushroomTypes = new ArrayList<>();

	public GameMapGenerationParams(int targetWidth, int targetHeight) {
		this.targetWidth = targetWidth;
		this.targetHeight = targetHeight;

		for (RockGroup rockGroup : RockGroup.values()) {
			rockTypeMap.put(rockGroup, new ArrayList<RockType>());
			gemTypeMap.put(rockGroup, new ArrayList<GemType>());
		}

	}

	public void addOreRequirement(OreType oreType) {
		oreTypeList.add(oreType);
		totalOfOreRequirementAmounts += oreType.getWeighting();
	}

	public List<OreType> getOreTypeList() {
		return oreTypeList;
	}

	public int getTargetWidth() {
		return targetWidth;
	}

	public int getTargetHeight() {
		return targetHeight;
	}

	public float getHeightMapVariance() {
		return heightMapVariance;
	}

	public void setHeightMapVariance(float heightMapVariance) {
		this.heightMapVariance = heightMapVariance;
	}

	public float getHeightMapRoughness() {
		return heightMapRoughness;
	}

	public void setHeightMapRoughness(float heightMapRoughness) {
		this.heightMapRoughness = heightMapRoughness;
	}

	public float getRatioOfMountains() {
		return ratioOfMountains;
	}

	public void setRatioOfMountains(float ratioOfMountains) {
		this.ratioOfMountains = ratioOfMountains;
	}

	public float getRequiredTotalOreRatio() {
		return requiredTotalOreRatio;
	}

	public float getRequiredTotalGemRatio() {
		return requiredTotalGemRatio;
	}

	public float getTotalOfOreRequirementAmounts() {
		return totalOfOreRequirementAmounts;
	}

	public int getMinOreVeinLength() {
		return minOreVeinLength;
	}

	public void setMinOreVeinLength(int minOreVeinLength) {
		this.minOreVeinLength = minOreVeinLength;
	}

	public int getMaxOreVeinLength() {
		return maxOreVeinLength;
	}

	public void setMaxOreVeinLength(int maxOreVeinLength) {
		this.maxOreVeinLength = maxOreVeinLength;
	}

	public float getOreVeinWidthVariance() {
		return oreVeinWidthVariance;
	}

	public float getRequiredMushroomRatio() {
		return requiredMushroomRatio;
	}

	public void setRequiredMushroomRatio(float requiredMushroomRatio) {
		this.requiredMushroomRatio = requiredMushroomRatio;
	}

	public List<MushroomType> getMushroomTypes() {
		return mushroomTypes;
	}

	public void addRockTypes(RockType... rockTypes) {
		for (RockType rockType : rockTypes) {
			rockTypeMap.get(rockType.getRockGroup()).add(rockType);
		}
	}

	public void addGemTypes(GemType... gemTypes) {
		for (GemType gemType : gemTypes) {
			gemTypeMap.get(gemType.getRockGroup()).add(gemType);
		}

	}

	public List<RockType> getRockTypes(RockGroup group) {
		return rockTypeMap.get(group);
	}

	public List<GemType> getGemTypes(RockGroup group) {
		return gemTypeMap.get(group);
	}

	public List<TreeType> getTreeTypes() {
		return treeTypes;
	}

	public List<ShrubType> getShrubTypes() {
		return shrubTypes;
	}

	public float getRatioOfFruitingShrubs() {
		return ratioOfFruitingShrubs;
	}

	public void setRatioOfFruitingShrubs(float ratioOfFruitingShrubs) {
		this.ratioOfFruitingShrubs = ratioOfFruitingShrubs;
	}
}
