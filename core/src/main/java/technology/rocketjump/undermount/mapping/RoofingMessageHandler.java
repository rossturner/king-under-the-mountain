package technology.rocketjump.undermount.mapping;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.creature.DeathReason;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.jobs.model.JobTarget;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.roof.RoofConstructionState;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.*;
import technology.rocketjump.undermount.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;
import technology.rocketjump.undermount.settlement.notifications.Notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static technology.rocketjump.undermount.mapping.MapMessageHandler.propagateDarknessFromTile;
import static technology.rocketjump.undermount.misc.VectorUtils.toVector;
import static technology.rocketjump.undermount.settlement.notifications.NotificationType.ROOFING_COLLAPSE;

@Singleton
public class RoofingMessageHandler implements Telegraph, GameContextAware {

	private static final float CHANCE_OF_DEATH_FROM_ROOF_DEBRIS = 0.2f;
	private final MessageDispatcher messageDispatcher;
	private final RoofConstructionManager roofConstructionManager;
	private final OutdoorLightProcessor outdoorLightProcessor;
	private final ParticleEffectType wallRemovedParticleEffectType;
	private GameContext gameContext;

	@Inject
	public RoofingMessageHandler(MessageDispatcher messageDispatcher, RoofConstructionManager roofConstructionManager,
								 OutdoorLightProcessor outdoorLightProcessor, ParticleEffectTypeDictionary particleEffectTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.roofConstructionManager = roofConstructionManager;
		this.outdoorLightProcessor = outdoorLightProcessor;
		this.wallRemovedParticleEffectType = particleEffectTypeDictionary.getByName("Dust cloud"); // MODDING expose this

		messageDispatcher.addListener(this, MessageType.ROOF_CONSTRUCTION_QUEUE_CHANGE);
		messageDispatcher.addListener(this, MessageType.ROOF_DECONSTRUCTION_QUEUE_CHANGE);
		messageDispatcher.addListener(this, MessageType.ROOF_CONSTRUCTED);
		messageDispatcher.addListener(this, MessageType.ROOF_DECONSTRUCTED);
		messageDispatcher.addListener(this, MessageType.ROOF_SUPPORT_REMOVED);
		messageDispatcher.addListener(this, MessageType.WALL_REMOVED);
		messageDispatcher.addListener(this, MessageType.ROOF_COLLAPSE);
		messageDispatcher.addListener(this, MessageType.ROOF_TILE_COLLAPSE);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.ROOF_CONSTRUCTION_QUEUE_CHANGE: {
				return handle((TileConstructionQueueMessage) msg.extraInfo);
			}
			case MessageType.ROOF_DECONSTRUCTION_QUEUE_CHANGE: {
				return handle((TileDeconstructionQueueMessage) msg.extraInfo);
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
			case MessageType.ROOF_COLLAPSE: {
				return roofCollapse((RoofCollapseMessage) msg.extraInfo);
			}
			case MessageType.ROOF_TILE_COLLAPSE: {
				return roofTileCollapse((GridPoint2) msg.extraInfo);
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this + ", " + msg);
		}
	}

	private boolean handle(TileConstructionQueueMessage message) {
		if (message.parentTile.getRoof().getState().equals(TileRoofState.OPEN)) {
			if (message.constructionQueued) {
				roofConstructionManager.roofConstructionAdded(message.parentTile);
			} else {
				roofConstructionManager.roofConstructionRemoved(message.parentTile);
			}
		}
		return true;
	}

	private boolean handle(TileDeconstructionQueueMessage message) {
		if (message.parentTile.getRoof().getState().equals(TileRoofState.CONSTRUCTED)) {
			if (message.deconstructionQueued) {
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

			if (tile.hasRoom()) {
				tile.getRoomTile().getRoom().checkIfEnclosed(gameContext.getAreaMap());
			}
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

			if (tile.hasRoom()) {
				tile.getRoomTile().getRoom().checkIfEnclosed(gameContext.getAreaMap());
			}
		}
		return true;
	}

	private boolean roofCollapse(RoofCollapseMessage message) {
		messageDispatcher.dispatchMessage(MessageType.TRIGGER_SCREEN_SHAKE);
		Notification notification = new Notification(ROOFING_COLLAPSE, toVector(message.tilesToCollapseConstructedRoofing.iterator().next().getTilePosition()));
		messageDispatcher.dispatchMessage(MessageType.POST_NOTIFICATION, notification);

		List<MapTile> tiles = new ArrayList<>(message.tilesToCollapseConstructedRoofing);
		Collections.shuffle(tiles, gameContext.getRandom());
		float delaySecs = 0.05f;
		for (MapTile mapTile : tiles) {
			messageDispatcher.dispatchMessage(delaySecs, MessageType.ROOF_TILE_COLLAPSE, mapTile.getTilePosition());
			delaySecs += 0.05f;
		}
		return true;
	}

	private boolean roofTileCollapse(GridPoint2 location) {
		MapTile tile = gameContext.getAreaMap().getTile(location);
		if (tile != null && tile.getRoof().getState().equals(TileRoofState.CONSTRUCTED)) {

			messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(wallRemovedParticleEffectType,
					Optional.empty(), Optional.of(new JobTarget(tile, tile.getRoof())), (p) -> {}));

			tile.getRoof().setState(TileRoofState.OPEN);
			tile.getRoof().setConstructionState(RoofConstructionState.NONE);
			MapMessageHandler.markAsOutside(tile, gameContext, outdoorLightProcessor);

			for (Entity entity : tile.getEntities()) {
				if (entity.getType().equals(EntityType.CREATURE)) {
					if (gameContext.getRandom().nextFloat() < CHANCE_OF_DEATH_FROM_ROOF_DEBRIS) {
						messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH, new CreatureDeathMessage(entity, DeathReason.CRUSHED_BY_FALLING_DEBRIS));
					}
				}
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
