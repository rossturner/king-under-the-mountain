package technology.rocketjump.undermount.entities;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.FloorTypeDictionary;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.entities.behaviour.effects.FireEffectBehaviour;
import technology.rocketjump.undermount.entities.components.AttachedEntitiesComponent;
import technology.rocketjump.undermount.entities.components.humanoid.StatusComponent;
import technology.rocketjump.undermount.entities.factories.OngoingEffectAttributesFactory;
import technology.rocketjump.undermount.entities.factories.OngoingEffectEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.undermount.entities.model.physical.humanoid.status.OnFireStatus;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.ChangeFloorMessage;

import java.util.Optional;

import static technology.rocketjump.undermount.messaging.MessageType.*;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;

@Singleton
public class FireMessageHandler implements GameContextAware, Telegraph {

	private static final int NUM_DIRECTIONS_TO_SPREAD_FIRE_IN = 3;
	private static final int MAX_DISTANCE_TO_SPREAD_FIRE_IN = 3;
	private final MessageDispatcher messageDispatcher;
	private final FloorType ashFloor;
	private final GameMaterial ashMaterial;
	private final OngoingEffectAttributesFactory ongoingEffectAttributesFactory;
	private final OngoingEffectEntityFactory ongoingEffectEntityFactory;

	private GameContext gameContext;

	@Inject
	public FireMessageHandler(MessageDispatcher messageDispatcher, FloorTypeDictionary floorTypeDictionary,
							  GameMaterialDictionary gameMaterialDictionary, OngoingEffectAttributesFactory ongoingEffectAttributesFactory, OngoingEffectEntityFactory ongoingEffectEntityFactory) {
		this.messageDispatcher = messageDispatcher;
		this.ashFloor = floorTypeDictionary.getByFloorTypeName("ash");
		this.ashMaterial = gameMaterialDictionary.getByName("Ash");
		this.ongoingEffectAttributesFactory = ongoingEffectAttributesFactory;
		this.ongoingEffectEntityFactory = ongoingEffectEntityFactory;

		messageDispatcher.addListener(this, MessageType.SPREAD_FIRE_FROM_LOCATION);
		messageDispatcher.addListener(this, CONSUME_TILE_BY_FIRE);
		messageDispatcher.addListener(this, MessageType.CONSUME_ENTITY_BY_FIRE);
		messageDispatcher.addListener(this, MessageType.ADD_FIRE_TO_ENTITY);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case SPREAD_FIRE_FROM_LOCATION:
				Vector2 location = (Vector2) msg.extraInfo;
				spreadFireFrom(location);
				return true;
			case CONSUME_TILE_BY_FIRE:
				MapTile tile = gameContext.getAreaMap().getTile((Vector2) msg.extraInfo);
				if (tile != null) {
					if (tile.hasWall()) {
						messageDispatcher.dispatchMessage(MessageType.REMOVE_WALL, tile.getTilePosition());
					}
					messageDispatcher.dispatchMessage(MessageType.CHANGE_FLOOR, new ChangeFloorMessage(tile.getTilePosition(), ashFloor, ashMaterial));
				}
				return true;
			case ADD_FIRE_TO_ENTITY:
				Entity targetEntity = (Entity) msg.extraInfo;
				OngoingEffectAttributes attributes = ongoingEffectAttributesFactory.createByTypeName("Fire");
				Entity fireEntity = ongoingEffectEntityFactory.create(attributes, null, gameContext);

				AttachedEntitiesComponent attachedEntitiesComponent = targetEntity.getOrCreateComponent(AttachedEntitiesComponent.class);
				attachedEntitiesComponent.init(targetEntity, messageDispatcher, gameContext);
				attachedEntitiesComponent.addAttachedEntity(fireEntity);

				return true;
			case CONSUME_ENTITY_BY_FIRE:
				Logger.warn("Not yet implemented: CONSUME_ENTITY_BY_FIRE");
				return true;
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void spreadFireFrom(Vector2 location) {
		GridPoint2 centre = toGridPoint(location);
		MapTile centreTile = gameContext.getAreaMap().getTile(centre);
		if (centreTile == null) {
			return;
		}
		for (CompassDirection direction : CompassDirection.DIAGONAL_DIRECTIONS) {
			MapTile nextTile = centreTile;
			for (int distance = 1; distance <= MAX_DISTANCE_TO_SPREAD_FIRE_IN; distance++) {
				nextTile = selectNextTile(nextTile, direction);
				if (nextTile == null) {
					break;
				}

				if (nextTile.hasWall()) {
					if (nextTile.getWall().getMaterial().isCombustible()) {
						createFireInTile(nextTile);
					}
					break;
				} else {

					Optional<Entity> combustibleEntity = nextTile.getEntities().stream()
							.filter(e -> e.getPhysicalEntityComponent().getAttributes()
									.getMaterials().values().stream().anyMatch(GameMaterial::isCombustible))
							.findFirst();

					if (combustibleEntity.isPresent()) {
						StatusComponent statusComponent = combustibleEntity.get().getOrCreateComponent(StatusComponent.class);
						statusComponent.init(combustibleEntity.get(), messageDispatcher, gameContext);
						statusComponent.apply(new OnFireStatus());
						break;
					} else if (nextTile.getFloor().getMaterial().isCombustible()) {
						createFireInTile(nextTile);
						break;
					}


				}

			}
		}
	}

	/**
	 * This method is used to pick either the diagonal direction or one of the 2 adjacent orthogonal directions
	 */
	private MapTile selectNextTile(MapTile tile, CompassDirection diagonalDirection) {
		int xOffset = diagonalDirection.getXOffset();
		int yOffset = diagonalDirection.getYOffset();
		float roll = gameContext.getRandom().nextFloat();
		if (roll < 0.33f) {
			xOffset = 0;
		} else if (roll < 0.66f) {
			yOffset = 0;
		}

		return gameContext.getAreaMap().getTile(
				tile.getTileX() + xOffset,
				tile.getTileY() + yOffset
		);
	}

	private void createFireInTile(MapTile targetTile) {
		if (!targetTile.getEntities().stream().anyMatch(e -> e.getBehaviourComponent() instanceof FireEffectBehaviour)) {
			OngoingEffectAttributes attributes = ongoingEffectAttributesFactory.createByTypeName("Fire");
			ongoingEffectEntityFactory.create(attributes, targetTile.getWorldPositionOfCenter(), gameContext);
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
