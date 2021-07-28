package technology.rocketjump.undermount.persistence;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.FloorTypeDictionary;
import technology.rocketjump.undermount.assets.WallTypeDictionary;
import technology.rocketjump.undermount.assets.entities.CompleteAssetDictionary;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.cooking.CookingRecipeDictionary;
import technology.rocketjump.undermount.crafting.CraftingRecipeDictionary;
import technology.rocketjump.undermount.entities.ai.goap.GoalDictionary;
import technology.rocketjump.undermount.entities.ai.goap.ScheduleDictionary;
import technology.rocketjump.undermount.entities.ai.goap.actions.ActionDictionary;
import technology.rocketjump.undermount.entities.components.ComponentDictionary;
import technology.rocketjump.undermount.entities.components.StatusEffectDictionary;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureLayoutDictionary;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.undermount.entities.tags.TagDictionary;
import technology.rocketjump.undermount.environment.WeatherTypeDictionary;
import technology.rocketjump.undermount.jobs.CraftingTypeDictionary;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.jobs.JobTypeDictionary;
import technology.rocketjump.undermount.jobs.ProfessionDictionary;
import technology.rocketjump.undermount.mapping.tile.designation.TileDesignationDictionary;
import technology.rocketjump.undermount.materials.DynamicMaterialFactory;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.undermount.rooms.RoomStore;
import technology.rocketjump.undermount.rooms.RoomTypeDictionary;
import technology.rocketjump.undermount.rooms.StockpileGroupDictionary;
import technology.rocketjump.undermount.rooms.components.RoomComponentDictionary;
import technology.rocketjump.undermount.sprites.BridgeTypeDictionary;

/**
 * This class is used when loading a saved game for loaded data to allow for loading of dependent data
 */
@Singleton
public class SavedGameDependentDictionaries {

	public final DynamicMaterialFactory dynamicMaterialFactory;
	public final GameMaterialDictionary gameMaterialDictionary;
	public final MessageDispatcher messageDispatcher;
	public final ProfessionDictionary professionDictionary;
	public final JobTypeDictionary jobTypeDictionary;
	public final ItemTypeDictionary itemTypeDictionary;
	public final FloorTypeDictionary floorTypeDictionary;
	public final CookingRecipeDictionary cookingRecipeDictionary;
	public final ComponentDictionary componentDictionary;
	public final StatusEffectDictionary statusEffectDictionary;
	public final CraftingTypeDictionary craftingTypeDictionary;
	public final CraftingRecipeDictionary craftingRecipeDictionary;
	public final CompleteAssetDictionary completeAssetDictionary;
	public final GoalDictionary goalDictionary;
	public final ScheduleDictionary scheduleDictionary;
	public final RoomStore roomStore;
	public final ActionDictionary actionDictionary;
	public final FurnitureTypeDictionary furnitureTypeDictionary;
	public final FurnitureLayoutDictionary furnitureLayoutDictionary;
	public final PlantSpeciesDictionary plantSpeciesDictionary;
	public final WallTypeDictionary wallTypeDictionary;
	public final RoomTypeDictionary roomTypeDictionary;
	public final RoomComponentDictionary roomComponentDictionary;
	public final TileDesignationDictionary tileDesignationDictionary;
	public final StockpileGroupDictionary stockpileGroupDictionary;
	public final TagDictionary tagDictionary;
	public final SoundAssetDictionary soundAssetDictionary;
	public final BridgeTypeDictionary bridgeTypeDictionary;
	public final JobStore jobStore;
	public final ParticleEffectTypeDictionary particleEffectTypeDictionary;
	public final OngoingEffectTypeDictionary ongoingEffectTypeDictionary;
	public final WeatherTypeDictionary weatherTypeDictionary;

	@Inject
	public SavedGameDependentDictionaries(DynamicMaterialFactory dynamicMaterialFactory, GameMaterialDictionary gameMaterialDictionary,
										  MessageDispatcher messageDispatcher, ProfessionDictionary professionDictionary,
										  JobTypeDictionary jobTypeDictionary, ItemTypeDictionary itemTypeDictionary, FloorTypeDictionary floorTypeDictionary,
										  CookingRecipeDictionary cookingRecipeDictionary, ComponentDictionary componentDictionary,
										  StatusEffectDictionary statusEffectDictionary, CraftingTypeDictionary craftingTypeDictionary,
										  CraftingRecipeDictionary craftingRecipeDictionary, CompleteAssetDictionary completeAssetDictionary,
										  GoalDictionary goalDictionary, ScheduleDictionary scheduleDictionary, RoomStore roomStore,
										  ActionDictionary actionDictionary, FurnitureTypeDictionary furnitureTypeDictionary,
										  FurnitureLayoutDictionary furnitureLayoutDictionary, PlantSpeciesDictionary plantSpeciesDictionary,
										  WallTypeDictionary wallTypeDictionary, RoomTypeDictionary roomTypeDictionary,
										  RoomComponentDictionary roomComponentDictionary, TileDesignationDictionary tileDesignationDictionary,
										  StockpileGroupDictionary stockpileGroupDictionary, TagDictionary tagDictionary,
										  SoundAssetDictionary soundAssetDictionary, BridgeTypeDictionary bridgeTypeDictionary,
										  JobStore jobStore, ParticleEffectTypeDictionary particleEffectTypeDictionary,
										  OngoingEffectTypeDictionary ongoingEffectTypeDictionary, WeatherTypeDictionary weatherTypeDictionary) {
		this.dynamicMaterialFactory = dynamicMaterialFactory;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.messageDispatcher = messageDispatcher;
		this.professionDictionary = professionDictionary;
		this.jobTypeDictionary = jobTypeDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.floorTypeDictionary = floorTypeDictionary;
		this.cookingRecipeDictionary = cookingRecipeDictionary;
		this.componentDictionary = componentDictionary;
		this.statusEffectDictionary = statusEffectDictionary;
		this.craftingTypeDictionary = craftingTypeDictionary;
		this.craftingRecipeDictionary = craftingRecipeDictionary;
		this.completeAssetDictionary = completeAssetDictionary;
		this.goalDictionary = goalDictionary;
		this.scheduleDictionary = scheduleDictionary;
		this.roomStore = roomStore;
		this.actionDictionary = actionDictionary;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.furnitureLayoutDictionary = furnitureLayoutDictionary;
		this.plantSpeciesDictionary = plantSpeciesDictionary;
		this.wallTypeDictionary = wallTypeDictionary;
		this.roomTypeDictionary = roomTypeDictionary;
		this.roomComponentDictionary = roomComponentDictionary;
		this.tileDesignationDictionary = tileDesignationDictionary;
		this.stockpileGroupDictionary = stockpileGroupDictionary;
		this.tagDictionary = tagDictionary;
		this.soundAssetDictionary = soundAssetDictionary;
		this.bridgeTypeDictionary = bridgeTypeDictionary;
		this.jobStore = jobStore;
		this.particleEffectTypeDictionary = particleEffectTypeDictionary;
		this.ongoingEffectTypeDictionary = ongoingEffectTypeDictionary;
		this.weatherTypeDictionary = weatherTypeDictionary;
	}
}
