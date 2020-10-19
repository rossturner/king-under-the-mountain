package technology.rocketjump.undermount.gamecontext;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.settlement.SettlementState;
import technology.rocketjump.undermount.settlement.production.ProductionQuota;

import java.util.HashMap;

@Singleton
public class GameContextFactory {

	private final ItemTypeDictionary itemTypeDictionary;
	private final JSONObject productionDefaultsJson;

	@Inject
	public GameContextFactory(ItemTypeDictionary itemTypeDictionary) {
		this.itemTypeDictionary = itemTypeDictionary;
		FileHandle productionDefaultsFile = new FileHandle("assets/definitions/crafting/productionDefaults.json");
		productionDefaultsJson = JSON.parseObject(productionDefaultsFile.readString());
	}

	public GameContext create(TiledMap areaMap, long worldSeed, GameClock clock) {
		GameContext context = new GameContext();
		context.setAreaMap(areaMap);
		context.setRandom(new RandomXS128(worldSeed));
		context.setGameClock(clock);
		initialise(context.getSettlementState());
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
		context.setRandom(new RandomXS128()); // Not yet maintaining world seed
		context.setGameClock(stateHolder.getGameClock());

		return context;
	}

	private void initialise(SettlementState settlementState) {
		for (String itemTypeString : productionDefaultsJson.keySet()) {
			ItemType itemType = itemTypeDictionary.getByName(itemTypeString);
			if (itemType != null) {
				JSONObject quotaJson = productionDefaultsJson.getJSONObject(itemTypeString);
				ProductionQuota quota = new ProductionQuota();
				quota.setFixedAmount(quotaJson.getInteger("fixedAmount"));
				quota.setPerSettler(quotaJson.getFloat("perSettler"));

				if (quota.getFixedAmount() == null && quota.getPerSettler() == null) {
					Logger.error("Can not parse " + quotaJson.toString() + " from productionDefaults for " + itemTypeString);
				} else {
					settlementState.itemTypeProductionQuotas.put(itemType, quota);
					settlementState.productionAssignments.put(itemType, new HashMap<>());
					settlementState.requiredItemCounts.put(itemType, 0);
				}
			} else {
				Logger.error("Unrecognised item type name from productionDefaults.json: " + itemTypeString);
			}
		}
	}
}
