package technology.rocketjump.undermount.rooms.components.behaviour;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.rooms.components.RoomComponent;

public abstract class RoomBehaviourComponent extends RoomComponent {
	public RoomBehaviourComponent(Room parent, MessageDispatcher messageDispatcher) {
		super(parent, messageDispatcher);
	}

	public abstract void infrequentUpdate(GameContext gameContext, MessageDispatcher messageDispatcher);

}
