package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.ai.goap.EntityNeed;
import technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent;
import technology.rocketjump.undermount.entities.components.humanoid.NeedsComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.Consciousness;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.DeathReason;
import technology.rocketjump.undermount.entities.model.physical.creature.body.BodyPart;
import technology.rocketjump.undermount.entities.model.physical.creature.body.BodyPartDamage;
import technology.rocketjump.undermount.entities.model.physical.creature.body.BodyPartDamageLevel;
import technology.rocketjump.undermount.entities.model.physical.creature.body.BodyPartOrgan;
import technology.rocketjump.undermount.entities.model.physical.creature.body.organs.OrganDamageLevel;
import technology.rocketjump.undermount.environment.model.WeatherType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.CreatureDeathMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import static technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation.*;
import static technology.rocketjump.undermount.entities.ai.goap.EntityNeed.DRINK;
import static technology.rocketjump.undermount.entities.ai.goap.EntityNeed.FOOD;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent.HappinessModifier.*;
import static technology.rocketjump.undermount.entities.components.humanoid.NeedsComponent.MAX_NEED_VALUE;
import static technology.rocketjump.undermount.mapping.tile.roof.TileRoofState.OPEN;

public class SleepOnFloorAction extends Action {
	private static final float CHANCE_TO_HEAL_ORGAN_DAMAGE = 0.1f;
	private static final float CHANCE_TO_HEAL_BODY_DAMAGE = 0.4f;

	public SleepOnFloorAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parent.parentEntity.getPhysicalEntityComponent().getAttributes();
		if (!Consciousness.SLEEPING.equals(attributes.getConsciousness())) {
			changeToSleeping(gameContext);
		}

		HappinessComponent happinessComponent = parent.parentEntity.getComponent(HappinessComponent.class);
		if (happinessComponent != null) {
			happinessComponent.add(SLEPT_ON_GROUND);

			MapTile currentTile = gameContext.getAreaMap().getTile(parent.parentEntity.getLocationComponent().getWorldPosition());
			if (currentTile != null && currentTile.hasRoom() && currentTile.getRoomTile().getRoom().isFullyEnclosed()) {
				happinessComponent.add(SLEPT_IN_ENCLOSED_BEDROOM);
			}
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
		HappinessComponent happinessComponent = parent.parentEntity.getComponent(HappinessComponent.class);
		if (OPEN.equals(currentTile.getRoof().getState()) && happinessComponent != null) {
			happinessComponent.add(SLEPT_OUTSIDE);

			if (gameContext.getMapEnvironment().getCurrentWeather().getHappinessModifiers().containsKey(WeatherType.HappinessInteraction.SLEEPING)) {
				happinessComponent.add(gameContext.getMapEnvironment().getCurrentWeather().getHappinessModifiers().get(WeatherType.HappinessInteraction.SLEEPING));
			}
		}

		NeedsComponent needsComponent = parent.parentEntity.getComponent(NeedsComponent.class);
		// All changes in need amounts are handled by SettlerBehaviour
		if (needsComponent.getValue(EntityNeed.SLEEP) >= 100.0) {
			if (happinessComponent != null) {
				// Only entities with tracked happiness can freeze outside

				if (OPEN.equals(currentTile.getRoof().getState()) && gameContext.getMapEnvironment().getCurrentWeather().getChanceToFreezeToDeathFromSleeping() != null) {
					float roll = gameContext.getRandom().nextFloat();
					if (roll < gameContext.getMapEnvironment().getCurrentWeather().getChanceToFreezeToDeathFromSleeping()) {
						parent.messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH, new CreatureDeathMessage(parent.parentEntity, DeathReason.FROZEN));
						completionType = FAILURE;
						return;
					}
				}
			}

			changeToAwake(gameContext);
			completionType = SUCCESS;
		} else if ((needsComponent.has(FOOD) && needsComponent.getValue(FOOD) <= 0.0)
				|| (needsComponent.has(DRINK) && needsComponent.getValue(DRINK) <= 0.0)) {
			// Currently starving or dehydrated so wake up after sleep at 30%
			if (needsComponent.getValue(EntityNeed.SLEEP) >= 30.0) {
				changeToAwake(gameContext);
				completionType = SUCCESS;
			}
		} else if (parent.parentEntity.isOnFire()) {
			changeToAwake(gameContext);
			completionType = SUCCESS;
		}
	}

	protected void changeToSleeping(GameContext gameContext) {
		changeToConsciousnessOnFloor(parent.parentEntity, Consciousness.SLEEPING, gameContext, parent.messageDispatcher);
	}

	public static void changeToConsciousnessOnFloor(Entity entity, Consciousness consciousness,
													GameContext gameContext, MessageDispatcher messageDispatcher) {
		entity.getLocationComponent().setLinearVelocity(Vector2.Zero);
		entity.getBehaviourComponent().getSteeringComponent().destinationReached();

		if (entity.isJobAssignable()) {
			// Only job assignable rotate and face certain direction

			// face in a direction and rotate as appropriate
			showAsRotatedOnSide(entity, gameContext);
		}
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		attributes.setConsciousness(consciousness);
		if (entity.isJobAssignable()) {
			messageDispatcher.dispatchMessage(MessageType.SETTLER_FELL_ASLEEP);
		}
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entity);
	}

	public static void showAsRotatedOnSide(Entity entity, GameContext gameContext) {
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
	}

	protected void changeToAwake(GameContext gameContext) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parent.parentEntity.getPhysicalEntityComponent().getAttributes();

		// Stop rotation
		parent.parentEntity.getLocationComponent().setRotation(0);

		attributes.setConsciousness(Consciousness.AWAKE);
		if (parent.parentEntity.isJobAssignable()) {
			parent.messageDispatcher.dispatchMessage(MessageType.SETTLER_WOKE_UP);
		}
		parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parent.parentEntity);

		// Heal damage if this was a restful sleep
		Double sleepNeed = parent.parentEntity.getComponent(NeedsComponent.class).getValue(EntityNeed.SLEEP);
		if (sleepNeed > MAX_NEED_VALUE / 2) {
			for (Map.Entry<BodyPart, BodyPartDamage> entry : new ArrayList<>(attributes.getBody().getAllDamage())) {
				if (entry.getValue().getDamageLevel().equals(BodyPartDamageLevel.Destroyed)) {
					continue;
				}

				Iterator<Map.Entry<BodyPartOrgan, OrganDamageLevel>> organDamageIterator = entry.getValue().getOrganDamage().entrySet().iterator();
				while (organDamageIterator.hasNext()) {
					Map.Entry<BodyPartOrgan, OrganDamageLevel> organDamageLevelEntry = organDamageIterator.next();
					if (!organDamageLevelEntry.getValue().equals(OrganDamageLevel.DESTROYED)) {
						if (gameContext.getRandom().nextFloat() < CHANCE_TO_HEAL_ORGAN_DAMAGE) {
							organDamageIterator.remove();
						}
					}
				}

				if (!entry.getValue().getDamageLevel().equals(BodyPartDamageLevel.None)) {
					if (gameContext.getRandom().nextFloat() < CHANCE_TO_HEAL_BODY_DAMAGE) {
						entry.getValue().healOneLevel();
					}
				}

				if (entry.getValue().getDamageLevel().equals(BodyPartDamageLevel.None) && entry.getValue().getOrganDamage().isEmpty()) {
					attributes.getBody().clearDamage(entry.getKey());
				}

			}
		}
	}
}
