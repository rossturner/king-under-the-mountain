package technology.rocketjump.undermount.gamecontext;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.constants.ConstantsRepo;
import technology.rocketjump.undermount.constants.SettlementConstants;
import technology.rocketjump.undermount.entities.model.physical.creature.Race;
import technology.rocketjump.undermount.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.environment.DailyWeatherTypeDictionary;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.environment.WeatherTypeDictionary;
import technology.rocketjump.undermount.mapping.model.MapEnvironment;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;
import technology.rocketjump.undermount.settlement.SettlementState;
import technology.rocketjump.undermount.settlement.production.ProductionQuota;

import java.util.HashMap;

import static technology.rocketjump.undermount.environment.WeatherManager.selectDailyWeather;
import static technology.rocketjump.undermount.gamecontext.GameState.SELECT_SPAWN_LOCATION;

@Singleton
public class GameContextFactory {

	private final ItemTypeDictionary itemTypeDictionary;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final WeatherTypeDictionary weatherTypeDictionary;
	private final DailyWeatherTypeDictionary dailyWeatherTypeDictionary;
	private final JSONObject itemProductionDefaultsJson;
	private final JSONObject liquidProductionDefaultsJson;
	private final SettlementConstants settlementConstants;
	private Race settlerRace;

	@Inject
	public GameContextFactory(ItemTypeDictionary itemTypeDictionary, GameMaterialDictionary gameMaterialDictionary,
							  WeatherTypeDictionary weatherTypeDictionary, DailyWeatherTypeDictionary dailyWeatherTypeDictionary,
							  ConstantsRepo constantsRepo, RaceDictionary raceDictionary) {
		this.itemTypeDictionary = itemTypeDictionary;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.weatherTypeDictionary = weatherTypeDictionary;
		this.dailyWeatherTypeDictionary = dailyWeatherTypeDictionary;
		FileHandle itemProductionDefaultsFile = new FileHandle("assets/definitions/crafting/itemProductionDefaults.json");
		itemProductionDefaultsJson = JSON.parseObject(itemProductionDefaultsFile.readString());
		FileHandle liquidProductionDefaultsFile = new FileHandle("assets/definitions/crafting/liquidProductionDefaults.json");
		liquidProductionDefaultsJson = JSON.parseObject(liquidProductionDefaultsFile.readString());
		settlementConstants = constantsRepo.getSettlementConstants();
		this.settlerRace = raceDictionary.getByName("Dwarf"); // MODDING expose and test this
	}

	public GameContext create(String settlementName, TiledMap areaMap, long worldSeed, GameClock clock) {
		GameContext context = new GameContext();
		context.getSettlementState().setSettlementName(settlementName);
		context.getSettlementState().setSettlerRace(settlerRace);
		if (GlobalSettings.DEV_MODE) {
			if (GlobalSettings.CHOOSE_SPAWN_LOCATION) {
				context.getSettlementState().setGameState(SELECT_SPAWN_LOCATION);
				clock.setPaused(true);
			} else {
				context.getSettlementState().setGameState(GameState.NORMAL);
			}
		} else {
			context.getSettlementState().setGameState(SELECT_SPAWN_LOCATION);
			clock.setPaused(true);
		}
		context.getSettlementState().setFishRemainingInRiver(settlementConstants.getNumAnnualFish());
		context.setAreaMap(areaMap);
		context.setRandom(new RandomXS128(worldSeed));
		context.setGameClock(clock);
		context.setMapEnvironment(new MapEnvironment());
		initialise(context.getSettlementState());
		initialise(context.getMapEnvironment(), context);
		return context;
	}

	public GameContext create(SavedGameStateHolder stateHolder) {
		GameContext context = new GameContext();

		context.getJobs().putAll(stateHolder.jobs);
		for (int cursor = 0; cursor < stateHolder.entityIdsToLoad.size(); cursor++) {
			Long entityId = stateHolder.entityIdsToLoad.getLong(cursor);
			context.getEntities().put(entityId, stateHolder.entities.get(entityId));
		}

		context.getConstructions().putAll(stateHolder.constructions);
		context.getRooms().putAll(stateHolder.rooms);
		context.getJobRequestQueue().addAll(stateHolder.jobRequests.values());
		context.getDynamicallyCreatedMaterialsByCombinedId().putAll(stateHolder.dynamicMaterials);
		context.setSettlementState(stateHolder.getSettlementState());

		context.setAreaMap(stateHolder.getMap());
		context.setMapEnvironment(stateHolder.getMapEnvironment());
		context.setRandom(new RandomXS128()); // Not yet maintaining world seed
		context.setGameClock(stateHolder.getGameClock());

		return context;
	}

	private void initialise(MapEnvironment mapEnvironment, GameContext context) {
		mapEnvironment.setDailyWeather(selectDailyWeather(context, dailyWeatherTypeDictionary));
		mapEnvironment.setCurrentWeather(weatherTypeDictionary.getByName("Perfect"));
	}

	private void initialise(SettlementState settlementState) {
		for (String itemTypeString : itemProductionDefaultsJson.keySet()) {
			ItemType itemType = itemTypeDictionary.getByName(itemTypeString);
			if (itemType != null) {
				JSONObject quotaJson = itemProductionDefaultsJson.getJSONObject(itemTypeString);
				ProductionQuota quota = new ProductionQuota();
				quota.setFixedAmount(quotaJson.getInteger("fixedAmount"));
				quota.setPerSettler(quotaJson.getFloat("perSettler"));

				if (quota.getFixedAmount() == null && quota.getPerSettler() == null) {
					Logger.error("Can not parse " + quotaJson.toString() + " from productionDefaults for " + itemTypeString);
				} else {
					settlementState.itemTypeProductionQuotas.put(itemType, quota);
					settlementState.itemTypeProductionAssignments.put(itemType, new HashMap<>());
					settlementState.requiredItemCounts.put(itemType, 0);
				}
			} else {
				Logger.error("Unrecognised item type name from itemProductionDefaults.json: " + itemTypeString);
			}
		}

		for (String liquidMaterialName : liquidProductionDefaultsJson.keySet()) {
			GameMaterial liquidMaterial = gameMaterialDictionary.getByName(liquidMaterialName);
			if (liquidMaterial != null) {
				JSONObject quotaJson = liquidProductionDefaultsJson.getJSONObject(liquidMaterialName);
				ProductionQuota quota = new ProductionQuota();
				quota.setFixedAmount(quotaJson.getInteger("fixedAmount"));
				quota.setPerSettler(quotaJson.getFloat("perSettler"));

				if (quota.getFixedAmount() == null && quota.getPerSettler() == null) {
					Logger.error("Can not parse " + quotaJson.toString() + " from productionDefaults for " + liquidMaterialName);
				} else {
					settlementState.liquidProductionQuotas.put(liquidMaterial, quota);
					settlementState.liquidProductionAssignments.put(liquidMaterial, new HashMap<>());
					settlementState.requiredLiquidCounts.put(liquidMaterial, 0f);
				}
			} else {
				Logger.error("Unrecognised material name from liquidProductionDefaults.json: " + liquidMaterialName);
			}
		}
	}
}
