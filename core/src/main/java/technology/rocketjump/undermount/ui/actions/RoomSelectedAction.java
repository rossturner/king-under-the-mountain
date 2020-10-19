package technology.rocketjump.undermount.ui.actions;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rooms.RoomType;
import technology.rocketjump.undermount.ui.GameInteractionMode;
import technology.rocketjump.undermount.ui.views.GuiViewName;

public class RoomSelectedAction extends SwitchGuiViewAction {

	private final RoomType selectedRoomType;

	public RoomSelectedAction(RoomType selectedRoomType, MessageDispatcher messageDispatcher) {
		super(GuiViewName.ROOM_SIZING, messageDispatcher);
		this.selectedRoomType = selectedRoomType;
	}

	@Override
	public void onClick() {
		if (selectedRoomType != null) {
			messageDispatcher.dispatchMessage(MessageType.GUI_ROOM_TYPE_SELECTED, selectedRoomType);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_ROOM);
		}
		super.onClick();
	}
}
