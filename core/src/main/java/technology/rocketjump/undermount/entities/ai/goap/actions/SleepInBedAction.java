package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.assets.entities.tags.BedSleepingPositionTag;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.furniture.SleepingPositionComponent;
import technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.undermount.entities.model.physical.humanoid.Consciousness;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.rooms.RoomTile;

import static technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation.DOWN;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.undermount.entities.behaviour.furniture.CraftingStationBehaviour.getAnyNavigableWorkspace;
import static technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent.HappinessModifier.*;
import static technology.rocketjump.undermount.misc.VectorUtils.toVector;

/**
 * This extends SleepOnFloorAction purely to share some code regarding sleeping
 */
public class SleepInBedAction extends SleepOnFloorAction {

	private static final int MIN_AVERAGE_BEDROOM_SIZE = 6;

	public SleepInBedAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (!isAsleep()) {
			getIntoAssignedBedAndSleep(gameContext);
		} else {

			Entity bedEntity = gameContext.getEntities().get(parent.getAssignedFurnitureId());
			if (bedEntity == null) {
				completionType = FAILURE;
				return;
			}
			MapTile bedTile = gameContext.getAreaMap().getTile(bedEntity.getLocationComponent().getWorldPosition());
			Room bedroom = null;
			if (bedTile != null && bedTile.getRoomTile() != null) {
				bedroom = bedTile.getRoomTile().getRoom();
			}

			Entity assignedFurniture = gameContext.getEntities().get(parent.getAssignedFurnitureId());
			SleepingPositionComponent sleepingPositionComponent = assignedFurniture.getComponent(SleepingPositionComponent.class);
			if (sleepingPositionComponent.isOnFloor()) {
				parent.parentEntity.getComponent(HappinessComponent.class).add(SLEPT_ON_GROUND);
			}

			if (bedroom == null || bedroomIsShared(bedroom, gameContext)) {
				parent.parentEntity.getComponent(HappinessComponent.class).add(SLEPT_IN_SHARED_BEDROOM);
				if (bedroom != null && bedroomIsSmall(bedroom)) {
					parent.parentEntity.getComponent(HappinessComponent.class).add(SLEPT_IN_SMALL_BEDROOM);
				}
			} else {
				if (bedroom.isFullyEnclosed()) {
					parent.parentEntity.getComponent(HappinessComponent.class).add(SLEPT_IN_ENCLOSED_BEDROOM);
				}
				if (bedroomIsSmall(bedroom)) {
					parent.parentEntity.getComponent(HappinessComponent.class).add(SLEPT_IN_SMALL_BEDROOM);
				}
			}


			checkForWakingUp(gameContext);

			if (completionType == SUCCESS) {
				getOutOfBed(gameContext);
			}
		}
	}

	private boolean bedroomIsSmall(Room bedroom) {
		return bedroom.getRoomTiles().size() < MIN_AVERAGE_BEDROOM_SIZE;
	}

	private boolean bedroomIsShared(Room room, GameContext gameContext) {
		Entity bedEntity = gameContext.getEntities().get(parent.getAssignedFurnitureId());
		for (RoomTile roomTile : room.getRoomTiles().values()) {
			MapTile tile = roomTile.getTile();
			for (Entity entity : tile.getEntities()) {
				if (entity.getId() != bedEntity.getId() && entity.getType().equals(EntityType.FURNITURE)) {
					if (entity.getTag(BedSleepingPositionTag.class) != null) {
						// This is another bed
						FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
						Long assignedToEntityId = attributes.getAssignedToEntityId();
						if (assignedToEntityId != null && assignedToEntityId != parent.parentEntity.getId()) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	public void actionInterrupted(GameContext gameContext) {
		if (isAsleep()) {
			changeToAwake();
			getOutOfBed(gameContext);
		}
		completionType = SUCCESS;
	}

	@Override
	public String getDescriptionOverrideI18nKey() {
		return "ACTION.SLEEP_ON_FLOOR.DESCRIPTION";
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state to read
	}

	private void getIntoAssignedBedAndSleep(GameContext gameContext) {
		Entity assignedFurniture = gameContext.getEntities().get(parent.getAssignedFurnitureId());
		SleepingPositionComponent sleepingPositionComponent = assignedFurniture.getComponent(SleepingPositionComponent.class);
		if (sleepingPositionComponent.isOnFloor()) {
			changeToSleeping(gameContext);
		} else if (assignedFurniture == null || !locatedInFurnitureWorkspace(assignedFurniture)) {
			completionType = FAILURE;
		} else {
			InventoryComponent inventoryComponent = assignedFurniture.getOrCreateComponent(InventoryComponent.class);
			inventoryComponent.add(parent.parentEntity, assignedFurniture, parent.messageDispatcher, gameContext.getGameClock());
			changeToSleeping(gameContext);
			updateForSleepingOrientation(gameContext, assignedFurniture);
		}
	}

	private void getOutOfBed(GameContext gameContext) {
		Entity assignedFurniture = gameContext.getEntities().get(parent.getAssignedFurnitureId());
		if (assignedFurniture == null) {
			Logger.error("Can't get out of bed! Can't find assigned furniture with ID " + parent.getAssignedFurnitureId());
			completionType = FAILURE;
			return;
		}
		SleepingPositionComponent sleepingPositionComponent = assignedFurniture.getComponent(SleepingPositionComponent.class);
		if (!sleepingPositionComponent.isOnFloor()) {
			InventoryComponent inventoryComponent = assignedFurniture.getOrCreateComponent(InventoryComponent.class);
			inventoryComponent.remove(parent.parentEntity.getId());

			Vector2 furnitureLocation = assignedFurniture.getLocationComponent().getWorldPosition().cpy();
			parent.parentEntity.getLocationComponent().setWorldPosition(furnitureLocation, false);

			FurnitureLayout.Workspace navigableWorkspace = getAnyNavigableWorkspace(assignedFurniture, gameContext.getAreaMap());
			if (navigableWorkspace != null) {
				Vector2 workspaceLocation = toVector(navigableWorkspace.getAccessedFrom());
				parent.parentEntity.getLocationComponent().setWorldPosition(workspaceLocation, true);
			} //  Else we can no longer get out of bed into a workspace, place onto bed tile instead
			parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parent.parentEntity);
		}
	}

	private void updateForSleepingOrientation(GameContext gameContext, Entity assignedFurniture) {
		SleepingPositionComponent sleepingPositionComponent = assignedFurniture.getComponent(SleepingPositionComponent.class);
		EntityAssetOrientation sleepingOrientation = sleepingPositionComponent.getSleepingOrientation();

		parent.parentEntity.getLocationComponent().setFacing(sleepingOrientation.toVector2());
		if (sleepingOrientation.toVector2().x > 0) {
			parent.parentEntity.getLocationComponent().setRotation(260f + (gameContext.getRandom().nextFloat() * 20f));
		} else if (sleepingOrientation.toVector2().x < 0) {
			parent.parentEntity.getLocationComponent().setRotation(80f + (gameContext.getRandom().nextFloat() * 20f));
		} else if (sleepingOrientation.equals(EntityAssetOrientation.UP)) {
			parent.parentEntity.getLocationComponent().setRotation(180);
			parent.parentEntity.getLocationComponent().setFacing(DOWN.toVector2());
		} else {
			parent.parentEntity.getLocationComponent().setRotation(0);
		}
		parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parent.parentEntity);
	}

	private boolean locatedInFurnitureWorkspace(Entity assignedFurniture) {
		Vector2 parentEntityPosition = parent.parentEntity.getLocationComponent().getWorldPosition();
		Vector2 furniturePosition = assignedFurniture.getLocationComponent().getWorldPosition();
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) assignedFurniture.getPhysicalEntityComponent().getAttributes();
		for (FurnitureLayout.Workspace workspace : attributes.getCurrentLayout().getWorkspaces()) {
			Vector2 workspacePosition = furniturePosition.cpy().add(workspace.getAccessedFrom().x, workspace.getAccessedFrom().y);
			if (workspacePosition.dst2(parentEntityPosition) < 1) {
				return true;
			}
		}
		return false;
	}

	private boolean isAsleep() {
		HumanoidEntityAttributes attributes = (HumanoidEntityAttributes) parent.parentEntity.getPhysicalEntityComponent().getAttributes();
		return attributes.getConsciousness().equals(Consciousness.SLEEPING);
	}

}
