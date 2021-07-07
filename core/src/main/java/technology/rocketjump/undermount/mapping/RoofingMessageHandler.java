package technology.rocketjump.undermount.mapping;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.roof.RoofConstructionState;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RoofConstructionMessage;
import technology.rocketjump.undermount.messaging.types.RoofConstructionQueueMessage;

import static technology.rocketjump.undermount.mapping.MapMessageHandler.propagateDarknessFromTile;

@Singleton
public class RoofingMessageHandler implements Telegraph, GameContextAware {

	private final MessageDispatcher messageDispatcher;
	private final RoofConstructionManager roofConstructionManager;
	private final OutdoorLightProcessor outdoorLightProcessor;
	private GameContext gameContext;

	@Inject
	public RoofingMessageHandler(MessageDispatcher messageDispatcher, RoofConstructionManager roofConstructionManager,
								 OutdoorLightProcessor outdoorLightProcessor) {
		this.messageDispatcher = messageDispatcher;
		this.roofConstructionManager = roofConstructionManager;
		this.outdoorLightProcessor = outdoorLightProcessor;

		messageDispatcher.addListener(this, MessageType.ROOF_CONSTRUCTION_QUEUE_CHANGE);
		messageDispatcher.addListener(this, MessageType.ROOF_CONSTRUCTED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.ROOF_CONSTRUCTION_QUEUE_CHANGE: {
				return handle((RoofConstructionQueueMessage) msg.extraInfo);
			}
			case MessageType.ROOF_CONSTRUCTED: {
				return handle((RoofConstructionMessage) msg.extraInfo);
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

	private boolean handle(RoofConstructionMessage constructionMessage) {
		MapTile tile = gameContext.getAreaMap().getTile(constructionMessage.roofTileLocation);
		if (tile != null) {
			tile.getRoof().setState(TileRoofState.CONSTRUCTED);
			tile.getRoof().setConstructionState(RoofConstructionState.NONE);
			propagateDarknessFromTile(tile, gameContext, outdoorLightProcessor);
			roofConstructionManager.roofConstructed(tile);
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
