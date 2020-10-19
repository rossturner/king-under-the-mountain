package technology.rocketjump.undermount.ui.actions;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.ui.GameInteractionMode;
import technology.rocketjump.undermount.ui.views.FurnitureSelectedCallback;

public class FurnitureSelectedAction implements ButtonAction {

	private final FurnitureType selectedFurnitureType;
	private final MessageDispatcher messageDispatcher;
	private final FurnitureSelectedCallback callback;

	public FurnitureSelectedAction(FurnitureType selectedFurnitureType, MessageDispatcher messageDispatcher, FurnitureSelectedCallback callback) {
		this.selectedFurnitureType = selectedFurnitureType;
		this.messageDispatcher = messageDispatcher;
		this.callback = callback;
	}

	@Override
	public void onClick() {
		if (selectedFurnitureType != null) {
			callback.furnitureTypeSelected(selectedFurnitureType);
			GameInteractionMode.PLACE_FURNITURE.setFurnitureType(selectedFurnitureType);
			messageDispatcher.dispatchMessage(MessageType.GUI_FURNITURE_TYPE_SELECTED, selectedFurnitureType);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_FURNITURE);
		}
	}
}
