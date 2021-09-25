package technology.rocketjump.undermount.mapping;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.underground.PipeConstructionState;
import technology.rocketjump.undermount.mapping.tile.underground.UnderTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.PipeConstructionMessage;
import technology.rocketjump.undermount.messaging.types.TileConstructionQueueMessage;
import technology.rocketjump.undermount.messaging.types.TileDeconstructionQueueMessage;
import technology.rocketjump.undermount.particles.ParticleEffectTypeDictionary;

@Singleton
public class PipingMessageHandler implements Telegraph, GameContextAware {

	private final MessageDispatcher messageDispatcher;
	private final PipeConstructionManager pipeConstructionManager;
	private final OutdoorLightProcessor outdoorLightProcessor;
	private GameContext gameContext;

	@Inject
	public PipingMessageHandler(MessageDispatcher messageDispatcher, PipeConstructionManager pipeConstructionManager,
								OutdoorLightProcessor outdoorLightProcessor, ParticleEffectTypeDictionary particleEffectTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.pipeConstructionManager = pipeConstructionManager;
		this.outdoorLightProcessor = outdoorLightProcessor;

		messageDispatcher.addListener(this, MessageType.PIPE_CONSTRUCTION_QUEUE_CHANGE);
		messageDispatcher.addListener(this, MessageType.PIPE_DECONSTRUCTION_QUEUE_CHANGE);
		messageDispatcher.addListener(this, MessageType.PIPE_CONSTRUCTED);
		messageDispatcher.addListener(this, MessageType.PIPE_DECONSTRUCTED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.PIPE_CONSTRUCTION_QUEUE_CHANGE: {
				return handle((TileConstructionQueueMessage) msg.extraInfo);
			}
			case MessageType.PIPE_DECONSTRUCTION_QUEUE_CHANGE: {
				return handle((TileDeconstructionQueueMessage) msg.extraInfo);
			}
			case MessageType.PIPE_CONSTRUCTED: {
				return pipeConstructed((PipeConstructionMessage) msg.extraInfo);
			}
			case MessageType.PIPE_DECONSTRUCTED: {
				return pipeDeconstructed((PipeConstructionMessage) msg.extraInfo);
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this + ", " + msg);
		}
	}

	private boolean handle(TileConstructionQueueMessage message) {
		if (message.constructionQueued) {

			pipeConstructionManager.pipeConstructionAdded(message.parentTile);
		} else {
			pipeConstructionManager.pipeConstructionRemoved(message.parentTile);
		}
		return true;
	}

	private boolean handle(TileDeconstructionQueueMessage message) {
		UnderTile underTile = message.parentTile.getUnderTile();
		if (underTile != null && underTile.getPipeEntity() != null) {
			if (message.deconstructionQueued) {
				pipeConstructionManager.pipeDeconstructionAdded(message.parentTile);
			} else {
				pipeConstructionManager.pipeDeconstructionRemoved(message.parentTile);
			}
		}
		return true;
	}

	private boolean pipeConstructed(PipeConstructionMessage message) {
		MapTile tile = gameContext.getAreaMap().getTile(message.tilePosition);
		if (tile != null) {
			UnderTile underTile = tile.getOrCreateUnderTile();
			underTile.setPipeConstructionState(PipeConstructionState.NONE);
			messageDispatcher.dispatchMessage(MessageType.ADD_PIPE, new PipeConstructionMessage(
					tile.getTilePosition(), message.material));
		}
		return true;
	}

	private boolean pipeDeconstructed(PipeConstructionMessage message) {
		MapTile tile = gameContext.getAreaMap().getTile(message.tilePosition);
		if (tile != null) {
			UnderTile underTile = tile.getOrCreateUnderTile();
			underTile.setPipeConstructionState(PipeConstructionState.NONE);
			messageDispatcher.dispatchMessage(MessageType.REMOVE_PIPE, message.tilePosition);
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
