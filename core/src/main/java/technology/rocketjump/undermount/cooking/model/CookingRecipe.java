package technology.rocketjump.undermount.cooking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeWithMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.misc.Name;

import java.util.List;

public class CookingRecipe {

	@Name
	private String recipeName;

	private List<ItemTypeWithMaterial> inputItemOptions;
	private int inputItemQuantity;

	private List<ItemTypeWithMaterial> inputLiquidOptions;
	private int inputLiquidQuantity;

	private String cookedInFurnitureName;
	@JsonIgnore
	private FurnitureType cookedInFurniture;

	private String verbOverrideI18nKey;
	private String outputDescriptionI18nKey;
	private GameMaterialType outputMaterialType;
	private int outputQuantity;
	private CookingProcess outputProcess;
	private String outputItemTypeName;
	@JsonIgnore
	private ItemType outputItemType;
	private String outputMaterialName;
	@JsonIgnore
	private GameMaterial outputMaterial;
	private String activeSoundAssetName;
	@JsonIgnore
	private SoundAsset activeSoundAsset;

	private String onCompletionSoundAssetName;
	@JsonIgnore
	private SoundAsset onCompletionSoundAsset;
	private Float defaultTimeToCompleteCooking;

	public String getOutputDescriptionI18nKey() {
		return outputDescriptionI18nKey;
	}

	public void setOutputDescriptionI18nKey(String outputDescriptionI18nKey) {
		this.outputDescriptionI18nKey = outputDescriptionI18nKey;
	}

	public String getVerbOverrideI18nKey() {
		return verbOverrideI18nKey;
	}

	public void setVerbOverrideI18nKey(String verbOverrideI18nKey) {
		this.verbOverrideI18nKey = verbOverrideI18nKey;
	}

	public Float getDefaultTimeToCompleteCooking() {
		return defaultTimeToCompleteCooking;
	}

	public void setDefaultTimeToCompleteCooking(Float defaultTimeToCompleteCooking) {
		this.defaultTimeToCompleteCooking = defaultTimeToCompleteCooking;
	}

	public enum CookingProcess {
		COMBINE_ITEM_MATERIALS, PICK_MOST_COMMON_ITEM_MATERIAL, SPECIFIED_MATERIAL
	}

	public String getRecipeName() {
		return recipeName;
	}

	public void setRecipeName(String recipeName) {
		this.recipeName = recipeName;
	}

	public List<ItemTypeWithMaterial> getInputItemOptions() {
		return inputItemOptions;
	}

	public void setInputItemOptions(List<ItemTypeWithMaterial> inputItemOptions) {
		this.inputItemOptions = inputItemOptions;
	}

	public int getInputItemQuantity() {
		return inputItemQuantity;
	}

	public void setInputItemQuantity(int inputItemQuantity) {
		this.inputItemQuantity = inputItemQuantity;
	}

	public List<ItemTypeWithMaterial> getInputLiquidOptions() {
		return inputLiquidOptions;
	}

	public void setInputLiquidOptions(List<ItemTypeWithMaterial> inputLiquidOptions) {
		this.inputLiquidOptions = inputLiquidOptions;
	}

	public int getInputLiquidQuantity() {
		return inputLiquidQuantity;
	}

	public void setInputLiquidQuantity(int inputLiquidQuantity) {
		this.inputLiquidQuantity = inputLiquidQuantity;
	}

	public String getCookedInFurnitureName() {
		return cookedInFurnitureName;
	}

	public void setCookedInFurnitureName(String cookedInFurnitureName) {
		this.cookedInFurnitureName = cookedInFurnitureName;
	}

	public FurnitureType getCookedInFurniture() {
		return cookedInFurniture;
	}

	public void setCookedInFurniture(FurnitureType cookedInFurniture) {
		this.cookedInFurniture = cookedInFurniture;
	}

	public GameMaterialType getOutputMaterialType() {
		return outputMaterialType;
	}

	public void setOutputMaterialType(GameMaterialType outputMaterialType) {
		this.outputMaterialType = outputMaterialType;
	}

	public int getOutputQuantity() {
		return outputQuantity;
	}

	public void setOutputQuantity(int outputQuantity) {
		this.outputQuantity = outputQuantity;
	}

	public CookingProcess getOutputProcess() {
		return outputProcess;
	}

	public void setOutputProcess(CookingProcess outputProcess) {
		this.outputProcess = outputProcess;
	}

	public String getOutputItemTypeName() {
		return outputItemTypeName;
	}

	public void setOutputItemTypeName(String outputItemTypeName) {
		this.outputItemTypeName = outputItemTypeName;
	}

	public ItemType getOutputItemType() {
		return outputItemType;
	}

	public void setOutputItemType(ItemType outputItemType) {
		this.outputItemType = outputItemType;
	}

	public String getOutputMaterialName() {
		return outputMaterialName;
	}

	public void setOutputMaterialName(String outputMaterialName) {
		this.outputMaterialName = outputMaterialName;
	}

	public GameMaterial getOutputMaterial() {
		return outputMaterial;
	}

	public void setOutputMaterial(GameMaterial outputMaterial) {
		this.outputMaterial = outputMaterial;
	}

	public String getActiveSoundAssetName() {
		return activeSoundAssetName;
	}

	public void setActiveSoundAssetName(String activeSoundAssetName) {
		this.activeSoundAssetName = activeSoundAssetName;
	}

	public SoundAsset getActiveSoundAsset() {
		return activeSoundAsset;
	}

	public void setActiveSoundAsset(SoundAsset activeSoundAsset) {
		this.activeSoundAsset = activeSoundAsset;
	}

	public String getOnCompletionSoundAssetName() {
		return onCompletionSoundAssetName;
	}

	public void setOnCompletionSoundAssetName(String onCompletionSoundAssetName) {
		this.onCompletionSoundAssetName = onCompletionSoundAssetName;
	}

	public SoundAsset getOnCompletionSoundAsset() {
		return onCompletionSoundAsset;
	}

	public void setOnCompletionSoundAsset(SoundAsset onCompletionSoundAsset) {
		this.onCompletionSoundAsset = onCompletionSoundAsset;
	}

	@Override
	public String toString() {
		return recipeName;
	}
}
