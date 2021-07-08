package technology.rocketjump.undermount.mapping;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
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
import technology.rocketjump.undermount.messaging.types.RoofDeconstructionQueueMessage;

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
		messageDispatcher.addListener(this, MessageType.ROOF_DECONSTRUCTION_QUEUE_CHANGE);
		messageDispatcher.addListener(this, MessageType.ROOF_CONSTRUCTED);
		messageDispatcher.addListener(this, MessageType.ROOF_DECONSTRUCTED);
		messageDispatcher.addListener(this, MessageType.ROOF_SUPPORT_REMOVED);
		messageDispatcher.addListener(this, MessageType.WALL_REMOVED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.ROOF_CONSTRUCTION_QUEUE_CHANGE: {
				return handle((RoofConstructionQueueMessage) msg.extraInfo);
			}
			case MessageType.ROOF_DECONSTRUCTION_QUEUE_CHANGE: {
				return handle((RoofDeconstructionQueueMessage) msg.extraInfo);
			}
			case MessageType.ROOF_CONSTRUCTED: {
				return roofConstructed((RoofConstructionMessage) msg.extraInfo);
			}
			case MessageType.ROOF_DECONSTRUCTED: {
				return roofDeconstructed((RoofConstructionMessage) msg.extraInfo);
			}
			case MessageType.ROOF_SUPPORT_REMOVED:
			case MessageType.WALL_REMOVED: {
				GridPoint2 location = (GridPoint2) msg.extraInfo;
				roofConstructionManager.supportDeconstructed(gameContext.getAreaMap().getTile(location));
				return true;
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

	private boolean handle(RoofDeconstructionQueueMessage message) {
		if (message.parentTile.getRoof().getState().equals(TileRoofState.CONSTRUCTED)) {
			if (message.roofDeconstructionQueued) {
				roofConstructionManager.roofDeconstructionAdded(message.parentTile);
			} else {
				roofConstructionManager.roofConstructionRemoved(message.parentTile);
			}
		}
		return true;
	}

	private boolean roofConstructed(RoofConstructionMessage message) {
		MapTile tile = gameContext.getAreaMap().getTile(message.roofTileLocation);
		if (tile != null) {
			tile.getRoof().setState(TileRoofState.CONSTRUCTED);
			tile.getRoof().setRoofMaterial(message.roofMaterial);
			tile.getRoof().setConstructionState(RoofConstructionState.NONE);
			propagateDarknessFromTile(tile, gameContext, outdoorLightProcessor);
			roofConstructionManager.roofConstructed(tile);
		}
		return true;
	}

	private boolean roofDeconstructed(RoofConstructionMessage message) {
		MapTile tile = gameContext.getAreaMap().getTile(message.roofTileLocation);
		if (tile != null) {
			tile.getRoof().setState(TileRoofState.OPEN);
			tile.getRoof().setConstructionState(RoofConstructionState.NONE);
			MapMessageHandler.markAsOutside(tile, gameContext, outdoorLightProcessor);
			roofConstructionManager.roofDeconstructed(tile);
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
