package technology.rocketjump.undermount.settlement;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.crafting.model.CraftingRecipe;
import technology.rocketjump.undermount.crafting.model.CraftingRecipeMaterialSelection;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.Race;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.gamecontext.GameState;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.mapping.model.ImpendingMiningCollapse;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.misc.twitch.model.TwitchViewer;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.settlement.notifications.Notification;
import technology.rocketjump.undermount.settlement.production.ProductionAssignment;
import technology.rocketjump.undermount.settlement.production.ProductionQuota;

import java.util.*;

import static technology.rocketjump.undermount.gamecontext.GameState.GAME_OVER;
import static technology.rocketjump.undermount.gamecontext.GameState.NORMAL;

/**
 * Simple bean to hold "global" settlement data
 */
public class SettlementState implements Persistable {

	private String settlementName;

	public final Map<Long, Entity> furnitureHoldingCompletedCooking = new HashMap<>();
	public final List<MapTile> activeLiquidFlowTiles = new ArrayList<>();

	// Crafting-related state
	public final Map<ItemType, ProductionQuota> itemTypeProductionQuotas = new HashMap<>();
	public final Map<ItemType, Map<Long, ProductionAssignment>> itemTypeProductionAssignments = new HashMap<>();
	public final Map<ItemType, Integer> requiredItemCounts = new HashMap<>();
	public final Map<GameMaterial, ProductionQuota> liquidProductionQuotas = new HashMap<>();
	public final Map<GameMaterial, Map<Long, ProductionAssignment>> liquidProductionAssignments = new HashMap<>();
	public final Map<GameMaterial, Float> requiredLiquidCounts = new HashMap<>();

	public final Map<CraftingRecipe, JobPriority> craftingRecipePriority = new HashMap<>();
	public final Map<CraftingRecipe, CraftingRecipeMaterialSelection> craftingRecipeMaterialSelections = new HashMap<>();

	public final List<Notification> queuedNotifications = new ArrayList<>();
	public final List<ImpendingMiningCollapse> impendingMiningCollapses = new ArrayList<>();
	public final Map<String, Boolean> previousHints = new HashMap<>();
	public final List<String> currentHints = new ArrayList<>();
	public final Set<TwitchViewer> usedTwitchViewers = new HashSet<>();

	private int immigrantsDue;
	private int immigrantCounter;
	private Vector2 immigrationPoint;
	private Double nextImmigrationGameTime;
	private int fishRemainingInRiver;
	private GameState gameState;
	private Race settlerRace;

	public String getSettlementName() {
		return settlementName;
	}

	public void setSettlementName(String settlementName) {
		this.settlementName = settlementName;
	}

	public int getImmigrantsDue() {
		return immigrantsDue;
	}

	public void setImmigrantsDue(int immigrantsDue) {
		this.immigrantsDue = immigrantsDue;
	}

	public Double getNextImmigrationGameTime() {
		return nextImmigrationGameTime;
	}

	public void setNextImmigrationGameTime(Double nextImmigrationGameTime) {
		this.nextImmigrationGameTime = nextImmigrationGameTime;
	}

	public void setImmigrantCounter(int numImmigrants) {
		this.immigrantCounter = numImmigrants;
	}

	public int getImmigrantCounter() {
		return immigrantCounter;
	}

	public Vector2 getImmigrationPoint() {
		return immigrationPoint;
	}

	public void setImmigrationPoint(Vector2 immigrationPoint) {
		this.immigrationPoint = immigrationPoint;
	}

	public boolean isGameOver() {
		return gameState.equals(GAME_OVER);
	}

	public Map<String, Boolean> getPreviousHints() {
		return previousHints;
	}

	public List<String> getCurrentHints() {
		return currentHints;
	}

	public GameState getGameState() {
		return gameState;
	}

	public void setGameState(GameState currentState) {
		this.gameState = currentState;
	}

	public int getFishRemainingInRiver() {
		return fishRemainingInRiver;
	}

	public void setFishRemainingInRiver(int fishRemainingInRiver) {
		this.fishRemainingInRiver = fishRemainingInRiver;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		JSONObject asJson = savedGameStateHolder.settlementStateJson;

		asJson.put("settlementName", settlementName);

		JSONObject furnitureEntityJson = new JSONObject(true);
		for (Map.Entry<Long, Entity> entry : furnitureHoldingCompletedCooking.entrySet()) {
			furnitureEntityJson.put(String.valueOf(entry.getKey()), entry.getValue().getId());
		}
		asJson.put("furnitureHoldingCompletedCooking", furnitureEntityJson);

		JSONArray activeFlowTilesJson = new JSONArray();
		for (MapTile activeLiquidFlowTile : activeLiquidFlowTiles) {
			JSONObject activeFlowTileJson = JSONUtils.toJSON(activeLiquidFlowTile.getTilePosition());
			activeFlowTilesJson.add(activeFlowTileJson);
		}
		asJson.put("activeFlowTiles", activeFlowTilesJson);


		JSONObject productionQuotasJson = new JSONObject(true);
		for (Map.Entry<ItemType, ProductionQuota> entry : itemTypeProductionQuotas.entrySet()) {
			JSONObject quotaAsJson = new JSONObject(true);
			entry.getValue().writeTo(quotaAsJson, savedGameStateHolder);
			productionQuotasJson.put(entry.getKey().getItemTypeName(), quotaAsJson);
		}
		asJson.put("productionQuotas", productionQuotasJson);

		JSONObject productionAssignmentsJson = new JSONObject(true);
		for (Map.Entry<ItemType, Map<Long, ProductionAssignment>> itemTypeMapEntry : itemTypeProductionAssignments.entrySet()) {
			JSONArray productionAssignmentIds = new JSONArray();
			for (ProductionAssignment productionAssignment : itemTypeMapEntry.getValue().values()) {
				productionAssignment.writeTo(savedGameStateHolder);
				productionAssignmentIds.add(productionAssignment.productionAssignmentId);
			}
			productionAssignmentsJson.put(itemTypeMapEntry.getKey().getItemTypeName(), productionAssignmentIds);
		}
		asJson.put("productionAssignments", productionAssignmentsJson);

		JSONObject itemCountsJson = new JSONObject(true);
		for (Map.Entry<ItemType, Integer> entry : requiredItemCounts.entrySet()) {
			itemCountsJson.put(entry.getKey().getItemTypeName(), entry.getValue());
		}
		asJson.put("requiredItemCounts", itemCountsJson);

		JSONObject liquidProductionQuotasJson = new JSONObject(true);
		for (Map.Entry<GameMaterial, ProductionQuota> entry : liquidProductionQuotas.entrySet()) {
			JSONObject quotaAsJson = new JSONObject(true);
			entry.getValue().writeTo(quotaAsJson, savedGameStateHolder);
			liquidProductionQuotasJson.put(entry.getKey().getMaterialName(), quotaAsJson);
		}
		asJson.put("liquidProductionQuotas", liquidProductionQuotasJson);

		JSONObject liquidProductionAssignmentsJson = new JSONObject(true);
		for (Map.Entry<GameMaterial, Map<Long, ProductionAssignment>> gameMaterialMapEntry : liquidProductionAssignments.entrySet()) {
			JSONArray productionAssignmentIds = new JSONArray();
			for (ProductionAssignment productionAssignment : gameMaterialMapEntry.getValue().values()) {
				productionAssignment.writeTo(savedGameStateHolder);
				productionAssignmentIds.add(productionAssignment.productionAssignmentId);
			}
			liquidProductionAssignmentsJson.put(gameMaterialMapEntry.getKey().getMaterialName(), productionAssignmentIds);
		}
		asJson.put("liquidProductionAssignments", liquidProductionAssignmentsJson);

		JSONObject liquidCountsJson = new JSONObject(true);
		for (Map.Entry<GameMaterial, Float> entry : requiredLiquidCounts.entrySet()) {
			liquidCountsJson.put(entry.getKey().getMaterialName(), entry.getValue());
		}
		asJson.put("requiredLiquidCounts", liquidCountsJson);

		if (!previousHints.isEmpty()) {
			JSONArray previousHintsJson = new JSONArray();
			previousHintsJson.addAll(previousHints.keySet());
			asJson.put("previousHints", previousHintsJson);
		}

		if (!currentHints.isEmpty()) {
			JSONArray currentHintsJson = new JSONArray();
			currentHintsJson.addAll(currentHints);
			asJson.put("currentHints", currentHintsJson);
		}

		if (immigrantsDue != 0) {
			asJson.put("immigrantsDue", immigrantsDue);
		}
		if (immigrantCounter != 0) {
			asJson.put("immigrantCounter", immigrantCounter);
		}
		if (immigrationPoint != null) {
			asJson.put("immigrationPoint", JSONUtils.toJSON(immigrationPoint));
		}
		if (nextImmigrationGameTime != null) {
			asJson.put("nextImmigration", nextImmigrationGameTime);
		}

		if (!queuedNotifications.isEmpty()) {
			JSONArray notificationsJson = new JSONArray();
			for (Notification queuedNotification : queuedNotifications) {
				JSONObject notificationJson = new JSONObject(true);
				queuedNotification.writeTo(notificationJson, savedGameStateHolder);
				notificationsJson.add(notificationJson);
			}
			asJson.put("notifications", notificationsJson);
		}

		if (!impendingMiningCollapses.isEmpty()) {
			JSONArray collapsesJson = new JSONArray();
			for (ImpendingMiningCollapse impendingMiningCollapse : impendingMiningCollapses) {
				JSONObject collapseJson = new JSONObject(true);
				impendingMiningCollapse.writeTo(collapseJson, savedGameStateHolder);
				collapsesJson.add(collapseJson);
			}
			asJson.put("impendingCollapses", collapsesJson);
		}

		if (!gameState.equals(NORMAL)) {
			asJson.put("gameState", gameState.name());
		}

		if (!craftingRecipePriority.isEmpty()) {
			JSONObject craftingRecipePriorityJson = new JSONObject();
			for (Map.Entry<CraftingRecipe, JobPriority> entry : craftingRecipePriority.entrySet()) {
				if (entry.getValue().equals(JobPriority.NORMAL)) {
					continue;
				}
				craftingRecipePriorityJson.put(entry.getKey().getRecipeName(), entry.getValue().name());
			}
			asJson.put("craftingRecipePriority", craftingRecipePriorityJson);
		}

		if (!craftingRecipeMaterialSelections.isEmpty()) {
			JSONObject craftingRecipeSelectionJson = new JSONObject();
			for (Map.Entry<CraftingRecipe, CraftingRecipeMaterialSelection> entry : craftingRecipeMaterialSelections.entrySet()) {
				JSONObject materialSelectionJson = new JSONObject(true);
				entry.getValue().writeTo(materialSelectionJson, savedGameStateHolder);
				craftingRecipeSelectionJson.put(entry.getKey().getRecipeName(), materialSelectionJson);
			}
			asJson.put("craftingRecipeSelections", craftingRecipeSelectionJson);
		}

		if (!usedTwitchViewers.isEmpty()) {
			JSONArray viewerNames = new JSONArray();
			for (TwitchViewer twitchViewer : usedTwitchViewers) {
				viewerNames.add(twitchViewer.getUsername());
			}
			asJson.put("usedTwitchViewers", viewerNames);
		}

		asJson.put("fishRemaining", fishRemainingInRiver);

		if (settlerRace != null) {
			asJson.put("settlerRace", settlerRace.getName());
		}

		savedGameStateHolder.setSettlementState(this);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.settlementName = asJson.getString("settlementName");
		if (this.settlementName == null) {
			throw new InvalidSaveException("Settlement name not specified");
		}

		JSONObject furnitureEntityJson = asJson.getJSONObject("furnitureHoldingCompletedCooking");
		for (String keyString : furnitureEntityJson.keySet()) {
			long key = Long.valueOf(keyString);
			long value = furnitureEntityJson.getLongValue(keyString);
			Entity entity = savedGameStateHolder.entities.get(value);
			if (entity == null) {
				throw new InvalidSaveException("Could not find entity by ID " + value);
			}
			furnitureHoldingCompletedCooking.put(key, entity);
		}

		JSONArray activeFlowTilesJson = asJson.getJSONArray("activeFlowTiles");
		if (activeFlowTilesJson != null) {
			for (int cursor = 0; cursor < activeFlowTilesJson.size(); cursor++) {
				JSONObject tilePositionJson = activeFlowTilesJson.getJSONObject(cursor);
				GridPoint2 location = JSONUtils.gridPoint2(tilePositionJson);
				MapTile tile = savedGameStateHolder.getMap().getTile(location);
				if (tile == null) {
					throw new InvalidSaveException("Can not find tile at " + location);
				} else {
					activeLiquidFlowTiles.add(tile);
				}
			}
		}

		JSONObject productionQuotasJson = asJson.getJSONObject("productionQuotas");
		for (Map.Entry<String, Object> entry : productionQuotasJson.entrySet()) {
			ItemType itemType = relatedStores.itemTypeDictionary.getByName(entry.getKey());
			if (itemType == null) {
				throw new InvalidSaveException("Could not find item type by name " + entry.getKey());
			}
			JSONObject quotaJson = (JSONObject) entry.getValue();
			ProductionQuota quota = new ProductionQuota();
			quota.readFrom(quotaJson, savedGameStateHolder, relatedStores);
			itemTypeProductionQuotas.put(itemType, quota);
		}

		JSONObject productionAssignmentsJson = asJson.getJSONObject("productionAssignments");
		for (Map.Entry<String, Object> entry : productionAssignmentsJson.entrySet()) {
			ItemType itemType = relatedStores.itemTypeDictionary.getByName(entry.getKey());
			if (itemType == null) {
				throw new InvalidSaveException("Could not find item type by name " + entry.getKey());
			}
			Map<Long, ProductionAssignment> productionAssignmentMap = new HashMap<>();
			JSONArray productionAssignmentIds = (JSONArray) entry.getValue();
			for (int cursor = 0; cursor < productionAssignmentIds.size(); cursor++) {
				Long productionAssignmentId = productionAssignmentIds.getLong(cursor);
				ProductionAssignment productionAssignment = savedGameStateHolder.productionAssignments.get(productionAssignmentId);
				if (productionAssignment == null) {
					throw new InvalidSaveException("Could not find production assignment by ID " + productionAssignmentId);
				}
				productionAssignmentMap.put(productionAssignment.productionAssignmentId, productionAssignment);
			}
			itemTypeProductionAssignments.put(itemType, productionAssignmentMap);
		}

		JSONObject itemCounts = asJson.getJSONObject("requiredItemCounts");
		for (Map.Entry<String, Object> entry : itemCounts.entrySet()) {
			ItemType itemType = relatedStores.itemTypeDictionary.getByName(entry.getKey());
			if (itemType == null) {
				throw new InvalidSaveException("Could not find item type by name " + entry.getKey());
			}
			requiredItemCounts.put(itemType, (Integer) entry.getValue());
		}

		JSONObject liquidProductionQuotasJson = asJson.getJSONObject("liquidProductionQuotas");
		if (liquidProductionQuotasJson != null) {
			for (Map.Entry<String, Object> entry : liquidProductionQuotasJson.entrySet()) {
				GameMaterial liquidMaterial = relatedStores.gameMaterialDictionary.getByName(entry.getKey());
				if (liquidMaterial == null) {
					throw new InvalidSaveException("Could not find liquid material by name " + entry.getKey());
				}
				JSONObject quotaJson = (JSONObject) entry.getValue();
				ProductionQuota quota = new ProductionQuota();
				quota.readFrom(quotaJson, savedGameStateHolder, relatedStores);
				liquidProductionQuotas.put(liquidMaterial, quota);
			}
		}

		JSONObject liquidProductionAssignmentsJson = asJson.getJSONObject("liquidProductionAssignments");
		if (liquidProductionAssignmentsJson != null) {
			for (Map.Entry<String, Object> entry : liquidProductionAssignmentsJson.entrySet()) {
				GameMaterial liquidMaterial = relatedStores.gameMaterialDictionary.getByName(entry.getKey());
				if (liquidMaterial == null) {
					throw new InvalidSaveException("Could not find liquid material by name " + entry.getKey());
				}
				Map<Long, ProductionAssignment> productionAssignmentMap = new HashMap<>();
				JSONArray productionAssignmentIds = (JSONArray) entry.getValue();
				for (int cursor = 0; cursor < productionAssignmentIds.size(); cursor++) {
					Long productionAssignmentId = productionAssignmentIds.getLong(cursor);
					ProductionAssignment productionAssignment = savedGameStateHolder.productionAssignments.get(productionAssignmentId);
					if (productionAssignment == null) {
						throw new InvalidSaveException("Could not find production assignment by ID " + productionAssignmentId);
					}
					productionAssignmentMap.put(productionAssignment.productionAssignmentId, productionAssignment);
				}
				liquidProductionAssignments.put(liquidMaterial, productionAssignmentMap);
			}
		}

		JSONObject liquidCounts = asJson.getJSONObject("requiredLiquidCounts");
		if (liquidCounts != null) {
			for (Map.Entry<String, Object> entry : liquidCounts.entrySet()) {
				GameMaterial liquidMaterial = relatedStores.gameMaterialDictionary.getByName(entry.getKey());
				if (liquidMaterial == null) {
					throw new InvalidSaveException("Could not find liquid material by name " + entry.getKey());
				}
				if (entry.getValue() instanceof Number) {
					requiredLiquidCounts.put(liquidMaterial, ((Number) entry.getValue()).floatValue());
				} else {
					throw new InvalidSaveException("Unrecognised type " + entry.getValue().getClass() + " for parsing requiredLiquidCounts");
				}
			}
		}

		JSONArray previousHintsJson = asJson.getJSONArray("previousHints");
		if (previousHintsJson != null) {
			for (Object hintIdObj : previousHintsJson) {
				previousHints.put(hintIdObj.toString(), true);
			}
		}

		JSONArray currentHintsJson = asJson.getJSONArray("currentHints");
		if (currentHintsJson != null) {
			for (Object o : currentHintsJson) {
				currentHints.add(o.toString());
			}
		}

		this.immigrantsDue = asJson.getIntValue("immigrantsDue");
		this.immigrantCounter = asJson.getIntValue("immigrantCounter");
		this.immigrationPoint = JSONUtils.vector2(asJson.getJSONObject("immigrationPoint"));
		this.nextImmigrationGameTime = asJson.getDouble("nextImmigration");

		JSONArray notificationsJson = asJson.getJSONArray("notifications");
		if (notificationsJson != null) {
			for (int cursor = 0; cursor < notificationsJson.size(); cursor++) {
				JSONObject notificationJson = notificationsJson.getJSONObject(cursor);
				Notification notification = new Notification();
				notification.readFrom(notificationJson, savedGameStateHolder, relatedStores);
				this.queuedNotifications.add(notification);
			}
		}

		JSONArray collapsesJson = asJson.getJSONArray("impendingCollapses");
		if (collapsesJson != null) {
			for (int cursor = 0; cursor < collapsesJson.size(); cursor++) {
				JSONObject collapseJson = collapsesJson.getJSONObject(cursor);
				ImpendingMiningCollapse impendingCollapse = new ImpendingMiningCollapse();
				impendingCollapse.readFrom(collapseJson, savedGameStateHolder, relatedStores);
				this.impendingMiningCollapses.add(impendingCollapse);
			}
		}

		this.gameState = EnumParser.getEnumValue(asJson, "gameState", GameState.class, NORMAL);


		JSONObject craftingRecipePriorityJson = asJson.getJSONObject("craftingRecipePriority");
		if (craftingRecipePriorityJson != null) {
			for (Map.Entry<String, Object> entry : craftingRecipePriorityJson.entrySet()) {
				CraftingRecipe recipe = relatedStores.craftingRecipeDictionary.getByName(entry.getKey());
				if (recipe == null) {
					throw new InvalidSaveException("Could not find crafting recipe by name " + entry.getKey());
				} else {
					craftingRecipePriority.put(recipe, JobPriority.valueOf(entry.getValue().toString()));
				}
			}
		}

		JSONObject craftingRecipeSelectionsJson = asJson.getJSONObject("craftingRecipeSelections");
		if (craftingRecipeSelectionsJson != null) {
			for (Map.Entry<String, Object> entry : craftingRecipeSelectionsJson.entrySet()) {
				CraftingRecipe recipe = relatedStores.craftingRecipeDictionary.getByName(entry.getKey());
				if (recipe == null) {
					throw new InvalidSaveException("Could not find crafting recipe by name " + entry.getKey());
				} else {
					JSONObject selectionJson = (JSONObject) entry.getValue();
					CraftingRecipeMaterialSelection selection = new CraftingRecipeMaterialSelection();
					selection.readFrom(selectionJson, savedGameStateHolder, relatedStores);
					craftingRecipeMaterialSelections.put(recipe, selection);
				}
			}
		}

		JSONArray usedTwitchViewersJson = asJson.getJSONArray("usedTwitchViewers");
		if (usedTwitchViewersJson != null) {
			for (Object o : usedTwitchViewersJson) {
				usedTwitchViewers.add(new TwitchViewer(o.toString()));
			}
		}

		String settlerRaceName = asJson.getString("settlerRace");
		if (settlerRaceName == null) {
			settlerRaceName = "Dwarf";
		}
		this.settlerRace = relatedStores.raceDictionary.getByName(settlerRaceName);

		this.fishRemainingInRiver = asJson.getIntValue("fishRemaining");
	}

	public Race getSettlerRace() {
		return settlerRace;
	}

	public void setSettlerRace(Race settlerRace) {
		this.settlerRace = settlerRace;
	}
}
