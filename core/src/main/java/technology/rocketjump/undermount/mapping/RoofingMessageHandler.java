package technology.rocketjump.undermount.mapping;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RoofConstructionQueueMessage;

@Singleton
public class RoofingMessageHandler implements Telegraph, GameContextAware {

	private final MessageDispatcher messageDispatcher;
	private final RoofConstructionManager roofConstructionManager;
	private GameContext gameContext;

	@Inject
	public RoofingMessageHandler(MessageDispatcher messageDispatcher, RoofConstructionManager roofConstructionManager) {
		this.messageDispatcher = messageDispatcher;
		this.roofConstructionManager = roofConstructionManager;

		messageDispatcher.addListener(this, MessageType.ROOF_CONSTRUCTION_QUEUE_CHANGE);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.ROOF_CONSTRUCTION_QUEUE_CHANGE: {
				return handle((RoofConstructionQueueMessage) msg.extraInfo);
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this + ", " + msg);
		}
	}

	private boolean handle(RoofConstructionQueueMessage message) {
		if (message.parentTile.getRoof().getState().equals(TileRoofState.OPEN)) {
			if (message.roofConstructionQueued) {
				roofConstructionManager.roofConstructionAdded(message.parentTile);
			} else {
				roofConstructionManager.roofConstructionRemoved(message.parentTile);
			}
		}
		return true;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
