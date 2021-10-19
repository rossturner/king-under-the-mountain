package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.LookupMessage;
import technology.rocketjump.undermount.messaging.types.RequestHaulingAllocationMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class FindItemAction extends Action implements ItemTypeLookupCallback {

	private List<ItemType> itemTypesToLookFor = new ArrayList<>();

	public FindItemAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (parent.getRelevantMemory().getRelatedAmmoType() != null) {
			parent.messageDispatcher.dispatchMessage(MessageType.LOOKUP_ITEM_TYPE, new LookupMessage(EntityType.ITEM,
					parent.getRelevantMemory().getRelatedAmmoType().name(), this));

		} else if (parent.getRelevantMemory().getRelatedItemType() != null) {
			itemTypesToLookFor.add(parent.getRelevantMemory().getRelatedItemType());
		}

		Collections.shuffle(itemTypesToLookFor, gameContext.getRandom());

		for (ItemType itemType : itemTypesToLookFor) {
			parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION, new RequestHaulingAllocationMessage(
					parent.parentEntity,
					parent.parentEntity.getLocationComponent().getWorldOrParentPosition(), itemType,
					parent.getRelevantMemory().getRelatedMaterial(),
					true, null, null, (allocation) -> {
				if (allocation != null) {
					parent.setAssignedHaulingAllocation(allocation);
					completionType = SUCCESS;
				}
			}));

			if (completionType != null) {
				break;
			}
		}

		if (completionType == null) {
			completionType = FAILURE;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state to read
	}

	@Override
	public void itemTypeFound(Optional<ItemType> itemTypeLookup) {
		itemTypeLookup.ifPresent(itemType -> this.itemTypesToLookFor.add(itemType));
	}

	@Override
	public void itemTypesFound(List<ItemType> itemTypes) {
		this.itemTypesToLookFor.addAll(itemTypes);
	}
}
