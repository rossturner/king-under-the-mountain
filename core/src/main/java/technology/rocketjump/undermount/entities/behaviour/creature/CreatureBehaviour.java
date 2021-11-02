package technology.rocketjump.undermount.entities.behaviour.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.undermount.entities.ai.goap.*;
import technology.rocketjump.undermount.entities.ai.memory.Memory;
import technology.rocketjump.undermount.entities.ai.memory.MemoryType;
import technology.rocketjump.undermount.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.components.humanoid.*;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.AggressionResponse;
import technology.rocketjump.undermount.entities.model.physical.creature.Consciousness;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static technology.rocketjump.undermount.entities.ai.goap.SpecialGoal.IDLE;
import static technology.rocketjump.undermount.entities.ai.goap.SpecialGoal.ROLL_ON_FLOOR;
import static technology.rocketjump.undermount.entities.model.physical.creature.Consciousness.AWAKE;
import static technology.rocketjump.undermount.entities.model.physical.creature.Consciousness.KNOCKED_UNCONSCIOUS;
import static technology.rocketjump.undermount.environment.model.WeatherType.HappinessInteraction.STANDING;

public abstract class CreatureBehaviour implements BehaviourComponent, Destructible, SelectableDescription {

	protected SteeringComponent steeringComponent = new SteeringComponent();
	protected Entity parentEntity;
	protected MessageDispatcher messageDispatcher;
	protected GameContext gameContext;

	protected CreatureGroup creatureGroup;
	protected AssignedGoal currentGoal;
	protected final GoalQueue goalQueue = new GoalQueue();
	protected transient double lastUpdateGameTime;

	protected GoalDictionary goalDictionary;
	private float stunTime;

	public void constructWith(GoalDictionary goalDictionary) {
		this.goalDictionary = goalDictionary;
	}

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;
		this.gameContext = gameContext;
		steeringComponent.init(parentEntity, gameContext.getAreaMap(), parentEntity.getLocationComponent(), messageDispatcher);

		if (currentGoal != null) {
			currentGoal.init(parentEntity, messageDispatcher);
		}
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (currentGoal != null) {
			currentGoal.destroy(parentEntity, messageDispatcher, gameContext);
			currentGoal = null;
		}
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (currentGoal == null || currentGoal.isComplete()) {
			currentGoal = selectNextGoal(gameContext);
		}

		// Not going to update steering when asleep so can't be pushed around
		Consciousness consciousness = ((CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes()).getConsciousness();
		if (AWAKE.equals(consciousness)) {
			steeringComponent.update(deltaTime);
		} else if (KNOCKED_UNCONSCIOUS.equals(consciousness)) {
			return;
		}

		if (stunTime > 0) {
			stunTime -= deltaTime;
			if (stunTime < 0) {
				stunTime = 0;
			}
			return;
		}

		try {
			currentGoal.update(deltaTime, gameContext);
		} catch (SwitchGoalException e) {
			AssignedGoal newGoal = new AssignedGoal(e.target, parentEntity, messageDispatcher);
			newGoal.setAssignedJob(currentGoal.getAssignedJob());
			newGoal.setAssignedHaulingAllocation(currentGoal.getAssignedHaulingAllocation());
			newGoal.setLiquidAllocation(currentGoal.getLiquidAllocation());
			if (newGoal.getAssignedHaulingAllocation() == null) {
				newGoal.setAssignedHaulingAllocation(currentGoal.getAssignedJob().getHaulingAllocation());
			}
			currentGoal = newGoal;
		}
	}

	public AssignedGoal getCurrentGoal() {
		return currentGoal;
	}

	private AssignedGoal selectNextGoal(GameContext gameContext) {
		if (parentEntity.isOnFire()) {
			return onFireGoal(gameContext);
		}

		Optional<Memory> attackedMemory = getMemoryOfAttackedByCreature(parentEntity, creatureGroup, gameContext);
		if (attackedMemory.isPresent()) {
			return attackedByCreatureResponse(attackedMemory.get(), gameContext);
		}

		Schedule schedule = ((CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes()).getRace().getBehaviour().getSchedule();
		List<ScheduleCategory> currentScheduleCategories = schedule == null ? List.of() : schedule.getCurrentApplicableCategories(gameContext.getGameClock());
		QueuedGoal nextGoal = goalQueue.popNextGoal(currentScheduleCategories);
		if (nextGoal == null) {
			return new AssignedGoal(IDLE.getInstance(), parentEntity, messageDispatcher);
		}
		return new AssignedGoal(nextGoal.getGoal(), parentEntity, messageDispatcher);
	}

	public static Optional<Memory> getMemoryOfAttackedByCreature(Entity entity, CreatureGroup creatureGroup, GameContext gameContext) {
		MemoryComponent memoryComponent = entity.getOrCreateComponent(MemoryComponent.class);
		Optional<Memory> attackedMemory = memoryComponent.getShortTermMemories(gameContext.getGameClock())
				.stream().filter(m -> m.getType().equals(MemoryType.ATTACKED_BY_CREATURE)).findAny();
		if (attackedMemory.isEmpty() && creatureGroup != null) {
			attackedMemory = creatureGroup.getSharedMemoryComponent().getShortTermMemories(gameContext.getGameClock())
					.stream().filter(m -> m.getType().equals(MemoryType.ATTACKED_BY_CREATURE)).findAny();
		}
		return attackedMemory;
	}

	private AssignedGoal onFireGoal(GameContext gameContext) {
		if (gameContext.getRandom().nextBoolean()) {
			return new AssignedGoal(ROLL_ON_FLOOR.getInstance(), parentEntity, messageDispatcher);
		}
		return new AssignedGoal(IDLE.getInstance(), parentEntity, messageDispatcher);
	}

	protected AssignedGoal attackedByCreatureResponse(Memory attackedByCreatureMemory, GameContext gameContext) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		AggressionResponse aggressionResponse = attributes.getRace().getBehaviour().getAggressionResponse();
		if (aggressionResponse == null || aggressionResponse.equals(AggressionResponse.MIXED)) {
			if (gameContext.getRandom().nextBoolean()) {
				aggressionResponse = AggressionResponse.ATTACK;
			} else {
				aggressionResponse = AggressionResponse.FLEE;
			}
		}

		Goal goal;
		switch (aggressionResponse) {
			case ATTACK:
				goal = SpecialGoal.ATTACK_AGGRESSOR.getInstance();
				break;
			case FLEE:
				goal = SpecialGoal.FLEE_FROM_AGGRESSOR.getInstance();
				break;
			default:
				throw new NotImplementedException("No goal speified for aggression response: " + aggressionResponse.name());
		}
		AssignedGoal assignedGoal = new AssignedGoal(goal, parentEntity, messageDispatcher);
		assignedGoal.setRelevantMemory(attackedByCreatureMemory);
		return assignedGoal;
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		double gameTime = gameContext.getGameClock().getCurrentGameTime();
		double elapsed = gameTime - lastUpdateGameTime;
		lastUpdateGameTime = gameTime;

		if (creatureGroup != null) {
			creatureGroup.infrequentUpdate(gameContext);
		}

		NeedsComponent needsComponent = parentEntity.getComponent(NeedsComponent.class);
		needsComponent.update(elapsed, parentEntity, messageDispatcher);

		parentEntity.getComponent(StatusComponent.class).infrequentUpdate(elapsed);

		HappinessComponent happinessComponent = parentEntity.getComponent(HappinessComponent.class);
		if (happinessComponent != null) {
			MapTile currentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
			if (currentTile != null && currentTile.getRoof().getState().equals(TileRoofState.OPEN) &&
					gameContext.getMapEnvironment().getCurrentWeather().getHappinessModifiers().containsKey(STANDING)) {
				happinessComponent.add(gameContext.getMapEnvironment().getCurrentWeather().getHappinessModifiers().get(STANDING));
			}
		}
		addGoalsToQueue(gameContext);

//		lookAtNearbyThings(gameContext);

//		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
//		if (attributes.getSanity().equals(Sanity.SANE) && attributes.getConsciousness().equals(AWAKE) &&
//				happinessComponent.getNetModifier() <= MIN_HAPPINESS_VALUE) {
//			messageDispatcher.dispatchMessage(MessageType.HUMANOID_INSANITY, parentEntity);
//		}
	}

	@Override
	public SteeringComponent getSteeringComponent() {
		return steeringComponent;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return true;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return true;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
	}

	public CreatureGroup getCreatureGroup() {
		return creatureGroup;
	}

	public void setCreatureGroup(CreatureGroup creatureGroup) {
		this.creatureGroup = creatureGroup;
	}

	protected void addGoalsToQueue(GameContext gameContext) {
		NeedsComponent needsComponent = parentEntity.getComponent(NeedsComponent.class);
		MemoryComponent memoryComponent = parentEntity.getComponent(MemoryComponent.class);
		goalQueue.removeExpiredGoals(gameContext.getGameClock());
		for (Goal potentialGoal : goalDictionary.getAllGoals()) {
			if (potentialGoal.getSelectors().isEmpty()) {
				continue; // Don't add goals with no selectors
			}
			if (currentGoal != null && potentialGoal.equals(currentGoal.goal)) {
				continue; // Don't queue up the current goal
			}
			for (GoalSelector selector : potentialGoal.getSelectors()) {
				boolean allConditionsApply = true;
				for (GoalSelectionCondition condition : selector.conditions) {
					if (!condition.apply(gameContext.getGameClock(), needsComponent, memoryComponent)) {
						allConditionsApply = false;
						break;
					}
				}
				if (allConditionsApply) {
					goalQueue.add(new QueuedGoal(potentialGoal, selector.scheduleCategory, selector.priority, gameContext.getGameClock()));
					break;
				}
			}
		}
	}

	public void applyStun(Random random) {
		this.stunTime = 1f + (random.nextFloat() * 3f);
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		if (attributes.getConsciousness().equals(KNOCKED_UNCONSCIOUS)) {
			return List.of(i18nTranslator.getTranslatedString("ACTION.KNOCKED_UNCONSCIOUS"));
		}

		List<I18nText> descriptionStrings = new ArrayList<>();
		descriptionStrings.add(i18nTranslator.getCurrentGoalDescription(parentEntity, currentGoal, gameContext));
		if (stunTime > 0) {
			descriptionStrings.add(i18nTranslator.getTranslatedString("ACTION.STUNNED"));
		}
		return descriptionStrings;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (creatureGroup != null) {
			creatureGroup.writeTo(savedGameStateHolder);
			asJson.put("creatureGroup", creatureGroup.getGroupId());
		}

		if (currentGoal != null) {
			JSONObject currentGoalJson = new JSONObject(true);
			currentGoal.writeTo(currentGoalJson, savedGameStateHolder);
			asJson.put("currentGoal", currentGoalJson);
		}

		if (!goalQueue.isEmpty()) {
			JSONObject goalQueueJson = new JSONObject(true);
			goalQueue.writeTo(goalQueueJson, savedGameStateHolder);
			asJson.put("goalQueue", goalQueueJson);
		}

		if (steeringComponent != null) {
			JSONObject steeringComponentJson = new JSONObject(true);
			steeringComponent.writeTo(steeringComponentJson, savedGameStateHolder);
			asJson.put("steeringComponent", steeringComponentJson);
		}

		if (stunTime > 0) {
			asJson.put("stunTime", stunTime);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.goalDictionary = relatedStores.goalDictionary;

		Long creatureGroupId = asJson.getLong("creatureGroup");
		if (creatureGroupId != null) {
			this.creatureGroup = savedGameStateHolder.creatureGroups.get(creatureGroupId);
			if (this.creatureGroup == null) {
				throw new InvalidSaveException("Could not find creature group with ID " + creatureGroupId);
			}
		}

		JSONObject currentGoalJson = asJson.getJSONObject("currentGoal");
		if (currentGoalJson != null) {
			currentGoal = new AssignedGoal();
			currentGoal.readFrom(currentGoalJson, savedGameStateHolder, relatedStores);
		}

		JSONObject goalQueueJson = asJson.getJSONObject("goalQueue");
		if (goalQueueJson != null) {
			goalQueue.readFrom(goalQueueJson, savedGameStateHolder, relatedStores);
		}

		JSONObject steeringComponentJson = asJson.getJSONObject("steeringComponent");
		if (steeringComponentJson != null) {
			this.steeringComponent.readFrom(steeringComponentJson, savedGameStateHolder, relatedStores);
		}

		this.stunTime = asJson.getFloatValue("stunTime");
	}
}
