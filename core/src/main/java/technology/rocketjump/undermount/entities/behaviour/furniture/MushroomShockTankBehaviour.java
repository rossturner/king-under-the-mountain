package technology.rocketjump.undermount.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.common.collect.Lists;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.components.LiquidContainerComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.TransformItemMessage;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.ui.i18n.I18nString;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nWord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.undermount.entities.behaviour.furniture.MushroomShockTankBehaviour.MushrooomShockTankState.*;
import static technology.rocketjump.undermount.entities.components.ItemAllocation.Purpose.HELD_IN_INVENTORY;
import static technology.rocketjump.undermount.ui.i18n.I18nTranslator.oneDecimalFormat;

public class MushroomShockTankBehaviour extends FillLiquidContainerBehaviour implements SelectableDescription {

	private MushrooomShockTankState state = WAITING_FOR_LIQUID;
	private double TIME_TO_SHOCK_MUSHROOM_LOG;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		super.init(parentEntity, messageDispatcher, gameContext);
		TIME_TO_SHOCK_MUSHROOM_LOG = gameContext.getConstantsRepo().getSettlementConstants().getMushroomShockTimeHours();
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		super.destroy(parentEntity, messageDispatcher, gameContext);
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);
		if (parentEntity.isOnFire()) {
			return;
		}

		LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);
		InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);

		switch (state) {
			case WAITING_FOR_LIQUID: {
				if (liquidContainerComponent.getLiquidQuantity() >= liquidContainerComponent.getMaxLiquidCapacity()) {
					state = AVAILABLE;
				}
				break;
			}
			case AVAILABLE: {
				if (liquidContainerComponent.getLiquidQuantity() < liquidContainerComponent.getMaxLiquidCapacity()) {
					state = WAITING_FOR_LIQUID;
				}
				break;
			}
			case ASSIGNED: {
				if (shockingProgress(gameContext) >= 1f) {
					InventoryComponent.InventoryEntry inventoryEntry = getPrimaryItemFromInventory();
					messageDispatcher.dispatchMessage(MessageType.TRANSFORM_ITEM_TYPE, new TransformItemMessage(inventoryEntry.entity, relatedItemTypes.get(2)));
					inventoryEntry.entity.getOrCreateComponent(ItemAllocationComponent.class).cancelAll(HELD_IN_INVENTORY);
					state = LOG_SHOCK_COMPLETE;
				}
				break;
			}
			case LOG_SHOCK_COMPLETE: {
				if (inventoryComponent.isEmpty()) {
					state = AVAILABLE;
				}
				break;
			}
		}
	}

	public MushrooomShockTankState getState() {
		return state;
	}


	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext) {
		Map<String, I18nString> replacements = new HashMap<>();
		float progress = 100f * shockingProgress(gameContext);
		replacements.put("progress", new I18nWord("progress", oneDecimalFormat.format(progress)));
		replacements.put("itemType", i18nTranslator.getDictionary().getWord(relatedItemTypes.get(1).getI18nKey()));
		return Lists.newArrayList(i18nTranslator.getTranslatedWordWithReplacements(state.descriptionI18nKey, replacements));
	}

	private float shockingProgress(GameContext gameContext) {
		if (getSecondaryItemFromInventory() != null) {
			// Already transformed to shocked log
			return 1f;
		}
		InventoryComponent.InventoryEntry inventoryEntry = getPrimaryItemFromInventory();
		if (inventoryEntry != null) {
			double inventoryTime = inventoryEntry.getLastUpdateGameTime();
			double targetTime = inventoryTime + TIME_TO_SHOCK_MUSHROOM_LOG;
			double currentTime = gameContext.getGameClock().getCurrentGameTime();
			if (currentTime > targetTime) {
				return 1f;
			} else {
				return 1f - (float) ((targetTime - currentTime) / TIME_TO_SHOCK_MUSHROOM_LOG);
			}
		} else {
			return 0f;
		}
	}

	private InventoryComponent.InventoryEntry getPrimaryItemFromInventory() {
		InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);
		if (!inventoryComponent.isEmpty()) {
			for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) inventoryEntry.entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getItemType().equals(relatedItemTypes.get(1))) {
					return inventoryEntry;
				}
			}
		}
		return null;
	}

	private InventoryComponent.InventoryEntry getSecondaryItemFromInventory() {
		InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);
		if (!inventoryComponent.isEmpty()) {
			for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) inventoryEntry.entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getItemType().equals(relatedItemTypes.get(2))) {
					return inventoryEntry;
				}
			}
		}
		return null;
	}

	public void setState(MushrooomShockTankState state) {
		this.state = state;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		if (!WAITING_FOR_LIQUID.equals(state)) {
			asJson.put("state", state.name());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.state = EnumParser.getEnumValue(asJson, "state", MushrooomShockTankState.class, WAITING_FOR_LIQUID);
	}

	public enum MushrooomShockTankState {

		WAITING_FOR_LIQUID("FURNITURE.DESCRIPTION.WAITING_FOR_LIQUID"),
		AVAILABLE("FURNITURE.DESCRIPTION.WAITING_FOR_ITEM"),
		ASSIGNED("FURNITURE.DESCRIPTION.SHOCKING_IN_PROGRESS"),
		LOG_SHOCK_COMPLETE("FURNITURE.DESCRIPTION.SHOCKING_IN_PROGRESS");

		public final String descriptionI18nKey;

		MushrooomShockTankState(String descriptionI18nKey) {
			this.descriptionI18nKey = descriptionI18nKey;
		}
	}
}
