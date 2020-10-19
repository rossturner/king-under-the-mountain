package technology.rocketjump.undermount.settlement;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.mapping.model.ImpendingMiningCollapse;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.settlement.notifications.Notification;
import technology.rocketjump.undermount.settlement.production.ProductionAssignment;
import technology.rocketjump.undermount.settlement.production.ProductionQuota;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple bean to hold "global" settlement data
 */
public class SettlementState implements Persistable {

	public final Map<Long, Entity> furnitureHoldingCompletedCooking = new HashMap<>();
	public final Map<ItemType, ProductionQuota> itemTypeProductionQuotas = new HashMap<>();
	public final Map<ItemType, Map<Long, ProductionAssignment>> productionAssignments = new HashMap<>();
	public final Map<ItemType, Integer> requiredItemCounts = new HashMap<>();
	public final List<Notification> queuedNotifications = new ArrayList<>();
	public final List<ImpendingMiningCollapse> impendingMiningCollapses = new ArrayList<>();
	public final Map<String, Boolean> previousHints = new HashMap<>();
	public final List<String> currentHints = new ArrayList<>();

	private int immigrantsDue;
	private int immigrantCounter;
	private Vector2 immigrationPoint;
	private Double nextImmigrationGameTime;
	private boolean gameOver;

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		JSONObject asJson = savedGameStateHolder.settlementStateJson;

		JSONObject furnitureEntityJson = new JSONObject(true);
		for (Map.Entry<Long, Entity> entry : furnitureHoldingCompletedCooking.entrySet()) {
			furnitureEntityJson.put(String.valueOf(entry.getKey()), entry.getValue().getId());
		}
		asJson.put("furnitureHoldingCompletedCooking", furnitureEntityJson);

		JSONObject productionQuotasJson = new JSONObject(true);
		for (Map.Entry<ItemType, ProductionQuota> entry : itemTypeProductionQuotas.entrySet()) {
			JSONObject quotaAsJson = new JSONObject(true);
			entry.getValue().writeTo(quotaAsJson, savedGameStateHolder);
			productionQuotasJson.put(entry.getKey().getItemTypeName(), quotaAsJson);
		}
		asJson.put("productionQuotas", productionQuotasJson);

		JSONObject productionAssignmentsJson = new JSONObject(true);
		for (Map.Entry<ItemType, Map<Long, ProductionAssignment>> itemTypeMapEntry : productionAssignments.entrySet()) {
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

		if (gameOver) {
			asJson.put("gameOver", true);
		}

		savedGameStateHolder.setSettlementState(this);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
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
			productionAssignments.put(itemType, productionAssignmentMap);
		}

		JSONObject itemCounts = asJson.getJSONObject("requiredItemCounts");
		for (Map.Entry<String, Object> entry : itemCounts.entrySet()) {
			ItemType itemType = relatedStores.itemTypeDictionary.getByName(entry.getKey());
			if (itemType == null) {
				throw new InvalidSaveException("Could not find item type by name " + entry.getKey());
			}
			requiredItemCounts.put(itemType, (Integer)entry.getValue());
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

		gameOver = asJson.getBooleanValue("gameOver");
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

	public void setGameOver(boolean gameOver) {
		this.gameOver = gameOver;
	}

	public boolean isGameOver() {
		return gameOver;
	}

	public Map<String, Boolean> getPreviousHints() {
		return previousHints;
	}

	public List<String> getCurrentHints() {
		return currentHints;
	}
}
