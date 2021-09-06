package technology.rocketjump.undermount.entities.ai.goap.actions.nourishment;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.ai.goap.actions.Action;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.tags.LiquidContainerTag;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.LookupItemTypesByTagClassMessage;
import technology.rocketjump.undermount.messaging.types.RequestHaulingAllocationMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rooms.HaulingAllocation;

import java.util.Collections;
import java.util.List;

import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class LocateLiquidContainerAction extends Action implements RequestHaulingAllocationMessage.ItemAllocationCallback,
		LookupItemTypesByTagClassMessage.LookupItemTypesCallback {

	private List<ItemType> itemTypes = Collections.emptyList();

	public LocateLiquidContainerAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {

		parent.messageDispatcher.dispatchMessage(MessageType.LOOKUP_ITEM_TYPES_BY_TAG_CLASS, new LookupItemTypesByTagClassMessage(
				LiquidContainerTag.class, this));

		// TODO would be better to request any item of multiple types at once rather than loop through different types like this
		// so nearest item is always found first
		Collections.shuffle(itemTypes, gameContext.getRandom());

		for (ItemType itemType : itemTypes) {
			parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION, new RequestHaulingAllocationMessage(
					parent.parentEntity, parent.parentEntity.getLocationComponent().getWorldOrParentPosition(),
					itemType, null, false, 1,
					null, this));

			if (completionType != null) {
				break;
			}
		}

		if (completionType == null) {
			completionType = FAILURE;
		}
	}

	@Override
	public void itemTypesFound(List<ItemType> itemTypes) {
		this.itemTypes = itemTypes;
	}

	@Override
	public void allocationFound(HaulingAllocation haulingAllocation) {
		if (haulingAllocation != null) {
			parent.setAssignedHaulingAllocation(haulingAllocation);
			completionType = SUCCESS;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
