package technology.rocketjump.undermount.entities.model.physical.item;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.entities.tags.Tag;
import technology.rocketjump.undermount.jobs.model.CraftingType;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.misc.Name;
import technology.rocketjump.undermount.misc.SequentialId;
import technology.rocketjump.undermount.rooms.StockpileGroup;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemType {

	public static final double DEFAULT_HOURS_FOR_ITEM_TO_BECOME_UNUSED = 12.0;

	@SequentialId
	private long itemTypeId;
	@Name
	private String itemTypeName;
	private ItemGroup itemGroup;

	private int maxStackSize = 1;
	private int maxHauledAtOnce; // or requiresHauling
	private List<GameMaterialType> materialTypes = new ArrayList<>();
	private GameMaterialType primaryMaterialType;

	private ItemHoldPosition holdPosition = ItemHoldPosition.IN_FRONT;
	private boolean impedesMovement = false;
	private boolean blocksMovement = false;
	private boolean equippedWhileWorkingOnJob = true; // Might need replacing with "can be shown hauling" property
	private double hoursInInventoryUntilUnused = DEFAULT_HOURS_FOR_ITEM_TO_BECOME_UNUSED;

	private List<String> relatedCraftingTypeNames = new ArrayList<>();
	@JsonIgnore
	private List<CraftingType> relatedCraftingTypes = new ArrayList<>();

	private String stockpileGroupName;
	@JsonIgnore
	private StockpileGroup stockpileGroup;

	private Map<String, List<String>> tags = new HashMap<>();
	@JsonIgnore
	private List<Tag> processedTags = new ArrayList<>();

	private String placementSoundAssetName;
	@JsonIgnore
	private SoundAsset placementSoundAsset;

	private String consumeSoundAssetName;
	@JsonIgnore
	private SoundAsset consueSoundAsset;

	public long getItemTypeId() {
		return itemTypeId;
	}

	public void setItemTypeId(long itemTypeId) {
		this.itemTypeId = itemTypeId;
	}

	public String getItemTypeName() {
		return itemTypeName;
	}

	public void setItemTypeName(String itemTypeName) {
		this.itemTypeName = itemTypeName;
	}

	public int getMaxStackSize() {
		return maxStackSize;
	}

	public void setMaxStackSize(int maxStackSize) {
		this.maxStackSize = maxStackSize;
	}

	public int getMaxHauledAtOnce() {
		return maxHauledAtOnce;
	}

	public void setMaxHauledAtOnce(int maxHauledAtOnce) {
		this.maxHauledAtOnce = maxHauledAtOnce;
	}

	public boolean impedesMovement() {
		return impedesMovement;
	}

	public void setImpedesMovement(boolean impedesMovement) {
		this.impedesMovement = impedesMovement;
	}

	public boolean blocksMovement() {
		return blocksMovement;
	}

	public void setBlocksMovement(boolean blocksMovement) {
		this.blocksMovement = blocksMovement;
	}

	public ItemHoldPosition getHoldPosition() {
		return holdPosition;
	}

	public void setHoldPosition(ItemHoldPosition holdPosition) {
		this.holdPosition = holdPosition;
	}

	public String getConsumeSoundAssetName() {
		return consumeSoundAssetName;
	}

	public void setConsumeSoundAssetName(String consumeSoundAssetName) {
		this.consumeSoundAssetName = consumeSoundAssetName;
	}

	public SoundAsset getConsueSoundAsset() {
		return consueSoundAsset;
	}

	public void setConsueSoundAsset(SoundAsset consueSoundAsset) {
		this.consueSoundAsset = consueSoundAsset;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ItemType itemType = (ItemType) o;
		return itemTypeId == itemType.itemTypeId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(itemTypeId);
	}

	@Override
	public String toString() {
		return itemTypeName;
	}

	public List<GameMaterialType> getMaterialTypes() {
		return materialTypes;
	}

	public void setMaterialTypes(List<GameMaterialType> materialTypes) {
		this.materialTypes = materialTypes;
	}

	public GameMaterialType getPrimaryMaterialType() {
		return primaryMaterialType;
	}

	public void setPrimaryMaterialType(GameMaterialType primaryMaterialType) {
		this.primaryMaterialType = primaryMaterialType;
	}

	public ItemGroup getItemGroup() {
		return itemGroup;
	}

	public void setItemGroup(ItemGroup itemGroup) {
		this.itemGroup = itemGroup;
	}

	public boolean isImpedesMovement() {
		return impedesMovement;
	}

	public boolean isBlocksMovement() {
		return blocksMovement;
	}

	public List<String> getRelatedCraftingTypeNames() {
		return relatedCraftingTypeNames;
	}

	public void setRelatedCraftingTypeNames(List<String> relatedCraftingTypeNames) {
		this.relatedCraftingTypeNames = relatedCraftingTypeNames;
	}

	public List<CraftingType> getRelatedCraftingTypes() {
		return relatedCraftingTypes;
	}

	public void setRelatedCraftingTypes(List<CraftingType> relatedCraftingTypes) {
		this.relatedCraftingTypes = relatedCraftingTypes;
	}

	public String getI18nKey() {
		return itemTypeName.toUpperCase().replaceAll("-", ".").replaceAll(" ", "_");
	}

	public boolean isEquippedWhileWorkingOnJob() {
		return equippedWhileWorkingOnJob;
	}

	public void setEquippedWhileWorkingOnJob(boolean equippedWhileWorkingOnJob) {
		this.equippedWhileWorkingOnJob = equippedWhileWorkingOnJob;
	}

	public String getStockpileGroupName() {
		return stockpileGroupName;
	}

	public void setStockpileGroupName(String stockpileGroupName) {
		this.stockpileGroupName = stockpileGroupName;
	}

	public StockpileGroup getStockpileGroup() {
		return stockpileGroup;
	}

	public void setStockpileGroup(StockpileGroup stockpileGroup) {
		this.stockpileGroup = stockpileGroup;
	}

	public double getHoursInInventoryUntilUnused() {
		return hoursInInventoryUntilUnused;
	}

	public void setHoursInInventoryUntilUnused(double hoursInInventoryUntilUnused) {
		this.hoursInInventoryUntilUnused = hoursInInventoryUntilUnused;
	}

	public String getPlacementSoundAssetName() {
		return placementSoundAssetName;
	}

	public void setPlacementSoundAssetName(String placementSoundAssetName) {
		this.placementSoundAssetName = placementSoundAssetName;
	}

	public SoundAsset getPlacementSoundAsset() {
		return placementSoundAsset;
	}

	public void setPlacementSoundAsset(SoundAsset placementSoundAsset) {
		this.placementSoundAsset = placementSoundAsset;
	}

	public Map<String, List<String>> getTags() {
		return tags;
	}

	public void setTags(Map<String, List<String>> tags) {
		this.tags = tags;
	}

	public void setProcessedTags(List<Tag> processedTags) {
		this.processedTags = processedTags;
	}

	public List<Tag> getProcessedTags() {
		return processedTags;
	}
}
