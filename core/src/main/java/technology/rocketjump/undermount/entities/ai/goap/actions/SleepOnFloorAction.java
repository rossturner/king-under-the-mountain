package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.ai.goap.EntityNeed;
import technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent;
import technology.rocketjump.undermount.entities.components.humanoid.NeedsComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.humanoid.Consciousness;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidEntityAttributes;
import technology.rocketjump.undermount.environment.model.WeatherType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation.*;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent.HappinessModifier.*;
import static technology.rocketjump.undermount.mapping.tile.roof.TileRoofState.OPEN;

public class SleepOnFloorAction extends Action {
	public SleepOnFloorAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		HumanoidEntityAttributes attributes = (HumanoidEntityAttributes) parent.parentEntity.getPhysicalEntityComponent().getAttributes();
		if (!Consciousness.SLEEPING.equals(attributes.getConsciousness())) {
			changeToSleeping(gameContext);
		}

		parent.parentEntity.getComponent(HappinessComponent.class).add(SLEPT_ON_GROUND);

		MapTile currentTile = gameContext.getAreaMap().getTile(parent.parentEntity.getLocationComponent().getWorldPosition());
		if (currentTile != null && currentTile.hasRoom() && currentTile.getRoomTile().getRoom().isFullyEnclosed()) {
			parent.parentEntity.getComponent(HappinessComponent.class).add(SLEPT_IN_ENCLOSED_BEDROOM);
		}

		checkForWakingUp(gameContext);
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

	protected void checkForWakingUp(GameContext gameContext) {
		MapTile currentTile = gameContext.getAreaMap().getTile(parent.parentEntity.getLocationComponent().getWorldOrParentPosition());
		if (OPEN.equals(currentTile.getRoof().getState())) {
			parent.parentEntity.getComponent(HappinessComponent.class).add(SLEPT_OUTSIDE);

			if (gameContext.getMapEnvironment().getCurrentWeather().getHappinessModifiers().containsKey(WeatherType.HappinessInteraction.SLEEPING)) {
				parent.parentEntity.getComponent(HappinessComponent.class).add(gameContext.getMapEnvironment().getCurrentWeather().getHappinessModifiers().get(WeatherType.HappinessInteraction.SLEEPING));
			}
		}

		NeedsComponent needsComponent = parent.parentEntity.getComponent(NeedsComponent.class);
		// All changes in need amounts are handled by SettlerBehaviour
		if (needsComponent.getValue(EntityNeed.SLEEP) >= 100.0) {
			changeToAwake();
			completionType = SUCCESS;
		} else if (needsComponent.getValue(EntityNeed.FOOD) <= 0.0 || needsComponent.getValue(EntityNeed.DRINK) <= 0.0) {
			// Currently starving or dehydrated so wake up after sleep at 30%
			if (needsComponent.getValue(EntityNeed.SLEEP) >= 30.0) {
				changeToAwake();
				completionType = SUCCESS;
			}
		} else if (parent.parentEntity.isOnFire()) {
			changeToAwake();
			completionType = SUCCESS;
		}
	}

	protected void changeToSleeping(GameContext gameContext) {
		changeToConsciousnessOnFloor(parent.parentEntity, Consciousness.SLEEPING, gameContext, parent.messageDispatcher);
	}

	public static void changeToConsciousnessOnFloor(Entity entity, Consciousness consciousness,
													GameContext gameContext, MessageDispatcher messageDispatcher) {
		entity.getLocationComponent().setLinearVelocity(Vector2.Zero);
		entity.getLocationComponent().setAngularVelocity(0);
		entity.getBehaviourComponent().getSteeringComponent().destinationReached();

		// face in a direction and rotate as appropriate
		if (gameContext.getRandom().nextBoolean()) {
			entity.getLocationComponent().setRotation(80f + (gameContext.getRandom().nextFloat() * 20f));
			if (gameContext.getRandom().nextBoolean()) {
				entity.getLocationComponent().setFacing(DOWN_LEFT.toVector2());
			} else {
				entity.getLocationComponent().setFacing(DOWN.toVector2());
			}

		} else {
			entity.getLocationComponent().setRotation(260f + (gameContext.getRandom().nextFloat() * 20f));
			if (gameContext.getRandom().nextBoolean()) {
				entity.getLocationComponent().setFacing(DOWN_RIGHT.toVector2());
			} else {
				entity.getLocationComponent().setFacing(DOWN.toVector2());
			}

		}
		HumanoidEntityAttributes attributes = (HumanoidEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		attributes.setConsciousness(consciousness);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_FELL_ASLEEP);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entity);
	}

	protected void changeToAwake() {
		HumanoidEntityAttributes attributes = (HumanoidEntityAttributes) parent.parentEntity.getPhysicalEntityComponent().getAttributes();

		// Stop rotation
		parent.parentEntity.getLocationComponent().setRotation(0);

		attributes.setConsciousness(Consciousness.AWAKE);
		parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_WOKE_UP);
		parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parent.parentEntity);
	}
}
