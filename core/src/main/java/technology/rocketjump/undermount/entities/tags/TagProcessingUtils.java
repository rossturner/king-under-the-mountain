package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import technology.rocketjump.undermount.assets.FloorTypeDictionary;
import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.cooking.CookingRecipeDictionary;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.undermount.jobs.CraftingTypeDictionary;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.jobs.JobTypeDictionary;
import technology.rocketjump.undermount.jobs.ProfessionDictionary;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.undermount.rooms.StockpileGroupDictionary;

public class TagProcessingUtils {

	public final MessageDispatcher messageDispatcher;
	public final EntityAssetTypeDictionary entityAssetTypeDictionary;
	public final FloorTypeDictionary floorTypeDictionary;
	public final ItemTypeDictionary itemTypeDictionary;
	public final GameMaterialDictionary materialDictionary;
	public final PlantSpeciesDictionary plantSpeciesDictionary;
	public final StockpileGroupDictionary stockpileGroupDictionary;
	public final CookingRecipeDictionary cookingRecipeDictionary;
	public final ProfessionDictionary professionDictionary;
	public final JobTypeDictionary jobTypeDictionary;
	public final CraftingTypeDictionary craftingTypeDictionary;
	public final FurnitureTypeDictionary furnitureTypeDictionary;
	public final SoundAssetDictionary soundAssetDictionary;
	public final JobStore jobStore;
	public final ParticleEffectTypeDictionary particleEffectTypeDictionary;

	@Inject
	public TagProcessingUtils(MessageDispatcher messageDispatcher, EntityAssetTypeDictionary entityAssetTypeDictionary, FloorTypeDictionary floorTypeDictionary,
							  ItemTypeDictionary itemTypeDictionary, GameMaterialDictionary materialDictionary, PlantSpeciesDictionary plantSpeciesDictionary,
							  StockpileGroupDictionary stockpileGroupDictionary, CookingRecipeDictionary cookingRecipeDictionary,
							  ProfessionDictionary professionDictionary, JobTypeDictionary jobTypeDictionary,
							  CraftingTypeDictionary craftingTypeDictionary, FurnitureTypeDictionary furnitureTypeDictionary,
							  SoundAssetDictionary soundAssetDictionary, JobStore jobStore, ParticleEffectTypeDictionary particleEffectTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.entityAssetTypeDictionary = entityAssetTypeDictionary;
		this.floorTypeDictionary = floorTypeDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.materialDictionary = materialDictionary;
		this.plantSpeciesDictionary = plantSpeciesDictionary;
		this.stockpileGroupDictionary = stockpileGroupDictionary;
		this.cookingRecipeDictionary = cookingRecipeDictionary;
		this.jobTypeDictionary = jobTypeDictionary;
		this.professionDictionary = professionDictionary;
		this.craftingTypeDictionary = craftingTypeDictionary;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.soundAssetDictionary = soundAssetDictionary;
		this.jobStore = jobStore;
		this.particleEffectTypeDictionary = particleEffectTypeDictionary;
	}
}
