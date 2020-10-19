package technology.rocketjump.undermount.rendering.lighting;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.components.AttachedLightSourceComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.DoorwayPlacementMessage;

import java.util.HashSet;
import java.util.Set;

import static technology.rocketjump.undermount.rendering.lighting.PointLight.LIGHT_RADIUS;

/**
 * This class is to keep track of all changes which would change the visibility polygon of a light source
 * And set those lights to requiring an update
 *
 * Works by pushing affected tiles into tilesToCheck via messages notifying changes of scenery and
 * then iterating those tiles in the update step
 */
@Singleton
public class AttachedLightSourceProcessor implements Updatable, Telegraph {

	private GameContext gameContext;

	@Inject
	public AttachedLightSourceProcessor(MessageDispatcher messageDispatcher) {
		messageDispatcher.addListener(this, MessageType.DOOR_OPENED_OR_CLOSED);
		messageDispatcher.addListener(this, MessageType.WALL_CREATED);
		messageDispatcher.addListener(this, MessageType.WALL_REMOVED);
		messageDispatcher.addListener(this, MessageType.CREATE_DOORWAY);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.DOOR_OPENED_OR_CLOSED:
			case MessageType.WALL_CREATED:
			case MessageType.WALL_REMOVED:
				GridPoint2 doorPosition = (GridPoint2) msg.extraInfo;
				markTilesToCheckAround(doorPosition);
				return true;
			case MessageType.CREATE_DOORWAY:
				DoorwayPlacementMessage message = (DoorwayPlacementMessage) msg.extraInfo;
				markTilesToCheckAround(message.getTilePosition());
				return false; // This is not the primary consumer of CREATE_DOORWAY messages
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private Set<GridPoint2> tilesToCheck = new HashSet<>();

	private void markTilesToCheckAround(GridPoint2 gridPosition) {
		for (int y = gridPosition.y - (int)LIGHT_RADIUS; y <= gridPosition.y + (int)LIGHT_RADIUS; y++) {
			for (int x = gridPosition.x - (int)LIGHT_RADIUS; x <= gridPosition.x + (int)LIGHT_RADIUS; x++) {
				tilesToCheck.add(new GridPoint2(x, y));
			}
		}
	}

	private boolean isOnMap(GridPoint2 positionToCheck) {
		if (gameContext == null) {
			return false;
		} else {
			TiledMap areaMap = gameContext.getAreaMap();
			return positionToCheck.x >= 0 && positionToCheck.x < areaMap.getWidth() &&
					positionToCheck.y >= 0 && positionToCheck.y < areaMap.getHeight();
		}
	}

	@Override
	public void update(float deltaTime) {
		if (gameContext == null) {
			return;
		}
		TiledMap areaMap = gameContext.getAreaMap();
		for (GridPoint2 tileToCheckPosition : tilesToCheck) {
			if (isOnMap(tileToCheckPosition)) {
				MapTile tileToCheck = areaMap.getTile(tileToCheckPosition);
				for (Entity entity : tileToCheck.getEntities()) {
					AttachedLightSourceComponent component = entity.getComponent(AttachedLightSourceComponent.class);
					if (component != null) {
						component.setUpdateRequired();
					}
				}
			}
		}

		tilesToCheck.clear(); // Clear for next frame
	}

	@Override
	public boolean runWhilePaused() {
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
