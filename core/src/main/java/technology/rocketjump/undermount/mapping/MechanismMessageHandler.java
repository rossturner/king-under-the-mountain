package technology.rocketjump.undermount.mapping;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.behaviour.mechanisms.PowerMechanismBehaviour;
import technology.rocketjump.undermount.entities.factories.MechanismEntityAttributesFactory;
import technology.rocketjump.undermount.entities.factories.MechanismEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.underground.PowerGrid;
import technology.rocketjump.undermount.mapping.tile.underground.UnderTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.MechanismConstructionMessage;
import technology.rocketjump.undermount.messaging.types.MechanismPlacementMessage;
import technology.rocketjump.undermount.messaging.types.TileConstructionQueueMessage;
import technology.rocketjump.undermount.messaging.types.TileDeconstructionQueueMessage;

import static technology.rocketjump.undermount.mapping.tile.CompassDirection.oppositeOf;

@Singleton
public class MechanismMessageHandler implements Telegraph, GameContextAware {

	private final MessageDispatcher messageDispatcher;
	private final MechanismConstructionManager mechanismConstructionManager;
	private final MechanismEntityAttributesFactory mechanismEntityAttributesFactory;
	private final MechanismEntityFactory mechanismEntityFactory;
	private GameContext gameContext;

	@Inject
	public MechanismMessageHandler(MessageDispatcher messageDispatcher, MechanismConstructionManager mechanismConstructionManager,
								   MechanismEntityAttributesFactory mechanismEntityAttributesFactory, MechanismEntityFactory mechanismEntityFactory) {
		this.messageDispatcher = messageDispatcher;
		this.mechanismConstructionManager = mechanismConstructionManager;
		this.mechanismEntityAttributesFactory = mechanismEntityAttributesFactory;
		this.mechanismEntityFactory = mechanismEntityFactory;

		messageDispatcher.addListener(this, MessageType.MECHANISM_CONSTRUCTION_ADDED);
		messageDispatcher.addListener(this, MessageType.MECHANISM_CONSTRUCTION_REMOVED);
		messageDispatcher.addListener(this, MessageType.MECHANISM_DECONSTRUCTION_QUEUE_CHANGE);
		messageDispatcher.addListener(this, MessageType.MECHANISM_CONSTRUCTED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.MECHANISM_CONSTRUCTION_ADDED: {
				return handle((MechanismPlacementMessage) msg.extraInfo);
			}
			case MessageType.MECHANISM_CONSTRUCTION_REMOVED: {
				return handleConstructionRemoved((TileConstructionQueueMessage) msg.extraInfo);
			}
			case MessageType.MECHANISM_DECONSTRUCTION_QUEUE_CHANGE: {
				return handle((TileDeconstructionQueueMessage) msg.extraInfo);
			}
			case MessageType.MECHANISM_CONSTRUCTED: {
				return mechanismConstructed((MechanismConstructionMessage) msg.extraInfo);
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this + ", " + msg);
		}
	}

	private boolean handle(MechanismPlacementMessage mechanismPlacementMessage) {
		mechanismConstructionManager.mechanismConstructionAdded(mechanismPlacementMessage.mapTile, mechanismPlacementMessage.mechanismType);
		return true;
	}

	private boolean handleConstructionRemoved(TileConstructionQueueMessage message) {
		mechanismConstructionManager.mechanismConstructionRemoved(message.parentTile);
		return true;
	}


	private boolean handle(TileDeconstructionQueueMessage message) {
		UnderTile underTile = message.parentTile.getUnderTile();
		if (underTile != null && underTile.getPowerMechanismEntity() != null) {
			if (message.deconstructionQueued) {
				mechanismConstructionManager.mechanismDeconstructionAdded(message.parentTile);
			} else {
				mechanismConstructionManager.mechanismDeconstructionRemoved(message.parentTile);
			}
		}
		return true;
	}

	private boolean mechanismConstructed(MechanismConstructionMessage message) {
		MapTile tile = gameContext.getAreaMap().getTile(message.location);
		if (tile != null) {
			MechanismEntityAttributes attributes = mechanismEntityAttributesFactory.byType(message.mechanismType, message.material);
			Entity mechanismEntity = mechanismEntityFactory.create(attributes, message.location, new PowerMechanismBehaviour(), gameContext);
			messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, mechanismEntity);
			tile.removeEntity(mechanismEntity.getId());
			UnderTile underTile = tile.getOrCreateUnderTile();
			underTile.setQueuedMechanismType(null);
			underTile.setPowerMechanismEntity(mechanismEntity);
			createPowerGrid(tile);
		}
		return true;
	}

	private void createPowerGrid(MapTile tile) {
		PowerGrid grid = new PowerGrid(SequentialIdGenerator.nextId());
		grid.addTile(tile);

		Entity entity = tile.getUnderTile().getPowerMechanismEntity();
		MechanismType mechanismType = ((MechanismEntityAttributes)entity.getPhysicalEntityComponent().getAttributes()).getMechanismType();
		for (CompassDirection powerTransmissionDirection : mechanismType.getPowerTransmission()) {
			MapTile tileInDirection = gameContext.getAreaMap().getTile(tile.getTilePosition().x + powerTransmissionDirection.getXOffset(), tile.getTileY() + powerTransmissionDirection.getYOffset());
			if (tileInDirection.getUnderTile() != null) {
				PowerGrid powerGridInDirection = tileInDirection.getUnderTile().getPowerGrid();
				if (powerGridInDirection != null) {
					MechanismEntityAttributes attributesInDirection = (MechanismEntityAttributes) tileInDirection.getUnderTile().getPowerMechanismEntity().getPhysicalEntityComponent().getAttributes();
					if (attributesInDirection.getMechanismType().getPowerTransmission().contains(oppositeOf(powerTransmissionDirection))) {
						grid = powerGridInDirection.mergeIn(grid, gameContext);
					}
				}
			}
		}

	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

//	private boolean pipeConstructed(PipeConstructionMessage message) {
//		MapTile tile = gameContext.getAreaMap().getTile(message.tilePosition);
//		if (tile != null) {
//			UnderTile underTile = tile.getOrCreateUnderTile();
//			underTile.setPipeConstructionState(PipeConstructionState.NONE);
//			messageDispatcher.dispatchMessage(MessageType.ADD_PIPE, new PipeConstructionMessage(
//					tile.getTilePosition(), message.material));
//		}
//		return true;
//	}
//
//	private boolean pipeDeconstructed(PipeConstructionMessage message) {
//		MapTile tile = gameContext.getAreaMap().getTile(message.tilePosition);
//		if (tile != null) {
//			UnderTile underTile = tile.getOrCreateUnderTile();
//			underTile.setPipeConstructionState(PipeConstructionState.NONE);
//			messageDispatcher.dispatchMessage(MessageType.REMOVE_PIPE, message.tilePosition);
//		}
//		return true;
//	}

}
